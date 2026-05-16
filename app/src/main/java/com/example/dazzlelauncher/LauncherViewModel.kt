package com.example.dazzlelauncher

import android.app.AppOpsManager
import android.app.Application
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.TimeZone

enum class LauncherMode {
    HOME_ONLY, HOME_AND_DRAWER
}

data class AppUsageInfo(
    val appInfo: AppInfo?,
    val packageName: String,
    val usageTime: Long
)

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

    private val _isDefault = MutableStateFlow(true)
    val isDefault: StateFlow<Boolean> = _isDefault.asStateFlow()

    private val _shouldUseDarkText = MutableStateFlow(false)
    val shouldUseDarkText: StateFlow<Boolean> = _shouldUseDarkText.asStateFlow()

    private val _usageStats = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val usageStats: StateFlow<List<AppUsageInfo>> = _usageStats.asStateFlow()

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

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
    
    fun checkDefaultLauncher(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        _isDefault.value = resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun fetchUsageStats() {
        val context = getApplication<Application>()
        if (!hasUsageAccess(context)) return

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        val calendar = _selectedDate.value.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        val endTime = if (isToday(_selectedDate.value)) {
            System.currentTimeMillis()
        } else {
            val endCal = calendar.clone() as Calendar
            endCal.add(Calendar.DAY_OF_YEAR, 1)
            endCal.timeInMillis
        }

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val stats = mutableMapOf<String, Long>()
        val startTimes = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)
            
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    startTimes[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val startTimeForPkg = startTimes[event.packageName]
                    if (startTimeForPkg != null) {
                        val duration = event.timeStamp - startTimeForPkg
                        if (duration > 0) {
                            stats[event.packageName] = (stats[event.packageName] ?: 0L) + duration
                        }
                        startTimes.remove(event.packageName)
                    }
                }
            }
        }

        // Handle apps still in foreground
        if (isToday(_selectedDate.value)) {
            val now = System.currentTimeMillis()
            for ((pkg, time) in startTimes) {
                val duration = now - time
                if (duration > 0) {
                    stats[pkg] = (stats[pkg] ?: 0L) + duration
                }
            }
        }

        val appUsageList = stats.map { (pkg, time) ->
            AppUsageInfo(
                appInfo = _allApps.value.find { it.packageName == pkg },
                packageName = pkg,
                usageTime = time
            )
        }.filter { it.usageTime > 0 && it.packageName != context.packageName }
        .sortedByDescending { it.usageTime }
        
        _usageStats.value = appUsageList
    }

    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
               calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    fun changeDate(days: Int) {
        val newDate = _selectedDate.value.clone() as Calendar
        newDate.add(Calendar.DAY_OF_YEAR, days)
        // Don't allow future dates
        if (newDate.timeInMillis <= System.currentTimeMillis() || 
            newDate.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
            _selectedDate.value = newDate
            fetchUsageStats()
        }
    }

    fun launchApp(context: Context, packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
        }
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

