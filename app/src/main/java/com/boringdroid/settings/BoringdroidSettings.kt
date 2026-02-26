package com.boringdroid.settings

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import java.lang.reflect.InvocationTargetException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BoringdroidSettings : AppCompatActivity() {
    private enum class Screen {
        SETTINGS,
        FULLSCREEN_APPS,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BoringdroidSettingsApp() }
    }

    @Composable
    private fun BoringdroidSettingsApp() {
        var screen by rememberSaveable { mutableStateOf(Screen.SETTINGS) }

        MaterialTheme {
            when (screen) {
                Screen.SETTINGS ->
                    MainSettingsScreen(onOpenFullscreenApps = { screen = Screen.FULLSCREEN_APPS })
                Screen.FULLSCREEN_APPS ->
                    FullscreenAppsScreen(onBack = { screen = Screen.SETTINGS })
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun MainSettingsScreen(onOpenFullscreenApps: () -> Unit) {
        val context = LocalContext.current
        var pcModeEnabled by remember {
            mutableStateOf(getBooleanSystemProperties(PROPERTY_PC_MODE_KEY))
        }
        var systemUiEnabled by remember {
            mutableStateOf(getBooleanSystemProperties(PROPERTY_BD_SYSTEMUI_KEY))
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = { Text(text = stringResource(R.string.ic_name)) })
            }
        ) { innerPadding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                item { CategoryTitle(title = stringResource(R.string.title_category_pc_mode)) }
                item {
                    SwitchSettingRow(
                        title = stringResource(R.string.title_switch_enable_pc_mode),
                        checked = pcModeEnabled,
                    ) { enabled ->
                        pcModeEnabled = enabled
                        setBooleanSystemProperties(PROPERTY_PC_MODE_KEY, enabled)
                    }
                }
                item {
                    ActionSettingRow(
                        title = stringResource(R.string.title_set_all_fullscreen_apps)
                    ) {
                        onOpenFullscreenApps()
                    }
                }

                item { CategoryTitle(title = stringResource(R.string.title_category_bd_systemui)) }
                item {
                    SwitchSettingRow(
                        title = stringResource(R.string.title_switch_enable_bd_nav_bar),
                        checked = systemUiEnabled,
                    ) { enabled ->
                        systemUiEnabled = enabled
                        enableBoringdroidSystemUI(context, enabled)
                    }
                }

                item { CategoryTitle(title = stringResource(R.string.title_category_bd_about)) }
                item {
                    ActionSettingRow(
                        title = stringResource(R.string.title_bd_developer),
                        summary = stringResource(R.string.summary_bd_developer),
                    ) {
                        openURL(context, "https://github.com/utzcoz")
                    }
                }
                item {
                    ActionSettingRow(
                        title = stringResource(R.string.title_bd_github),
                        summary = stringResource(R.string.summary_bd_github),
                    ) {
                        openURL(context, "https://github.com/boringdroid")
                    }
                }
                item {
                    ActionSettingRow(
                        title = stringResource(R.string.title_bd_group),
                        summary = stringResource(R.string.summary_bd_group),
                    ) {
                        openURL(context, "http://blissos.org/")
                    }
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun FullscreenAppsScreen(onBack: () -> Unit) {
        BackHandler(onBack = onBack)

        val context = LocalContext.current
        val apps = remember { mutableStateListOf<AppInfo>() }
        var loading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            val loadedApps = withContext(Dispatchers.IO) { loadInstalledApps(context) }
            apps.clear()
            apps.addAll(loadedApps)
            loading = false
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = stringResource(R.string.title_set_all_fullscreen_apps)) },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text(text = stringResource(R.string.action_back))
                        }
                    },
                )
            }
        ) { innerPadding ->
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    items(items = apps, key = { it.packageName }) { appInfo ->
                        FullscreenAppRow(appInfo = appInfo)
                    }
                }
            }
        }
    }

    @Composable
    private fun FullscreenAppRow(appInfo: AppInfo) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable {
                        val updatedValue = !appInfo.isFullscreen
                        appInfo.isFullscreen = updatedValue
                        savePackageOverlayWindowingMode(
                            appInfo.packageName,
                            if (updatedValue) WINDOWING_MODE_FULLSCREEN
                            else WINDOWING_MODE_UNDEFINED,
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = appInfo.icon
            if (icon != null) {
                Image(
                    bitmap = remember(icon) { icon.toBitmap().asImageBitmap() },
                    contentDescription = stringResource(R.string.hint_app_icon),
                    modifier = Modifier.size(40.dp),
                )
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = appInfo.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.size(12.dp))
            Switch(
                checked = appInfo.isFullscreen,
                onCheckedChange = { checked ->
                    appInfo.isFullscreen = checked
                    savePackageOverlayWindowingMode(
                        appInfo.packageName,
                        if (checked) WINDOWING_MODE_FULLSCREEN else WINDOWING_MODE_UNDEFINED,
                    )
                },
            )
        }
        Divider()
    }

    @Composable
    private fun CategoryTitle(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }

    @Composable
    private fun SwitchSettingRow(
        title: String,
        checked: Boolean,
        onCheckedChanged: (Boolean) -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = checked, onCheckedChange = onCheckedChanged)
        }
        Divider()
    }

    @Composable
    private fun ActionSettingRow(title: String, summary: String? = null, onClick: () -> Unit) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (summary != null) {
                    Text(text = summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Divider()
    }

    private fun openURL(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    private fun enableBoringdroidSystemUI(context: Context, enable: Boolean) {
        Log.d(TAG, "enable boringdroid SystemUI $enable")
        setBooleanSystemProperties(PROPERTY_BD_SYSTEMUI_KEY, enable)
        val packageName = context.packageName
        val intent =
            Intent("com.android.systemui.action.RESTART")
                .setData(Uri.parse("package://$packageName"))
        val cn = ComponentName("com.android.systemui", "com.android.systemui.SysuiRestartReceiver")
        intent.component = cn
        context.sendBroadcast(intent)
    }

    private fun loadInstalledApps(context: Context): List<AppInfo> {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityInfoList = mutableListOf<LauncherActivityInfo>()
        for (userHandle in userManager.userProfiles) {
            activityInfoList.addAll(launcherApps.getActivityList(null, userHandle))
        }
        val fullscreenWindowingMode = getFullscreenWindowingMode()
        return activityInfoList
            .map { info ->
                val packageName = info.applicationInfo.packageName
                val windowingMode = getPackageWindowingMode(packageName)
                AppInfo(
                    label = info.label.toString(),
                    packageName = packageName,
                    icon = info.getIcon(0),
                    isFullscreen = windowingMode == fullscreenWindowingMode,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private class AppInfo(
        val label: String,
        val packageName: String,
        val icon: Drawable?,
        isFullscreen: Boolean,
    ) {
        var isFullscreen by mutableStateOf(isFullscreen)
    }

    companion object {
        private const val TAG = "BDSettings"
        private const val SYSTEM_PROPERTIES_CLASS_NAME = "android.os.SystemProperties"
        private const val PROPERTY_PC_MODE_KEY = "persist.sys.pcmode.enabled"
        private const val PROPERTY_BD_SYSTEMUI_KEY = "persist.sys.systemuiplugin.enabled"
        private val WINDOWING_MODE_FULLSCREEN = getFullscreenWindowingMode()
        private val WINDOWING_MODE_UNDEFINED = getUndefinedWindowingMode()

        private fun setBooleanSystemProperties(key: String, value: Boolean) {
            try {
                @SuppressLint("PrivateApi") val clazz = Class.forName(SYSTEM_PROPERTIES_CLASS_NAME)
                val setMethod = clazz.getMethod("set", String::class.java, String::class.java)
                setMethod.invoke(null, key, if (value) "true" else "false")
            } catch (e: ClassNotFoundException) {
                Log.d(TAG, "Failed to set value $value for $key")
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "Failed to set value $value for $key")
            } catch (e: IllegalAccessException) {
                Log.d(TAG, "Failed to set value $value for $key")
            } catch (e: InvocationTargetException) {
                Log.d(TAG, "Failed to set value $value for $key")
            }
        }

        private fun getBooleanSystemProperties(key: String): Boolean {
            try {
                @SuppressLint("PrivateApi") val clazz = Class.forName(SYSTEM_PROPERTIES_CLASS_NAME)
                val setMethod =
                    clazz.getMethod(
                        "getBoolean",
                        String::class.java,
                        Boolean::class.javaPrimitiveType,
                    )
                return setMethod.invoke(null, key, true) as Boolean
            } catch (e: ClassNotFoundException) {
                Log.d(TAG, "Failed to get value for $key")
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "Failed to get value for $key")
            } catch (e: IllegalAccessException) {
                Log.d(TAG, "Failed to get value for $key")
            } catch (e: InvocationTargetException) {
                Log.d(TAG, "Failed to get value for $key")
            }
            return true
        }

        private fun getFullscreenWindowingMode(): Int {
            return 1
        }

        private fun getUndefinedWindowingMode(): Int {
            return 0
        }

        @SuppressLint("PrivateApi")
        private fun getPackageWindowingMode(packageName: String): Int {
            try {
                val clazz = Class.forName("com.android.internal.BoringdroidManager")
                val setMethod =
                    clazz.getMethod("getPackageOverlayWindowingMode", String::class.java)
                return setMethod.invoke(null, packageName) as Int
            } catch (e: ClassNotFoundException) {
                Log.d(TAG, "Failed to get package windowing mode", e)
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "Failed to get package windowing mode", e)
            } catch (e: IllegalAccessException) {
                Log.d(TAG, "Failed to get package windowing mode", e)
            } catch (e: InvocationTargetException) {
                Log.d(TAG, "Failed to get package windowing mode", e)
            }
            return getUndefinedWindowingMode()
        }

        @SuppressLint("PrivateApi")
        private fun savePackageOverlayWindowingMode(packageName: String, windowingMode: Int) {
            try {
                val clazz = Class.forName("com.android.internal.BoringdroidManager")
                val setMethod =
                    clazz.getMethod(
                        "savePackageOverlayWindowingMode",
                        String::class.java,
                        Int::class.java,
                    )
                setMethod.invoke(null, packageName, windowingMode)
            } catch (e: ClassNotFoundException) {
                Log.d(TAG, "Failed to save package windowing mode", e)
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "Failed to save package windowing mode", e)
            } catch (e: IllegalAccessException) {
                Log.d(TAG, "Failed to save package windowing mode", e)
            } catch (e: InvocationTargetException) {
                Log.d(TAG, "Failed to save package windowing mode", e)
            }
        }
    }
}
