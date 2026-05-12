@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.boringdroid.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boringdroid.settings.theme.BdExpressiveMaterialTheme
import com.boringdroid.settings.theme.BdShape
import java.text.DateFormat
import java.util.Date

class AboutBoringdroidActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BdExpressiveMaterialTheme { AboutScreen(onBack = { finish() }) } }
    }
}

private data class AboutFacts(
    val versionName: String,
    val packageName: String,
    val releasedLabel: String,
    val androidVersion: String,
    val sdkLevel: Int,
    val hostDevice: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val facts = remember { readFacts(context) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.hint_back),
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .bdTag("about_root"),
        ) {
            HeroCard(
                version = facts.versionName,
                onWebsite = { openUrl(context, "https://boringdroid.github.io") },
                onGithub = { openUrl(context, "https://github.com/boringdroid") },
                onReportIssue = {
                    openUrl(context, "https://github.com/boringdroid/platform_manifest/issues")
                },
            )

            GroupTitle(stringResource(R.string.about_group_project))
            GroupCard {
                IconRow(
                    icon = Icons.Outlined.Commit,
                    title = stringResource(R.string.about_project_version),
                    subtitle = stringResource(R.string.about_project_version_sub),
                    trailingText = facts.versionName,
                    testTag = "about_project_version",
                )
                HorizontalDivider()
                IconRow(
                    icon = Icons.Outlined.Inventory2,
                    title = stringResource(R.string.about_project_package),
                    subtitle = facts.packageName,
                    trailingIcon = Icons.Outlined.OpenInNew,
                    testTag = "about_project_package",
                    onClick = { openUrl(context, "https://github.com/boringdroid") },
                )
                HorizontalDivider()
                IconRow(
                    icon = Icons.Outlined.CalendarToday,
                    title = stringResource(R.string.about_project_released),
                    subtitle = facts.releasedLabel,
                    testTag = "about_project_released",
                )
                HorizontalDivider()
                IconRow(
                    icon = Icons.Outlined.Balance,
                    title = stringResource(R.string.about_project_license),
                    subtitle = stringResource(R.string.about_project_license_sub),
                    trailingIcon = Icons.Outlined.OpenInNew,
                    testTag = "about_project_license",
                    onClick = { openUrl(context, "https://www.apache.org/licenses/LICENSE-2.0") },
                )
                HorizontalDivider()
                IconRow(
                    icon = Icons.Outlined.Hub,
                    title = stringResource(R.string.about_project_source),
                    subtitle = stringResource(R.string.about_project_source_sub),
                    trailingIcon = Icons.Outlined.OpenInNew,
                    testTag = "about_project_source",
                    onClick = { openUrl(context, "https://github.com/boringdroid") },
                )
            }

            GroupTitle(stringResource(R.string.about_group_authors))
            GroupCard {
                IconRow(
                    icon = Icons.Outlined.Person,
                    iconAccent = true,
                    title = stringResource(R.string.about_author_maintainer_name),
                    subtitle = stringResource(R.string.about_author_maintainer_role),
                    trailingIcon = Icons.Outlined.OpenInNew,
                    testTag = "about_author_maintainer",
                    onClick = { openUrl(context, "https://github.com/utzcoz") },
                )
                HorizontalDivider()
                IconRow(
                    icon = Icons.Outlined.Group,
                    title = stringResource(R.string.about_authors_contributors),
                    subtitle = stringResource(R.string.about_authors_contributors_sub),
                    trailingIcon = Icons.Outlined.OpenInNew,
                    testTag = "about_authors_contributors",
                    onClick = {
                        openUrl(context, "https://github.com/orgs/boringdroid/people")
                    },
                )
            }

            GroupTitle(stringResource(R.string.about_group_system))
            GroupCard {
                IconRow(
                    icon = Icons.Outlined.Memory,
                    title = stringResource(R.string.about_system_android),
                    subtitle =
                        stringResource(
                            R.string.about_system_android_sub,
                            facts.androidVersion,
                            facts.sdkLevel,
                        ),
                    trailingText = facts.androidVersion,
                    testTag = "about_system_android",
                )
                HorizontalDivider()
                IconRow(
                    icon = Icons.Outlined.DeveloperBoard,
                    title = stringResource(R.string.about_system_host),
                    subtitle = facts.hostDevice,
                    testTag = "about_system_host",
                )
                HorizontalDivider()
                IconRow(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.about_system_licenses),
                    subtitle = stringResource(R.string.about_system_licenses_sub),
                    trailingIcon = Icons.Outlined.OpenInNew,
                    testTag = "about_system_licenses",
                    onClick = { openLicenses(context) },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HeroCard(
    version: String,
    onWebsite: () -> Unit,
    onGithub: () -> Unit,
    onReportIssue: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiaryContainer
    Surface(
        modifier =
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp).bdTag("about_hero"),
        shape = BdShape.extra2Large,
        color = Color.Transparent,
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(colors = listOf(primary, tertiary)),
                        shape = BdShape.extra2Large,
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = BdShape.extra2Large,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(88.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.about_hero_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.about_hero_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.about_hero_version, version),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.about_hero_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(16.dp))
                // FlowRow wraps onto a new line when the hero card is narrow (e.g. when this
                // activity is opened in a small freeform window), matching the design's
                // `flex-wrap: wrap` behaviour.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HeroAction(
                        label = stringResource(R.string.about_hero_action_website),
                        icon = Icons.Outlined.Language,
                        filled = true,
                        testTag = "about_hero_website",
                        onClick = onWebsite,
                    )
                    HeroAction(
                        label = stringResource(R.string.about_hero_action_github),
                        icon = Icons.Outlined.Code,
                        filled = false,
                        testTag = "about_hero_github",
                        onClick = onGithub,
                    )
                    HeroAction(
                        label = stringResource(R.string.about_hero_action_report),
                        icon = Icons.Outlined.BugReport,
                        filled = false,
                        testTag = "about_hero_report",
                        onClick = onReportIssue,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HeroAction(
    label: String,
    icon: ImageVector,
    filled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val modifier = Modifier.bdTag(testTag)
    if (filled) {
        Button(
            onClick = onClick,
            shape = BdShape.pill,
            modifier = modifier,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = BdShape.pill,
            modifier = modifier,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun GroupTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W600),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun GroupCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = BdShape.extra2Large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun IconRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingIcon: ImageVector? = null,
    trailingText: String? = null,
    iconAccent: Boolean = false,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier =
        Modifier.fillMaxWidth()
            .let { base -> if (testTag != null) base.bdTag(testTag) else base }
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        val (iconBg, iconFg) =
            if (iconAccent) {
                MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh to
                    MaterialTheme.colorScheme.onSurfaceVariant
            }
        Box(
            modifier = Modifier.size(40.dp).background(color = iconBg, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconFg,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trailingIcon != null) {
            if (trailingText != null) Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openLicenses(context: Context) {
    // Stock Settings' legal-licenses activity: com.android.settings.Settings$LicenseActivity.
    // The android.settings.LICENSE intent is the public alias.
    val intent = Intent("android.settings.LICENSE")
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        openUrl(context, "https://www.apache.org/licenses/LICENSE-2.0")
    }
}

private fun readFacts(context: Context): AboutFacts {
    val versionName =
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "14.x"
        } catch (_: PackageManager.NameNotFoundException) {
            "14.x"
        }
    val firstInstall =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (_: PackageManager.NameNotFoundException) {
            Build.TIME
        }
    val released =
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(firstInstall))
    val hostDevice =
        listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { !it.isNullOrBlank() }
            .joinToString(" · ")
            .ifBlank { Build.PRODUCT }
    return AboutFacts(
        versionName = versionName,
        packageName = context.packageName,
        releasedLabel = released,
        androidVersion = Build.VERSION.RELEASE,
        sdkLevel = Build.VERSION.SDK_INT,
        hostDevice = hostDevice,
    )
}
