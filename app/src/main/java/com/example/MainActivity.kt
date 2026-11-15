package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.export.ReportShareHelper
import com.example.ui.InspectionViewModel
import com.example.ui.UiEvent
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.InspectScreen
import com.example.ui.screens.RiskExportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FailCrimson
import com.example.ui.theme.PlaySafeTheme

object AppRoutes {
    const val ROUTE_INSPECT = "inspect"
    const val ROUTE_RISK_EXPORT = "risk_export"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_SETTINGS = "settings"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        route = AppRoutes.ROUTE_INSPECT,
        label = "Inspect",
        selectedIcon = Icons.Filled.AssignmentTurnedIn,
        unselectedIcon = Icons.Outlined.AssignmentTurnedIn,
        testTag = "nav_inspect",
    ),
    TopLevelDestination(
        route = AppRoutes.ROUTE_RISK_EXPORT,
        label = "Risk & Export",
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment,
        testTag = "nav_risk_export",
    ),
    TopLevelDestination(
        route = AppRoutes.ROUTE_HISTORY,
        label = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_history",
    ),
    TopLevelDestination(
        route = AppRoutes.ROUTE_SETTINGS,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings",
    ),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlaySafeTheme {
                PlaySafeApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaySafeApp(
    viewModel: InspectionViewModel = viewModel(),
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeReport by viewModel.activeReport.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val savedReports by viewModel.savedReports.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.ROUTE_INSPECT

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.ShareExportedFile -> {
                    ReportShareHelper.shareFile(
                        context = context,
                        file = event.file,
                        mimeType = event.mimeType,
                        chooserTitle = event.chooserTitle,
                        report = event.report,
                    )
                }
                UiEvent.NavigateToInspect -> {
                    navigateSingleTop(navController, AppRoutes.ROUTE_INSPECT)
                }
                UiEvent.NavigateToRiskExport -> {
                    navigateSingleTop(navController, AppRoutes.ROUTE_RISK_EXPORT)
                }
            }
        }
    }

    val topBarTitle = when (currentRoute) {
        AppRoutes.ROUTE_INSPECT -> "PlaySafe Audit"
        AppRoutes.ROUTE_RISK_EXPORT -> "Risk Assessment & Export"
        AppRoutes.ROUTE_HISTORY -> "Saved Inspections"
        AppRoutes.ROUTE_SETTINGS -> "Equipment Settings"
        else -> "PlaySafe Audit"
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = topBarTitle,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    actions = {
                        if (currentRoute == AppRoutes.ROUTE_INSPECT) {
                            IconButton(
                                onClick = { viewModel.saveActiveReport() },
                                modifier = Modifier.testTag("topbar_save_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Save,
                                    contentDescription = "Save inspection offline",
                                )
                            }
                            IconButton(
                                onClick = { viewModel.startNewInspection() },
                                modifier = Modifier.testTag("topbar_new_inspection_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NoteAdd,
                                    contentDescription = "Start new inspection",
                                )
                            }
                        }
                    },
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateSingleTop(navController, dest.route) },
                                icon = {
                                    DestinationIcon(
                                        destination = dest,
                                        selected = selected,
                                        failCount = activeReport.failCount,
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavRail) {
                    NavigationRail {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateSingleTop(navController, dest.route) },
                                icon = {
                                    DestinationIcon(
                                        destination = dest,
                                        selected = selected,
                                        failCount = activeReport.failCount,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.ROUTE_INSPECT,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(AppRoutes.ROUTE_INSPECT) {
                        InspectScreen(
                            report = activeReport,
                            selectedCategoryFilter = selectedCategoryFilter,
                            onSelectCategoryFilter = viewModel::selectCategoryFilter,
                            onInspectorNameChange = viewModel::updateInspectorName,
                            onSiteNameChange = viewModel::updateSiteName,
                            onDateChange = viewModel::updateInspectionDate,
                            onTimeChange = viewModel::updateInspectionTime,
                            onWeatherChange = viewModel::updateWeatherConditions,
                            onItemStatusChange = viewModel::setItemStatus,
                            onItemNoteChange = viewModel::updateItemNote,
                            onItemDefectDetailsChange = viewModel::updateItemDefectDetails,
                            onItemRiskLevelChange = viewModel::updateItemRiskLevel,
                            onItemActionRequiredChange = viewModel::updateItemActionRequired,
                            onMarkCategoryAllPass = viewModel::markCategoryAllPass,
                            onToggleCategoryNa = viewModel::toggleCategoryNa,
                            onAddMaintenanceLog = viewModel::addMaintenanceLog,
                            onRemoveMaintenanceLog = viewModel::removeMaintenanceLog,
                            onOverallCommentsChange = viewModel::updateOverallComments,
                            onOverallActionChange = viewModel::updateOverallActionRequired,
                            onSignatureChange = viewModel::updateInspectorSignature,
                            onSaveReport = viewModel::saveActiveReport,
                            onNavigateToRiskExport = {
                                navigateSingleTop(navController, AppRoutes.ROUTE_RISK_EXPORT)
                            },
                            onNavigateToSettings = {
                                navigateSingleTop(navController, AppRoutes.ROUTE_SETTINGS)
                            },
                        )
                    }

                    composable(AppRoutes.ROUTE_RISK_EXPORT) {
                        RiskExportScreen(
                            report = activeReport,
                            isExporting = isExporting,
                            onSharePdfClick = { viewModel.exportAndSharePdf() },
                            onExportJsonClick = { viewModel.exportAndShareJson() },
                            onSaveOfflineClick = { viewModel.saveActiveReport() },
                            onNewInspectionClick = { viewModel.startNewInspection() },
                            onFixInChecklistClick = {
                                navigateSingleTop(navController, AppRoutes.ROUTE_INSPECT)
                            },
                            onUpdateDefectDetails = viewModel::updateItemDefectDetails,
                            onUpdateRiskLevel = viewModel::updateItemRiskLevel,
                            onUpdateActionRequired = viewModel::updateItemActionRequired,
                        )
                    }

                    composable(AppRoutes.ROUTE_HISTORY) {
                        HistoryScreen(
                            savedReports = savedReports,
                            onResumeReport = viewModel::loadSavedReport,
                            onExportPdf = viewModel::exportSavedEntityPdf,
                            onExportJson = viewModel::exportSavedEntityJson,
                            onDeleteReport = viewModel::deleteSavedReport,
                            onStartInspectionClick = {
                                navigateSingleTop(navController, AppRoutes.ROUTE_INSPECT)
                            },
                        )
                    }

                    composable(AppRoutes.ROUTE_SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            onToggleCategory = viewModel::setCategoryEnabled,
                            onToggleQuestion = viewModel::setQuestionEnabled,
                            onAddCustomQuestion = viewModel::addCustomQuestion,
                            onRemoveCustomQuestion = viewModel::removeCustomQuestion,
                            onResetDefaults = viewModel::resetChecklistDefaults,
                            onDefaultInspectorNameChange = viewModel::setDefaultInspectorName,
                            onDefaultOrganizationChange = viewModel::setDefaultOrganization,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationIcon(
    destination: TopLevelDestination,
    selected: Boolean,
    failCount: Int,
) {
    val iconVector = if (selected) destination.selectedIcon else destination.unselectedIcon
    if (destination.route == AppRoutes.ROUTE_RISK_EXPORT && failCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = FailCrimson) {
                    Text(
                        text = failCount.toString(),
                        color = MaterialTheme.colorScheme.onError,
                    )
                }
            },
        ) {
            Icon(imageVector = iconVector, contentDescription = destination.label)
        }
    } else {
        Icon(imageVector = iconVector, contentDescription = destination.label)
    }
}

private fun navigateSingleTop(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
