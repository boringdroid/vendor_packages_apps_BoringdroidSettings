package com.boringdroid.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.boringdroid.settings.SettingsTestBase.FIND_TIMEOUT_MS
import com.boringdroid.settings.SettingsTestBase.byTag
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutBoringdroidTest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val root =
            SettingsTestBase.launchActivity(
                activityClass = "com.boringdroid.settings.AboutBoringdroidActivity",
                readyTag = "about_root",
            )
        assertThat(root).isNotNull()
    }

    @After
    fun tearDown() {
        device.pressHome()
        device.waitForIdle()
    }

    @Test
    fun heroCard_rendersWithActionButtons() {
        assertThat(device.hasObject(byTag("about_hero"))).isTrue()
        for (tag in HERO_ACTIONS) {
            val obj = device.wait(Until.findObject(byTag(tag)), FIND_TIMEOUT_MS)
            assertThat(obj).isNotNull()
        }
    }

    @Test
    fun projectCard_listsExpectedRows() {
        // The Project card sits below the hero. Compose's verticalScroll doesn't surface off-screen
        // children to UiAutomator's window dump, so each row needs to be scrolled into view first.
        for (tag in PROJECT_ROWS) {
            val obj = SettingsTestBase.scrollToTag(tag)
            assertThat(obj).isNotNull()
        }
    }

    @Test
    fun authorsCard_listsMaintainerAndContributors() {
        for (tag in AUTHOR_ROWS) {
            val obj = SettingsTestBase.scrollToTag(tag)
            assertThat(obj).isNotNull()
        }
    }

    @Test
    fun systemCard_listsAndroidHostLicenses() {
        for (tag in SYSTEM_ROWS) {
            val obj = SettingsTestBase.scrollToTag(tag)
            assertThat(obj).isNotNull()
        }
    }

    companion object {
        private val HERO_ACTIONS =
            listOf("about_hero_website", "about_hero_github", "about_hero_report")
        private val PROJECT_ROWS =
            listOf(
                "about_project_version",
                "about_project_package",
                "about_project_released",
                "about_project_license",
                "about_project_source",
            )
        private val AUTHOR_ROWS =
            listOf("about_author_maintainer", "about_authors_contributors")
        private val SYSTEM_ROWS =
            listOf("about_system_android", "about_system_host", "about_system_licenses")
    }
}
