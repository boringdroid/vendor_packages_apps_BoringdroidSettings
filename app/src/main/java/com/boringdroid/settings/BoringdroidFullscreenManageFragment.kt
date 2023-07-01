package com.boringdroid.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.os.UserManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import java.lang.reflect.InvocationTargetException

class BoringdroidFullscreenManageFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Do nothing
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val result = super.onCreateView(inflater, container, savedInstanceState)
        val adapter = AppListAdapter(requireContext())
        if (listView != null) {
            listView.adapter = adapter
        }
        val userManager = requireContext().getSystemService(Context.USER_SERVICE) as UserManager
        val userHandles = userManager.userProfiles
        val activityInfoList: MutableList<LauncherActivityInfo> = java.util.ArrayList()
        val launcherApps =
            requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        for (userHandle in userHandles) {
            activityInfoList.addAll(launcherApps.getActivityList(null, userHandle))
        }
        val allApps = ArrayList<AppInfo>()
        val fullscreenWindowingMode = getFullscreenWindowingMode()
        for (info in activityInfoList) {
            val appInfo = AppInfo()
            appInfo.label = info.label as String
            appInfo.packageName = info.applicationInfo.packageName
            appInfo.icon = info.getIcon(0)
            val windowingMode = getPackageWindowingMode(appInfo.packageName)
            appInfo.isFullscreen = windowingMode == fullscreenWindowingMode
            allApps.add(appInfo)
            Log.d(
                TAG,
                "package ${appInfo.packageName} " +
                    "windowing mode $windowingMode " +
                    "fullscreenWindowingMode $fullscreenWindowingMode " +
                    "forceFullscreen ${appInfo.isFullscreen}",
            )
        }
        adapter.setData(allApps)
        return result
    }

    class AppListAdapter(private var context: Context) :
        RecyclerView.Adapter<AppListAdapter.ViewHolder>() {
        private var apps = ArrayList<AppInfo>()

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var ivIcon: ImageView = itemView.findViewById(R.id.iv_app_icon)
            var tvName: TextView = itemView.findViewById(R.id.tv_app_name)
            var switchChangeToFullscreen: SwitchCompat =
                itemView.findViewById(R.id.switch_change_to_fullscreen)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val appInfoLayout = LayoutInflater.from(context)
                .inflate(R.layout.layout_app_info, parent, false)
            return ViewHolder(appInfoLayout)
        }

        override fun getItemCount(): Int = apps.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val appInfo = apps[position]
            holder.ivIcon.setImageDrawable(appInfo.icon)
            holder.tvName.text = appInfo.label
            holder.switchChangeToFullscreen.isEnabled = true
            holder.switchChangeToFullscreen.isChecked = appInfo.isFullscreen

            holder.switchChangeToFullscreen.setOnCheckedChangeListener { _, isChecked ->
                appInfo.isFullscreen = isChecked
                val windowingMode =
                    if (appInfo.isFullscreen) {
                        WINDOWING_MODE_FULLSCREEN
                    } else {
                        WINDOWING_MODE_UNDEFINED
                    }
                Log.d(
                    TAG,
                    "checked changed ${appInfo.packageName} " +
                        "value ${appInfo.isFullscreen} windowingMode $windowingMode",
                )
                savePackageOverlayWindowingMode(appInfo.packageName, windowingMode)
            }
        }

        fun setData(apps: List<AppInfo>) {
            this.apps.clear()
            this.apps.addAll(apps)
            notifyDataSetChanged()
        }
    }

    class AppInfo {
        var label: String = ""
        var packageName: String = ""
        var icon: Drawable? = null
        var isFullscreen: Boolean = false
    }

    companion object {
        var TAG = "FullscreenManage"
        val WINDOWING_MODE_FULLSCREEN = getFullscreenWindowingMode()
        val WINDOWING_MODE_UNDEFINED = getUndefinedWindowingMode()

        @SuppressLint("PrivateApi")
        private fun getFullscreenWindowingMode(): Int {
            val defaultFullscreenWindowingMode = 1
            try {
                val clazz = Class.forName("android.app.WindowConfiguration")
                return clazz.getDeclaredField("WINDOWING_MODE_FULLSCREEN")
                    .getInt(defaultFullscreenWindowingMode)
            } catch (e: ClassNotFoundException) {
                Log.d(TAG, "Failed to get fullscreen value", e)
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "Failed to get fullscreen value", e)
            } catch (e: IllegalAccessException) {
                Log.d(TAG, "Failed to get fullscreen value", e)
            } catch (e: InvocationTargetException) {
                Log.d(TAG, "Failed to get fullscreen value", e)
            }
            return defaultFullscreenWindowingMode
        }

        @SuppressLint("PrivateApi")
        private fun getUndefinedWindowingMode(): Int {
            val defaultUndefinedWindowingMode = 0
            try {
                val clazz = Class.forName("android.app.WindowConfiguration")
                return clazz.getDeclaredField("WINDOWING_MODE_UNDEFINED")
                    .getInt(defaultUndefinedWindowingMode)
            } catch (e: ClassNotFoundException) {
                Log.d(TAG, "Failed to get undefined value", e)
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "Failed to get undefined value", e)
            } catch (e: IllegalAccessException) {
                Log.d(TAG, "Failed to get undefined value", e)
            } catch (e: InvocationTargetException) {
                Log.d(TAG, "Failed to get undefined value", e)
            }
            return defaultUndefinedWindowingMode
        }

        @SuppressLint("PrivateApi")
        private fun getPackageWindowingMode(packageName: String): Int {
            try {
                val clazz = Class.forName("com.android.internal.BoringdroidManager")
                val setMethod = clazz.getMethod(
                    "getPackageOverlayWindowingMode",
                    String::class.java,
                )
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
            Environment.getDataDirectory()
            try {
                val clazz = Class.forName("com.android.internal.BoringdroidManager")
                val setMethod = clazz.getMethod(
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
