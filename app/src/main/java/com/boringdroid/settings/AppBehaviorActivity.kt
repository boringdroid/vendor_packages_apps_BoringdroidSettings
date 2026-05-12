@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.boringdroid.settings

import android.content.ContentResolver
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.boringdroid.settings.AppWindowingModeStore.Mode
import com.boringdroid.settings.theme.BdExpressiveMaterialTheme
import com.boringdroid.settings.theme.BdShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppBehaviorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BdExpressiveMaterialTheme { AppBehaviorScreen(onBack = { finish() }) } }
    }
}

private data class AppEntry(
    val label: String,
    val packageName: String,
    val icon: Drawable?,
    val isSystem: Boolean,
)

private enum class AppFilter(val labelId: Int, val icon: ImageVector, val tag: String) {
    ALL(R.string.app_behavior_filter_all, Icons.Outlined.Apps, "app_behavior_filter_all"),
    FREEFORM(R.string.app_behavior_filter_freeform, Icons.Outlined.PictureInPicture, "app_behavior_filter_freeform"),
    FULLSCREEN(R.string.app_behavior_filter_fullscreen, Icons.Outlined.FitScreen, "app_behavior_filter_fullscreen"),
    DEFAULT_MODE(R.string.app_behavior_filter_default, Icons.Outlined.SettingsSuggest, "app_behavior_filter_default"),
    USER(R.string.app_behavior_filter_user, Icons.Outlined.Person, "app_behavior_filter_user"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBehaviorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val modes: SnapshotStateMap<String, Mode> = remember { mutableStateMapOf() }
    val selected: SnapshotStateMap<String, Boolean> = remember { mutableStateMapOf() }
    var filter by rememberSaveable { mutableStateOf(AppFilter.ALL) }
    var sheetTarget by remember { mutableStateOf<AppEntry?>(null) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { loadInstalledApps(context) }
        val stored = withContext(Dispatchers.IO) { AppWindowingModeStore.load(resolver) }
        apps = loaded
        stored.forEach { (pkg, mode) -> modes[pkg] = mode }
        loading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_behavior_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.hint_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.app_behavior_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            AppBehaviorBody(
                innerPadding = innerPadding,
                apps = apps,
                modes = modes,
                selected = selected,
                filter = filter,
                onFilterChanged = { filter = it },
                onCheckboxToggle = { pkg ->
                    selected[pkg] = !(selected[pkg] ?: false)
                },
                onSelectAll = {
                    val anyUnselected = apps.any { selected[it.packageName] != true }
                    apps.forEach { selected[it.packageName] = anyUnselected }
                },
                onClearSelection = { selected.clear() },
                onRowClick = { sheetTarget = it },
                onBulkApply = { mode ->
                    val picks = apps.filter { selected[it.packageName] == true }
                    if (picks.isEmpty()) return@AppBehaviorBody
                    val previous = picks.associate { it.packageName to (modes[it.packageName] ?: Mode.DEFAULT) }
                    picks.forEach { applyMode(resolver, modes, it.packageName, mode) }
                    selected.clear()
                    val label = chipLabel(context, mode)
                    val message = context.getString(R.string.app_behavior_bulk_applied, picks.size, label)
                    scope.launch {
                        val result =
                            snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = context.getString(R.string.app_behavior_snackbar_undo),
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            previous.forEach { (pkg, prev) -> applyMode(resolver, modes, pkg, prev) }
                        }
                    }
                },
            )
        }
    }

    sheetTarget?.let { target ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { sheetTarget = null },
            sheetState = sheetState,
        ) {
            ModeSheetContent(
                target = target,
                current = modes[target.packageName] ?: Mode.DEFAULT,
                onPick = { picked ->
                    val previous = modes[target.packageName] ?: Mode.DEFAULT
                    applyMode(resolver, modes, target.packageName, picked)
                    sheetTarget = null
                    val undoMsg =
                        context.getString(
                            R.string.app_behavior_snackbar_updated,
                            target.label,
                            modeFullLabel(context, picked),
                        )
                    scope.launch {
                        val result =
                            snackbarHostState.showSnackbar(
                                message = undoMsg,
                                actionLabel = context.getString(R.string.app_behavior_snackbar_undo),
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            applyMode(resolver, modes, target.packageName, previous)
                        }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBehaviorBody(
    innerPadding: PaddingValues,
    apps: List<AppEntry>,
    modes: SnapshotStateMap<String, Mode>,
    selected: SnapshotStateMap<String, Boolean>,
    filter: AppFilter,
    onFilterChanged: (AppFilter) -> Unit,
    onCheckboxToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRowClick: (AppEntry) -> Unit,
    onBulkApply: (Mode) -> Unit,
) {
    val visible =
        apps.filter { app ->
            val mode = modes[app.packageName] ?: Mode.DEFAULT
            when (filter) {
                AppFilter.ALL -> true
                AppFilter.FREEFORM -> mode == Mode.FREEFORM
                AppFilter.FULLSCREEN -> mode == Mode.FULLSCREEN
                AppFilter.DEFAULT_MODE -> mode == Mode.DEFAULT
                AppFilter.USER -> !app.isSystem
            }
        }
    val selectedCount = apps.count { selected[it.packageName] == true }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .bdTag("app_behavior_root"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.app_behavior_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            // FlowRow wraps the chips onto additional rows when the window is too narrow to fit
            // all five on one line (e.g. when this activity opens in a small freeform window).
            // Diverges from the design's `overflow-x: auto` so every chip stays reachable without
            // a horizontal swipe — and so every chip stays composed for UiAutomator selectors,
            // which skip off-screen content even when it's laid out by horizontalScroll.
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (f in AppFilter.values()) {
                    FilterChip(
                        selected = filter == f,
                        onClick = { onFilterChanged(f) },
                        label = { Text(stringResource(f.labelId)) },
                        leadingIcon = {
                            Icon(
                                imageVector = f.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        shape = BdShape.pill,
                        colors = FilterChipDefaults.filterChipColors(),
                        modifier = Modifier.bdTag(f.tag),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_behavior_installed_apps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = " · ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.app_behavior_selected_count, selectedCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.W600,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text =
                        if (selectedCount > 0) stringResource(R.string.app_behavior_clear_selection)
                        else stringResource(R.string.app_behavior_select_all),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.W600,
                    modifier =
                        Modifier.bdTag(
                                if (selectedCount > 0) "app_behavior_clear_selection"
                                else "app_behavior_select_all"
                            )
                            .clickable(
                                onClick =
                                    if (selectedCount > 0) onClearSelection else onSelectAll
                            ),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 96.dp),
            ) {
                items(items = visible, key = { it.packageName }) { app ->
                    AppRow(
                        entry = app,
                        mode = modes[app.packageName] ?: Mode.DEFAULT,
                        selected = selected[app.packageName] == true,
                        onCheckboxToggle = { onCheckboxToggle(app.packageName) },
                        onClick = { onRowClick(app) },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectedCount > 0,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .bdTag("app_behavior_bulk_bar"),
        ) {
            BulkActionBar(
                count = selectedCount,
                onApply = onBulkApply,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AppRow(
    entry: AppEntry,
    mode: Mode,
    selected: Boolean,
    onCheckboxToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val rowBg =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(color = rowBg, shape = RoundedCornerShape(20.dp))
                .bdTag("app_behavior_row_${entry.packageName}")
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckboxBox(
            selected = selected,
            onClick = onCheckboxToggle,
            packageName = entry.packageName,
        )
        Spacer(Modifier.width(12.dp))
        val icon = entry.icon
        if (icon != null) {
            Image(
                bitmap = remember(icon) { icon.toBitmap().asImageBitmap() },
                contentDescription = stringResource(R.string.hint_app_icon),
                modifier =
                    Modifier.size(44.dp).background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                    ),
            )
        } else {
            Spacer(modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.W500),
            )
            Text(
                text = entry.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        ModeChip(mode)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CheckboxBox(selected: Boolean, onClick: () -> Unit, packageName: String) {
    val border =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier =
            Modifier.size(22.dp)
                .background(color = bg, shape = RoundedCornerShape(6.dp))
                .border(width = 2.dp, color = border, shape = RoundedCornerShape(6.dp))
                .bdTag("app_behavior_checkbox_$packageName")
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ModeChip(mode: Mode) {
    val (bg, fg, icon, labelId) =
        when (mode) {
            Mode.FREEFORM ->
                Quad(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    MaterialTheme.colorScheme.primary,
                    Icons.Outlined.PictureInPicture,
                    R.string.app_behavior_mode_chip_freeform,
                )
            Mode.FULLSCREEN ->
                Quad(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f),
                    MaterialTheme.colorScheme.tertiary,
                    Icons.Outlined.FitScreen,
                    R.string.app_behavior_mode_chip_fullscreen,
                )
            Mode.DEFAULT ->
                Quad(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    Icons.Outlined.SettingsSuggest,
                    R.string.app_behavior_mode_chip_default,
                )
        }
    Surface(color = bg, shape = BdShape.pill) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(labelId),
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.W600,
            )
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
private fun BulkActionBar(count: Int, onApply: (Mode) -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // FlowRow lets the bar wrap onto a second line on narrow windows (the activity opens in a
        // freeform window by default and may not be wide enough for the count + three pill
        // buttons in one row).
        FlowRow(
            modifier = Modifier.padding(start = 18.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.app_behavior_selected_count, count),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.W600,
                modifier = Modifier.padding(end = 4.dp),
            )
            Button(
                onClick = { onApply(Mode.FREEFORM) },
                shape = BdShape.pill,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.bdTag("app_behavior_bulk_freeform"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PictureInPicture,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(text = stringResource(R.string.app_behavior_bulk_freeform))
            }
            Button(
                onClick = { onApply(Mode.FULLSCREEN) },
                shape = BdShape.pill,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.bdTag("app_behavior_bulk_fullscreen"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FitScreen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(text = stringResource(R.string.app_behavior_bulk_fullscreen))
            }
            IconButton(
                onClick = { onApply(Mode.DEFAULT) },
                modifier = Modifier.bdTag("app_behavior_bulk_reset"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = stringResource(R.string.app_behavior_bulk_reset),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ModeSheetContent(target: AppEntry, current: Mode, onPick: (Mode) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = target.icon
            if (icon != null) {
                Image(
                    bitmap = remember(icon) { icon.toBitmap().asImageBitmap() },
                    contentDescription = stringResource(R.string.hint_app_icon),
                    modifier =
                        Modifier.size(40.dp).background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = CircleShape,
                        ),
                )
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = target.label,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = target.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ModeOption(
            title = stringResource(R.string.app_behavior_mode_default),
            subtitle = stringResource(R.string.app_behavior_mode_default_desc),
            trailing = Icons.Outlined.SettingsSuggest,
            selected = current == Mode.DEFAULT,
            onClick = { onPick(Mode.DEFAULT) },
        )
        ModeOption(
            title = stringResource(R.string.app_behavior_mode_freeform),
            subtitle = stringResource(R.string.app_behavior_mode_freeform_desc),
            trailing = Icons.Outlined.PictureInPicture,
            selected = current == Mode.FREEFORM,
            onClick = { onPick(Mode.FREEFORM) },
        )
        ModeOption(
            title = stringResource(R.string.app_behavior_mode_fullscreen),
            subtitle = stringResource(R.string.app_behavior_mode_fullscreen_desc),
            trailing = Icons.Outlined.FitScreen,
            selected = current == Mode.FULLSCREEN,
            onClick = { onPick(Mode.FULLSCREEN) },
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ModeOption(
    title: String,
    subtitle: String,
    trailing: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = trailing,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun chipLabel(context: Context, mode: Mode): String =
    context.getString(
        when (mode) {
            Mode.DEFAULT -> R.string.app_behavior_mode_chip_default
            Mode.FREEFORM -> R.string.app_behavior_mode_chip_freeform
            Mode.FULLSCREEN -> R.string.app_behavior_mode_chip_fullscreen
        }
    )

private fun modeFullLabel(context: Context, mode: Mode): String =
    context.getString(
        when (mode) {
            Mode.DEFAULT -> R.string.app_behavior_mode_default
            Mode.FREEFORM -> R.string.app_behavior_mode_freeform
            Mode.FULLSCREEN -> R.string.app_behavior_mode_fullscreen
        }
    )

private fun applyMode(
    resolver: ContentResolver,
    modes: SnapshotStateMap<String, Mode>,
    pkg: String,
    mode: Mode,
) {
    if (mode == Mode.DEFAULT) {
        modes.remove(pkg)
    } else {
        modes[pkg] = mode
    }
    AppWindowingModeStore.put(resolver, pkg, mode)
}

private fun loadInstalledApps(context: Context): List<AppEntry> {
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val activityInfoList = mutableListOf<LauncherActivityInfo>()
    for (userHandle in userManager.userProfiles) {
        activityInfoList.addAll(launcherApps.getActivityList(null, userHandle))
    }
    return activityInfoList
        .map { info ->
            val appInfo = info.applicationInfo
            AppEntry(
                label = info.label.toString(),
                packageName = appInfo.packageName,
                icon = info.getIcon(0),
                isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
            )
        }
        .sortedBy { it.label.lowercase() }
}
