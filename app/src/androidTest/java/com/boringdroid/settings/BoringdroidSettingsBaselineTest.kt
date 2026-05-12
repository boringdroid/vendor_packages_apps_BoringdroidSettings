package com.boringdroid.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoringdroidSettingsBaselineTest {

    @Test
    fun targetPackageName_matchesPlatformApp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertThat(appContext.packageName).isEqualTo(SettingsTestBase.PKG)
    }

    @Test
    fun appBehaviorActivity_launchesAndRendersRoot() {
        val root =
            SettingsTestBase.launchActivity(
                activityClass = "com.boringdroid.settings.AppBehaviorActivity",
                readyTag = "app_behavior_root",
            )
        assertThat(root).isNotNull()
    }

    @Test
    fun aboutActivity_launchesAndRendersRoot() {
        val root =
            SettingsTestBase.launchActivity(
                activityClass = "com.boringdroid.settings.AboutBoringdroidActivity",
                readyTag = "about_root",
            )
        assertThat(root).isNotNull()
    }
}
