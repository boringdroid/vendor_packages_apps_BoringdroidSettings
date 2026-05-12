@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.boringdroid.settings

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId

private const val ID_NAMESPACE = "com.boringdroid.settings:id/"

/**
 * Compose's [testTagsAsResourceId] writes the literal testTag into the AccessibilityNodeInfo's
 * `viewIdResourceName`, so UiAutomator's `By.res(packageName, resourceId)` selector (which expects
 * the fully-qualified `pkg:id/...` form) only resolves when the tag itself carries the prefix.
 * Use this extension everywhere instead of hand-rolled `semantics { … }` blocks so the prefix
 * stays consistent and the tests stay green.
 */
fun Modifier.bdTag(name: String): Modifier =
    this.semantics {
        testTagsAsResourceId = true
        testTag = "$ID_NAMESPACE$name"
    }
