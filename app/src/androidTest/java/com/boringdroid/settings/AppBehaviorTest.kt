package com.boringdroid.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.boringdroid.settings.SettingsTestBase.FIND_TIMEOUT_MS
import com.boringdroid.settings.SettingsTestBase.PKG
import com.boringdroid.settings.SettingsTestBase.byTag
import com.google.common.truth.Truth.assertThat
import java.util.regex.Pattern
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppBehaviorTest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val root =
            SettingsTestBase.launchActivity(
                activityClass = "com.boringdroid.settings.AppBehaviorActivity",
                readyTag = "app_behavior_root",
            )
        assertThat(root).isNotNull()
    }

    @After
    fun tearDown() {
        device.pressHome()
        device.waitForIdle()
    }

    @Test
    fun filterChips_renderAllFive() {
        for (tag in FILTER_TAGS) {
            val chip = device.wait(Until.findObject(byTag(tag)), FIND_TIMEOUT_MS)
            assertThat(chip).isNotNull()
        }
    }

    @Test
    fun selectAllLink_isVisibleWhenSelectionEmpty() {
        val link = device.wait(Until.findObject(byTag("app_behavior_select_all")), FIND_TIMEOUT_MS)
        assertThat(link).isNotNull()
    }

    @Test
    fun selectAll_revealsBulkActionBar() {
        // Bulk bar absent at start.
        assertThat(device.hasObject(byTag("app_behavior_bulk_bar"))).isFalse()

        val selectAll =
            device.wait(Until.findObject(byTag("app_behavior_select_all")), FIND_TIMEOUT_MS)
        assertThat(selectAll).isNotNull()
        selectAll.click()

        val bar = device.wait(Until.findObject(byTag("app_behavior_bulk_bar")), FIND_TIMEOUT_MS)
        assertThat(bar).isNotNull()

        // Reset using the "Clear" toggle (replaces "Select all" while selection > 0).
        val clear =
            device.wait(Until.findObject(byTag("app_behavior_clear_selection")), FIND_TIMEOUT_MS)
        assertThat(clear).isNotNull()
        clear.click()

        val gone = device.wait(Until.gone(byTag("app_behavior_bulk_bar")), FIND_TIMEOUT_MS)
        assertThat(gone).isTrue()
    }

    @Test
    fun bulkBar_exposesFreeformFullscreenReset() {
        val selectAll =
            device.wait(Until.findObject(byTag("app_behavior_select_all")), FIND_TIMEOUT_MS)
        selectAll.click()
        device.wait(Until.findObject(byTag("app_behavior_bulk_bar")), FIND_TIMEOUT_MS)

        for (tag in BULK_BUTTON_TAGS) {
            val btn = device.wait(Until.findObject(byTag(tag)), FIND_TIMEOUT_MS)
            assertThat(btn).isNotNull()
        }
    }

    @Test
    fun bulkApplyFreeform_dismissesSelection() {
        val selectAll =
            device.wait(Until.findObject(byTag("app_behavior_select_all")), FIND_TIMEOUT_MS)
        selectAll.click()
        device.wait(Until.findObject(byTag("app_behavior_bulk_bar")), FIND_TIMEOUT_MS)

        val applyFreeform =
            device.wait(
                Until.findObject(byTag("app_behavior_bulk_freeform")),
                FIND_TIMEOUT_MS,
            )
        assertThat(applyFreeform).isNotNull()
        applyFreeform.click()

        val gone = device.wait(Until.gone(byTag("app_behavior_bulk_bar")), FIND_TIMEOUT_MS)
        assertThat(gone).isTrue()
    }

    @Test
    fun singleAppRow_opensModeSheet() {
        // Find any app row by id pattern. UiAutomator's pattern-form `By.res(Pattern)` matches
        // the fully-qualified resource name, so include the package prefix.
        val rowSelector = By.res(Pattern.compile("$PKG:id/app_behavior_row_.+"))
        val row = device.wait(Until.findObject(rowSelector), FIND_TIMEOUT_MS)
        assertThat(row).isNotNull()
        row.click()
        // Mode sheet exposes the three radio options' text labels.
        val freeformLabel =
            device.wait(Until.findObject(By.textContains("Force free-form")), FIND_TIMEOUT_MS)
        assertThat(freeformLabel).isNotNull()
        device.pressBack()
        device.waitForIdle()
    }

    companion object {
        private val FILTER_TAGS =
            listOf(
                "app_behavior_filter_all",
                "app_behavior_filter_freeform",
                "app_behavior_filter_fullscreen",
                "app_behavior_filter_default",
                "app_behavior_filter_user",
            )
        private val BULK_BUTTON_TAGS =
            listOf(
                "app_behavior_bulk_freeform",
                "app_behavior_bulk_fullscreen",
                "app_behavior_bulk_reset",
            )
    }
}
