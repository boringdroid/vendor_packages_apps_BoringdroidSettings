package com.boringdroid.settings

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asImageBitmap
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

private enum class AppFilter(val stringId: Int) {
    ALL(R.string.app_behavior_filter_all),
    USER(R.string.app_behavior_filter_user),
    SYSTEM(R.string.app_behavior_filter_system),
    MODIFIED(R.string.app_behavior_filter_modified),
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
                filter = filter,
                onFilterChanged = { filter = it },
                onRowClick = { sheetTarget = it },
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
                    val label = modeLabel(context, picked)
                    val undoMsg =
                        context.getString(
                            R.string.app_behavior_snackbar_updated,
                            target.label,
                            label,
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
    filter: AppFilter,
    onFilterChanged: (AppFilter) -> Unit,
    onRowClick: (AppEntry) -> Unit,
) {
    val visible =
        apps.filter { app ->
            when (filter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !app.isSystem
                AppFilter.SYSTEM -> app.isSystem
                AppFilter.MODIFIED ->
                    (modes[app.packageName] ?: Mode.DEFAULT) != Mode.DEFAULT
            }
        }

    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        Text(
            text = stringResource(R.string.app_behavior_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = AppFilter.values().toList()) { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { onFilterChanged(f) },
                    label = { Text(stringResource(f.stringId)) },
                    shape = BdShape.pill,
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            items(items = visible, key = { it.packageName }) { app ->
                AppRow(
                    entry = app,
                    mode = modes[app.packageName] ?: Mode.DEFAULT,
                    onClick = { onRowClick(app) },
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AppRow(entry: AppEntry, mode: Mode, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .semantics {
                    testTagsAsResourceId = true
                    testTag = "app_behavior_row_${entry.packageName}"
                }
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Spacer(Modifier.width(12.dp))
        ModeChip(mode)
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ModeChip(mode: Mode) {
    val (bg, fg) =
        when (mode) {
            Mode.FREEFORM ->
                MaterialTheme.colorScheme.primaryContainer to
                    MaterialTheme.colorScheme.onPrimaryContainer
            Mode.FULLSCREEN ->
                MaterialTheme.colorScheme.tertiaryContainer to
                    MaterialTheme.colorScheme.onTertiaryContainer
            Mode.DEFAULT ->
                MaterialTheme.colorScheme.surfaceContainerHigh to
                    MaterialTheme.colorScheme.onSurfaceVariant
        }
    val labelId =
        when (mode) {
            Mode.FREEFORM -> R.string.app_behavior_mode_freeform
            Mode.FULLSCREEN -> R.string.app_behavior_mode_fullscreen
            Mode.DEFAULT -> R.string.app_behavior_mode_default
        }
    Surface(color = bg, shape = BdShape.pill) {
        Text(
            text = stringResource(labelId),
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
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
                            shape = RoundedCornerShape(12.dp),
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
            selected = current == Mode.DEFAULT,
            onClick = { onPick(Mode.DEFAULT) },
        )
        ModeOption(
            title = stringResource(R.string.app_behavior_mode_freeform),
            subtitle = stringResource(R.string.app_behavior_mode_freeform_desc),
            selected = current == Mode.FREEFORM,
            onClick = { onPick(Mode.FREEFORM) },
        )
        ModeOption(
            title = stringResource(R.string.app_behavior_mode_fullscreen),
            subtitle = stringResource(R.string.app_behavior_mode_fullscreen_desc),
            selected = current == Mode.FULLSCREEN,
            onClick = { onPick(Mode.FULLSCREEN) },
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ModeOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
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
    }
}

private fun modeLabel(context: Context, mode: Mode): String =
    context.getString(
        when (mode) {
            Mode.DEFAULT -> R.string.app_behavior_mode_default
            Mode.FREEFORM -> R.string.app_behavior_mode_freeform
            Mode.FULLSCREEN -> R.string.app_behavior_mode_fullscreen
        }
    )

private fun applyMode(
    resolver: android.content.ContentResolver,
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
