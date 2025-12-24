package com.abhishek.adblocker.data.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.abhishek.adblocker.domain.model.AppInfo
import com.abhishek.adblocker.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun getAllInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            Logger.d("Loading installed apps...")
            val rawApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            Logger.d("Found ${rawApps.size} raw apps from PackageManager")

            val apps = rawApps
                .filterNot { it.packageName == context.packageName }
                .map { appInfo ->
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = appInfo.loadLabel(packageManager).toString(),
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        isSelected = false
                    )
                }
                .sortedWith(AppInfo.comparator())

            Logger.d("Returning ${apps.size} apps after filtering")
            apps
        } catch (e: Exception) {
            Logger.e("Error loading installed apps", e)
            emptyList()
        }
    }

    suspend fun getInstalledApps(): List<AppInfo> = getAllInstalledApps()

    suspend fun filterValidPackages(packageNames: Set<String>): Set<String> =
        withContext(Dispatchers.IO) {
            packageNames.filter { packageName ->
                try {
                    packageManager.getApplicationInfo(packageName, 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    Logger.w("App package not found: $packageName - removing from selection")
                    false
                }
            }.toSet()
        }
}
