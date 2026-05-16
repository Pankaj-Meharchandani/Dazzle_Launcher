package com.example.dazzlelauncher

import android.app.Application
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LauncherMode {
    HOME_ONLY, HOME_AND_DRAWER
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val packageManager = application.packageManager
    private val prefs = application.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    private val wallpaperManager = WallpaperManager.getInstance(application)

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _homeApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val homeApps: StateFlow<List<AppInfo>> = _homeApps.asStateFlow()

    private val _dockApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val dockApps: StateFlow<List<AppInfo>> = _dockApps.asStateFlow()

    private val _mode = MutableStateFlow(
        LauncherMode.valueOf(prefs.getString("mode", LauncherMode.HOME_AND_DRAWER.name)!!)
    )
    val mode: StateFlow<LauncherMode> = _mode.asStateFlow()

    private val _useWallpaper = MutableStateFlow(prefs.getBoolean("use_wallpaper", true))
    val useWallpaper: StateFlow<Boolean> = _useWallpaper.asStateFlow()

    private val _shouldUseDarkText = MutableStateFlow(false)
    val shouldUseDarkText: StateFlow<Boolean> = _shouldUseDarkText.asStateFlow()

    private val wallpaperColorsListener = WallpaperManager.OnColorsChangedListener { colors, _ ->
        updateWallpaperHints(colors)
    }

    init {
        loadApps()
        initWallpaperColors()
    }

    private fun initWallpaperColors() {
        val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        updateWallpaperHints(colors)
        wallpaperManager.addOnColorsChangedListener(wallpaperColorsListener, Handler(Looper.getMainLooper()))
    }

    private fun updateWallpaperHints(colors: WallpaperColors?) {
        if (colors == null) {
            _shouldUseDarkText.value = false
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            _shouldUseDarkText.value = (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
        } else {
            // Fallback for API 30: check luminance of primary color
            val primary = colors.primaryColor.toArgb()
            _shouldUseDarkText.value = ColorUtils.calculateLuminance(primary) > 0.5
        }
    }

    override fun onCleared() {
        super.onCleared()
        wallpaperManager.removeOnColorsChangedListener(wallpaperColorsListener)
    }

    fun loadApps() {
        val launcherApps = getApplication<Application>().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val user = Process.myUserHandle()
        val apps = launcherApps.getActivityList(null, user).map { info ->
            val iconDrawable = info.getIcon(0)
            // Limit icon size for better performance and memory usage
            val bitmap = iconDrawable?.toBitmap(width = 200, height = 200)
            AppInfo(
                label = info.label.toString(),
                packageName = info.applicationInfo.packageName,
                className = info.name,
                icon = bitmap?.asImageBitmap()
            )
        }.sortedBy { it.label }
        
        _allApps.value = apps
        
        updateDockApps(apps)
        updateHomeApps(apps)
    }

    private fun updateDockApps(apps: List<AppInfo>) {
        val savedDock = prefs.getStringSet("dock_apps", null)
        if (savedDock != null) {
            _dockApps.value = apps.filter { it.packageName in savedDock }
        } else {
            // Default: Phone and Calculator
            _dockApps.value = apps.filter { 
                it.label.equals("Phone", ignoreCase = true) || 
                it.label.equals("Calculator", ignoreCase = true) 
            }.take(5)
        }
    }

    private fun updateHomeApps(apps: List<AppInfo>) {
        val dockPackageNames = _dockApps.value.map { it.packageName }
        
        if (_mode.value == LauncherMode.HOME_ONLY) {
            // In HOME_ONLY, show all apps not in dock, shuffled
            _homeApps.value = apps.filter { it.packageName !in dockPackageNames }.shuffled()
        } else {
            val savedHomeApps = prefs.getStringSet("home_apps", null)
            if (savedHomeApps != null) {
                // Show saved apps that are NOT in the dock
                _homeApps.value = apps.filter { 
                    it.packageName in savedHomeApps && it.packageName !in dockPackageNames 
                }.shuffled()
            } else {
                // Default home apps: take first 12 that are NOT in the dock
                _homeApps.value = apps.filter { it.packageName !in dockPackageNames }.take(12).shuffled()
            }
        }
    }

    fun shuffleHomeApps() {
        _homeApps.value = _homeApps.value.shuffled()
    }

    fun setMode(mode: LauncherMode) {
        _mode.value = mode
        prefs.edit().putString("mode", mode.name).apply()
        updateHomeApps(_allApps.value)
    }

    fun toggleDockApp(app: AppInfo) {
        val currentDock = _dockApps.value.toMutableList()
        val isAlreadyInDock = currentDock.any { it.packageName == app.packageName }
        
        if (isAlreadyInDock) {
            currentDock.removeAll { it.packageName == app.packageName }
            // Add back to home if removed from dock
            val currentHome = _homeApps.value.toMutableList()
            if (!currentHome.any { it.packageName == app.packageName }) {
                currentHome.add(app)
            }
            _homeApps.value = currentHome
        } else if (currentDock.size < 5) {
            // Remove from home if adding to dock to avoid duplicates
            val currentHome = _homeApps.value.toMutableList()
            currentHome.removeAll { it.packageName == app.packageName }
            _homeApps.value = currentHome
            
            currentDock.add(app)
        }
        
        _dockApps.value = currentDock
        prefs.edit().putStringSet("dock_apps", currentDock.map { it.packageName }.toSet()).apply()
        
        // Save home apps state too
        prefs.edit().putStringSet("home_apps", _homeApps.value.map { it.packageName }.toSet()).apply()
    }

    fun setUseWallpaper(use: Boolean) {
        _useWallpaper.value = use
        prefs.edit().putBoolean("use_wallpaper", use).apply()
    }


    fun toggleHomeApp(app: AppInfo) {
        val currentHome = _homeApps.value.toMutableList()
        val dockPackageNames = _dockApps.value.map { it.packageName }
        
        if (currentHome.any { it.packageName == app.packageName }) {
            currentHome.removeAll { it.packageName == app.packageName }
        } else if (app.packageName !in dockPackageNames) {
            currentHome.add(app)
        }

        _homeApps.value = currentHome
        prefs.edit().putStringSet("home_apps", currentHome.map { it.packageName }.toSet()).apply()
    }
    
    fun launchApp(context: Context, packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
        }
    }

    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun openNotifications(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

