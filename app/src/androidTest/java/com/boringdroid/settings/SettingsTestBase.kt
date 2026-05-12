package com.boringdroid.settings

import android.content.ComponentName
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/**
 * Common UiAutomator helpers shared by the BoringdroidSettings instrumentation tests.
 *
 * The activities ship in their own process (`com.boringdroid.settings`) so testTags on Compose
 * elements end up under that package's resource namespace — `By.res(PKG, "foo")` resolves to the
 * fully-qualified `com.boringdroid.settings:id/foo` and matches a tag of just `"foo"`. No
 * cross-package prefix is needed (unlike the BoringdroidSystemUI plugin which runs inside the
 * SystemUI process and has to ship fully-qualified tags).
 */
object SettingsTestBase {
    const val PKG = "com.boringdroid.settings"
    const val FIND_TIMEOUT_MS = 8000L

    /** Resets to home, then launches the named activity via Intent and waits for [readyTag]. */
    fun launchActivity(activityClass: String, readyTag: String): UiObject2? {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressHome()
        device.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().context
        val intent =
            Intent().apply {
                component = ComponentName(PKG, activityClass)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        context.startActivity(intent)
        return device.wait(Until.findObject(byTag(readyTag)), FIND_TIMEOUT_MS)
    }

    fun byTag(tag: String): BySelector = By.res(PKG, tag)

    /**
     * Swipe-scrolls the screen until [tag] resolves or [maxSwipes] are exhausted. Returns the
     * found object, or null if it never appeared. Use this in tests that need to reach a row that
     * sits below the fold of a `verticalScroll`-driven page (e.g. Project / Authors / System cards
     * on About) — Compose's `verticalScroll` does not promote off-screen children into UiAutomator's
     * a11y window dump, so `Until.findObject` alone won't find them.
     */
    fun scrollToTag(tag: String, maxSwipes: Int = 6): UiObject2? {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        repeat(maxSwipes) {
            val found = device.wait(Until.findObject(byTag(tag)), 1500L)
            if (found != null) return found
            val w = device.displayWidth
            val h = device.displayHeight
            device.swipe(w / 2, (h * 0.78).toInt(), w / 2, (h * 0.22).toInt(), 20)
            device.waitForIdle()
        }
        return device.wait(Until.findObject(byTag(tag)), 1500L)
    }
}

