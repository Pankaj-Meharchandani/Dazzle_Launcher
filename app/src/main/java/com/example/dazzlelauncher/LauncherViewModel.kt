package com.example.dazzlelauncher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import androidx.compose.ui.graphics.asImageBitmap
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

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _homeApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val homeApps: StateFlow<List<AppInfo>> = _homeApps.asStateFlow()

    private val _mode = MutableStateFlow(
        LauncherMode.valueOf(prefs.getString("mode", LauncherMode.HOME_AND_DRAWER.name)!!)
    )
    val mode: StateFlow<LauncherMode> = _mode.asStateFlow()

    init {
        loadApps()
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
                icon = bitmap?.asImageBitmap()
            )
        }.sortedBy { it.label }
        
        _allApps.value = apps
        
        updateHomeApps(apps)
    }

    private fun updateHomeApps(apps: List<AppInfo>) {
        if (_mode.value == LauncherMode.HOME_ONLY) {
            _homeApps.value = apps
        } else {
            val savedHomeApps = prefs.getStringSet("home_apps", null)
            if (savedHomeApps != null) {
                _homeApps.value = apps.filter { it.packageName in savedHomeApps }
            } else {
                _homeApps.value = apps.take(12)
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


    fun toggleHomeApp(app: AppInfo) {
        val currentHome = _homeApps.value.toMutableList()
        if (currentHome.any { it.packageName == app.packageName }) {
            currentHome.removeAll { it.packageName == app.packageName }
        } else {
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

