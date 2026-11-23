package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.RadioStation
import com.example.ui.components.AddCustomStreamDialog
import com.example.ui.components.AntennaStatusDialog
import com.example.ui.components.AssignPresetSlotDialog
import com.example.ui.components.CountryPickerDialog
import com.example.ui.components.StationImageRecognitionDialog
import com.example.ui.screens.CockpitScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.StationsScreen
import com.example.ui.theme.AutoTunerTheme
import com.example.ui.viewmodel.RadioViewModel
import kotlinx.serialization.Serializable

@Serializable
data object CockpitRoute

@Serializable
data object StationsRoute

@Serializable
data object PresetsRoute

private data class NavDestinationItem(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTunerApp(
    viewModel: RadioViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val isDarkCockpit by viewModel.isDarkCockpit.collectAsStateWithLifecycle()
    val isDrivingMode by viewModel.isDrivingMode.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val filteredStations by viewModel.filteredStations.collectAsStateWithLifecycle()
    val dabStations by viewModel.dabStations.collectAsStateWithLifecycle()
    val fmStations by viewModel.fmStations.collectAsStateWithLifecycle()
    val presetStations by viewModel.presetStations.collectAsStateWithLifecycle()
    val favoriteStations by viewModel.favoriteStations.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val selectedBand by viewModel.selectedBand.collectAsStateWithLifecycle()
    val activeCountry by viewModel.activeCountry.collectAsStateWithLifecycle()
    val availableCountries by viewModel.availableCountries.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val imageRecognitionState by viewModel.imageRecognitionState.collectAsStateWithLifecycle()
    val carHardwareState by viewModel.carHardwareState.collectAsStateWithLifecycle()

    var showAddStreamDialog by remember { mutableStateOf(false) }
    var showCountryPickerDialog by remember { mutableStateOf(false) }
    var showImageRecognitionDialog by remember { mutableStateOf(false) }
    var showAntennaDialog by remember { mutableStateOf(false) }
    var stationToAssignPreset by remember { mutableStateOf<RadioStation?>(null) }
    var isLandscapeLocked by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    val navItems = remember {
        listOf(
            NavDestinationItem(
                route = CockpitRoute,
                labelRes = R.string.nav_cockpit,
                selectedIcon = Icons.Filled.Tune,
                unselectedIcon = Icons.Outlined.Tune,
                testTag = "nav_cockpit"
            ),
            NavDestinationItem(
                route = StationsRoute,
                labelRes = R.string.nav_stations,
                selectedIcon = Icons.Filled.Radio,
                unselectedIcon = Icons.Outlined.Radio,
                testTag = "nav_stations"
            ),
            NavDestinationItem(
                route = PresetsRoute,
                labelRes = R.string.nav_presets,
                selectedIcon = Icons.Filled.Bookmark,
                unselectedIcon = Icons.Outlined.BookmarkBorder,
                testTag = "nav_presets"
            )
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    AutoTunerTheme(darkTheme = isDarkCockpit) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isHeadUnitLandscape = maxWidth >= 560.dp && maxWidth > maxHeight
            val useNavigationRail = isHeadUnitLandscape || maxWidth >= 680.dp

            val toggleLandscapeOrientation = {
                val next = !isLandscapeLocked
                isLandscapeLocked = next
                activity?.requestedOrientation = if (next) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (isHeadUnitLandscape) {
                        // Slim 48dp Automotive Head-Unit Top Status Bar for 8-inch Monitors
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.foundation.layout.Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                        .height(48.dp)
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Radio,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.app_name),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = if (this@BoxWithConstraints.maxWidth >= 960.dp) "10\" STARTSEITE" else "8\" STARTSEITE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { showImageRecognitionDialog = true },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("top_image_recognition_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CenterFocusStrong,
                                                contentDescription = stringResource(R.string.cd_image_recognition),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = toggleLandscapeOrientation,
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("landscape_toggle_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ScreenRotation,
                                                contentDescription = stringResource(R.string.cd_toggle_landscape),
                                                tint = if (isLandscapeLocked) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                        IconButton(
                                            onClick = { showCountryPickerDialog = true },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("top_country_picker_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Public,
                                                contentDescription = stringResource(R.string.cd_country_select)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.toggleDrivingMode() },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("driving_mode_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DirectionsCar,
                                                contentDescription = stringResource(R.string.cd_driving_mode),
                                                tint = if (isDrivingMode) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                        IconButton(
                                            onClick = { showAddStreamDialog = true },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("top_add_stream_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = stringResource(R.string.cd_add_station)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.toggleTheme() },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("theme_toggle_button")
                                        ) {
                                            Icon(
                                                imageVector = if (isDarkCockpit) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                                contentDescription = stringResource(R.string.cd_theme_toggle)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    } else {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Radio,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { showImageRecognitionDialog = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("top_image_recognition_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CenterFocusStrong,
                                        contentDescription = stringResource(R.string.cd_image_recognition),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = toggleLandscapeOrientation,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("landscape_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ScreenRotation,
                                        contentDescription = stringResource(R.string.cd_toggle_landscape),
                                        tint = if (isLandscapeLocked) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = { showCountryPickerDialog = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("top_country_picker_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Public,
                                        contentDescription = stringResource(R.string.cd_country_select)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.toggleDrivingMode() },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("driving_mode_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DirectionsCar,
                                        contentDescription = stringResource(R.string.cd_driving_mode),
                                        tint = if (isDrivingMode) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.toggleTheme() },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isDarkCockpit) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                        contentDescription = stringResource(R.string.cd_theme_toggle)
                                    )
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            navItems.forEach { item ->
                                val selected = currentDestination?.hasRoute(item.route::class) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(item.labelRes),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Spacer(Modifier.height(6.dp))
                            navItems.forEach { item ->
                                val selected = currentDestination?.hasRoute(item.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = CockpitRoute,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable<CockpitRoute> {
                            CockpitScreen(
                                nowPlaying = nowPlaying,
                                filteredStations = filteredStations,
                                dabStations = dabStations,
                                fmStations = fmStations,
                                selectedBand = selectedBand,
                                activeCountry = activeCountry,
                                isDrivingMode = isDrivingMode,
                                onSelectBand = viewModel::selectBand,
                                onOpenCountryPicker = { showCountryPickerDialog = true },
                                onOpenAntennaDialog = { showAntennaDialog = true },
                                onTuneStation = viewModel::tuneStation,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onPrevious = viewModel::skipPrevious,
                                onNext = viewModel::skipNext,
                                onRetryStream = viewModel::retryCurrentStream,
                                onVolumeChange = viewModel::setVolume,
                                onToggleMute = viewModel::toggleMute,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onRequestAssignPresetDialog = { stationToAssignPreset = it }
                            )
                        }
                        composable<StationsRoute> {
                            StationsScreen(
                                stations = filteredStations,
                                currentStationId = nowPlaying.station?.id,
                                searchQuery = searchQuery,
                                selectedBand = selectedBand,
                                activeCountry = activeCountry,
                                selectedGenre = selectedGenre,
                                isSyncing = isSyncing,
                                onSearchChange = viewModel::setSearchQuery,
                                onSelectBand = viewModel::selectBand,
                                onSelectCountry = viewModel::selectCountry,
                                onOpenCountryPicker = { showCountryPickerDialog = true },
                                onSelectGenre = viewModel::selectGenre,
                                onRefreshOnline = { viewModel.syncStationsFromApi() },
                                onOpenAddCustomStream = { showAddStreamDialog = true },
                                onOpenImageRecognition = { showImageRecognitionDialog = true },
                                onTuneStation = { station, queue ->
                                    viewModel.tuneStation(station, queue)
                                },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onRequestAssignPreset = { stationToAssignPreset = it },
                                onDeleteCustomStation = viewModel::deleteCustomStation
                            )
                        }
                        composable<PresetsRoute> {
                            PresetsScreen(
                                presets = presetStations,
                                favorites = favoriteStations,
                                recentlyPlayed = recentlyPlayed,
                                currentStationId = nowPlaying.station?.id,
                                onTuneStation = viewModel::tuneStation,
                                onAssignCurrentToPreset = viewModel::assignCurrentToPreset,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onRequestAssignPreset = { stationToAssignPreset = it }
                            )
                        }
                    }
                }
            }

            if (showAddStreamDialog) {
                AddCustomStreamDialog(
                    onDismiss = { showAddStreamDialog = false },
                    onSave = { name, url, band, freq ->
                        viewModel.addCustomStation(name, url, band, freq)
                        showAddStreamDialog = false
                    }
                )
            }

            if (showCountryPickerDialog) {
                CountryPickerDialog(
                    countries = availableCountries,
                    selectedCode = activeCountry.code,
                    onSelectCountry = viewModel::selectCountry,
                    onDismiss = { showCountryPickerDialog = false }
                )
            }

            if (showImageRecognitionDialog) {
                StationImageRecognitionDialog(
                    state = imageRecognitionState,
                    currentStationId = nowPlaying.station?.id,
                    onPickImageUri = viewModel::recognizeStationFromUri,
                    onCaptureBitmap = viewModel::recognizeStationFromBitmap,
                    onTuneStation = viewModel::tuneStation,
                    onRequestAssignPreset = { stationToAssignPreset = it },
                    onApplySearchQuery = { query ->
                        viewModel.setSearchQuery(query)
                        navController.navigate(StationsRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onReset = viewModel::resetImageRecognition,
                    onDismiss = { showImageRecognitionDialog = false }
                )
            }

            if (showAntennaDialog) {
                AntennaStatusDialog(
                    hardwareState = carHardwareState,
                    reception = nowPlaying.reception,
                    onRequestUsbPermission = viewModel::requestUsbDabPermission,
                    onRefreshHardware = viewModel::refreshHardwareStatus,
                    onRefreshImages = { viewModel.refreshStationImages(showFeedback = true) },
                    onDismiss = { showAntennaDialog = false }
                )
            }

            stationToAssignPreset?.let { station ->
                AssignPresetSlotDialog(
                    station = station,
                    presets = presetStations,
                    onAssignSlot = { slot ->
                        viewModel.assignStationToPreset(station, slot)
                    },
                    onDismiss = { stationToAssignPreset = null }
                )
            }
        }
    }
}
