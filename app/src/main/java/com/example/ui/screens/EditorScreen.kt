package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.IconEditorViewModel
import com.example.ui.viewmodel.ScreenState

enum class EditorTab(val title: String, val icon: @Composable () -> Unit) {
    PHOTO("Photo", { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) }),
    PRESETS("Presets", { Icon(Icons.Default.AutoAwesome, contentDescription = null) }),
    FILTERS("Filters", { Icon(Icons.Default.FilterBAndW, contentDescription = null) }),
    EFFECTS("Effects", { Icon(Icons.Default.Tune, contentDescription = null) }),
    SHAPE_BORDER("Shape", { Icon(Icons.Default.CropSquare, contentDescription = null) }),
    BACKGROUND("Backdrop", { Icon(Icons.Default.Wallpaper, contentDescription = null) }),
    SYMBOL_TEXT("Symbol", { Icon(Icons.Default.EmojiSymbols, contentDescription = null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: IconEditorViewModel
) {
    val context = LocalContext.current
    val project by viewModel.currentProject.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val isRendering by viewModel.isRendering.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    var selectedTab by remember { mutableStateOf(EditorTab.PHOTO) }
    var showExportDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.updateProject({
                it.copy(customImageUri = uri.toString())
            })
            Toast.makeText(context, "Photo loaded!", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        viewModel.navigateTo(ScreenState.DASHBOARD)
    }

    Scaffold(
        containerColor = NoirBlack,
        topBar = {
            Surface(
                color = NoirSurface,
                border = BorderStroke(1.dp, NoirBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.navigateTo(ScreenState.DASHBOARD) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkPureWhite)
                        }
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = canUndo
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo) InkPureWhite else InkMediumGray
                            )
                        }
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = canRedo
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) InkPureWhite else InkMediumGray
                            )
                        }
                    }

                    // Project Name / Title
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.saveCurrentProject() }) {
                            Icon(Icons.Default.Save, contentDescription = "Save Project", tint = InkPureWhite)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = { showExportDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InkPureWhite,
                                contentColor = InkPureBlack
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("EXPORT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val config = LocalConfiguration.current
        val isTabletLandscape = config.screenWidthDp > 700

        if (isTabletLandscape) {
            // Tablet two-column layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LivePreviewArea(previewBitmap, isRendering, project)
                }

                Surface(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    color = NoirDark,
                    border = BorderStroke(1.dp, NoirBorder)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabsSelector(selectedTab, onSelect = { selectedTab = it })
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            TabContent(selectedTab, project, viewModel, photoPickerLauncher)
                        }
                    }
                }
            }
        } else {
            // Phone vertical layout: Large Preview on top, controls tab bar and content below
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Live preview viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(NoirBlack)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LivePreviewArea(previewBitmap, isRendering, project)
                }

                // Bottom control panel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.35f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = NoirSurface,
                    border = BorderStroke(1.dp, NoirBorder)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabsSelector(selectedTab, onSelect = { selectedTab = it })
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .navigationBarsPadding()
                        ) {
                            TabContent(selectedTab, project, viewModel, photoPickerLauncher)
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        ExportDialog(
            viewModel = viewModel,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun LivePreviewArea(
    bitmap: Bitmap?,
    isRendering: Boolean,
    project: IconProject
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Transparent checkerboard or dark frame
        Surface(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(32.dp)),
            color = NoirSurfaceElevated,
            border = BorderStroke(1.5.dp, NoirBorderLight),
            shape = RoundedCornerShape(32.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Live Icon Preview",
                        modifier = Modifier
                            .size(220.dp)
                    )
                }

                if (isRendering) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = InkPureWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Badge showing active style and shape
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NoirCard,
            border = BorderStroke(0.5.dp, NoirBorder)
        ) {
            Text(
                text = "${project.filter.displayName.uppercase()} • ${project.shape.displayName.uppercase()}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = InkMediumGray
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun TabsSelector(
    selectedTab: EditorTab,
    onSelect: (EditorTab) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoirDark)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(EditorTab.values()) { tab ->
            val isSelected = selectedTab == tab
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) InkPureWhite else NoirSurfaceElevated,
                border = BorderStroke(1.dp, if (isSelected) InkPureWhite else NoirBorder),
                modifier = Modifier.clickable { onSelect(tab) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides if (isSelected) InkPureBlack else InkLightGray
                    ) {
                        tab.icon()
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSelected) InkPureBlack else InkLightGray
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TabContent(
    tab: EditorTab,
    project: IconProject,
    viewModel: IconEditorViewModel,
    photoPickerLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>
) {
    when (tab) {
        EditorTab.PHOTO -> PhotoAndCropTab(project, viewModel, photoPickerLauncher)
        EditorTab.PRESETS -> PresetsTab(project, viewModel)
        EditorTab.FILTERS -> FiltersTab(project, viewModel)
        EditorTab.EFFECTS -> EffectsTab(project, viewModel)
        EditorTab.SHAPE_BORDER -> ShapeAndBorderTab(project, viewModel)
        EditorTab.BACKGROUND -> BackgroundTab(project, viewModel)
        EditorTab.SYMBOL_TEXT -> SymbolAndTextTab(project, viewModel)
    }
}

// -------------------------------------------------------------
// TAB 1: Photo & Crop
// -------------------------------------------------------------
@Composable
fun PhotoAndCropTab(
    project: IconProject,
    viewModel: IconEditorViewModel,
    photoPickerLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Upload Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NoirButton(
                text = if (project.customImageUri != null) "Replace Photo" else "Upload Photo",
                isPrimary = true,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(Icons.Default.Upload, contentDescription = null, tint = InkPureBlack)
                },
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                testTag = "upload_photo_button"
            )

            if (project.customImageUri != null) {
                NoirButton(
                    text = "Clear Photo",
                    isPrimary = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.updateProject({ it.copy(customImageUri = null) })
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Built-in Artwork Grid
        Text(
            text = "OR CHOOSE BUILT-IN ARTWORK",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = InkMediumGray
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(BuiltInArtwork.values()) { art ->
                val isSelected = project.customImageUri == null && project.builtInArtwork == art
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) NoirSurfaceElevated else NoirCard,
                    border = BorderStroke(1.5.dp, if (isSelected) InkPureWhite else NoirBorder),
                    modifier = Modifier.clickable {
                        viewModel.updateProject({
                            it.copy(
                                builtInArtwork = art,
                                customImageUri = null
                            )
                        })
                    }
                ) {
                    Text(
                        text = art.displayName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            color = if (isSelected) InkPureWhite else InkLightGray
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Crop & Transform Controls
        SectionHeader(
            title = "Crop & Transform",
            subtitle = "Pan, zoom, and rotate the subject inside the square mask"
        )

        // Quick crop buttons: Fit, Fill, Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NoirButton(
                text = "Fit",
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.fitCrop() }
            )
            NoirButton(
                text = "Fill",
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.fillCrop() }
            )
            NoirButton(
                text = "Reset Crop",
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.resetCrop() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        SliderControl(
            title = "Zoom",
            value = project.zoom,
            range = 0.5f..3.0f,
            onValueChange = { z -> viewModel.updateProject({ it.copy(zoom = z) }, addToHistory = false) },
            unit = "x",
            defaultValue = 1.0f,
            onReset = { viewModel.updateProject({ it.copy(zoom = 1.0f) }) }
        )

        SliderControl(
            title = "Rotation",
            value = project.rotation,
            range = -180f..180f,
            onValueChange = { r -> viewModel.updateProject({ it.copy(rotation = r) }, addToHistory = false) },
            unit = "°",
            defaultValue = 0f,
            onReset = { viewModel.updateProject({ it.copy(rotation = 0f) }) }
        )

        SliderControl(
            title = "Horizontal Position (X)",
            value = project.panX,
            range = -100f..100f,
            onValueChange = { px -> viewModel.updateProject({ it.copy(panX = px) }, addToHistory = false) },
            unit = "%",
            defaultValue = 0f,
            onReset = { viewModel.updateProject({ it.copy(panX = 0f) }) }
        )

        SliderControl(
            title = "Vertical Position (Y)",
            value = project.panY,
            range = -100f..100f,
            onValueChange = { py -> viewModel.updateProject({ it.copy(panY = py) }, addToHistory = false) },
            unit = "%",
            defaultValue = 0f,
            onReset = { viewModel.updateProject({ it.copy(panY = 0f) }) }
        )
    }
}

// -------------------------------------------------------------
// TAB 2: Presets
// -------------------------------------------------------------
@Composable
fun PresetsTab(
    project: IconProject,
    viewModel: IconEditorViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "One-Click Presets",
            subtitle = "Curated vintage comic & newsprint monochrome configurations"
        )

        PresetStyle.values().forEach { preset ->
            val isSelected = project.preset == preset
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) NoirSurfaceElevated else NoirCard,
                border = BorderStroke(1.5.dp, if (isSelected) InkPureWhite else NoirBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { viewModel.applyPreset(preset) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.displayName.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = if (isSelected) InkPureWhite else InkOffWhite
                            )
                        )
                        Text(
                            text = preset.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(color = InkMediumGray),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.applyPreset(preset) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = InkPureWhite,
                            unselectedColor = InkMediumGray
                        )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: Filters
// -------------------------------------------------------------
@Composable
fun FiltersTab(
    project: IconProject,
    viewModel: IconEditorViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Monochrome Filter System",
            subtitle = "Select one monochrome processing pipeline"
        )

        IconFilter.values().forEach { filter ->
            val isSelected = project.filter == filter
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) NoirSurfaceElevated else NoirCard,
                border = BorderStroke(1.5.dp, if (isSelected) InkPureWhite else NoirBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { viewModel.updateProject({ it.copy(filter = filter, preset = null) }) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = filter.displayName.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = if (isSelected) InkPureWhite else InkOffWhite
                            )
                        )
                        Text(
                            text = filter.description,
                            style = MaterialTheme.typography.bodySmall.copy(color = InkMediumGray),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.updateProject({ it.copy(filter = filter, preset = null) }) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = InkPureWhite,
                            unselectedColor = InkMediumGray
                        )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: Effects
// -------------------------------------------------------------
@Composable
fun EffectsTab(
    project: IconProject,
    viewModel: IconEditorViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = "Adjustable Effects",
                subtitle = "Fine-tune contrast, ink grain, vignette, and halftone"
            )
        }

        NoirButton(
            text = "Reset All Effects",
            isPrimary = false,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
            },
            onClick = { viewModel.resetEffects() }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SliderControl(
            title = "Contrast",
            value = project.contrast,
            range = -50f..100f,
            onValueChange = { v -> viewModel.updateProject({ it.copy(contrast = v) }, addToHistory = false) },
            defaultValue = 35f,
            onReset = { viewModel.updateProject({ it.copy(contrast = 35f) }) }
        )

        SliderControl(
            title = "Brightness",
            value = project.brightness,
            range = -50f..50f,
            onValueChange = { v -> viewModel.updateProject({ it.copy(brightness = v) }, addToHistory = false) },
            defaultValue = 0f,
            onReset = { viewModel.updateProject({ it.copy(brightness = 0f) }) }
        )

        SliderControl(
            title = "Exposure",
            value = project.exposure,
            range = -50f..50f,
            onValueChange = { v -> viewModel.updateProject({ it.copy(exposure = v) }, addToHistory = false) },
            defaultValue = 0f,
            onReset = { viewModel.updateProject({ it.copy(exposure = 0f) }) }
        )

        SliderControl(
            title = "Ink Threshold",
            value = project.threshold,
            range = 10f..90f,
            onValueChange = { v -> viewModel.updateProject({ it.copy(threshold = v) }, addToHistory = false) },
            defaultValue = 50f,
            onReset = { viewModel.updateProject({ it.copy(threshold = 50f) }) }
        )

        SliderControl(
            title = "Paper Grain",
            value = project.grain,
            range = 0f..80f,
            onValueChange = { v -> viewModel.updateProject({ it.copy(grain = v) }, addToHistory = false) },
            defaultValue = 15f,
            onReset = { viewModel.updateProject({ it.copy(grain = 15f) }) }
        )

        SliderControl(
            title = "Halftone Matrix",
            value = project.halftone,
            range = 0f..100f,
            onValueChange = { v -> viewModel.updateProject({ it.copy(halftone = v) }, addToHistory = false) },
            defaultValue = 0f,
            onReset = { viewModel.updateProject({ it.copy(halftone = 0f) }) }
        )

        SliderControl(
            title = "Noir Vignette",
            value = project.vignette,
            range = 0f..100f,
            onValueChange = { v -> viewModel.updateProject({ it.copy(vignette = v) }, addToHistory = false) },
            defaultValue = 25f,
            onReset = { viewModel.updateProject({ it.copy(vignette = 25f) }) }
        )

        SliderControl(
            title = "Sharpen",
            value = project.sharpen,
            range = 0f..100f,
            onValueChange = { v -> viewModel.updateProject({ it.copy(sharpen = v) }, addToHistory = false) },
            defaultValue = 20f,
            onReset = { viewModel.updateProject({ it.copy(sharpen = 20f) }) }
        )
    }
}

// -------------------------------------------------------------
// TAB 5: Shape & Border
// -------------------------------------------------------------
@Composable
fun ShapeAndBorderTab(
    project: IconProject,
    viewModel: IconEditorViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Icon Shape",
            subtitle = "Select mask silhouette and corner curvature"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconShape.values().forEach { shape ->
                val isSelected = project.shape == shape
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) InkPureWhite else NoirCard,
                    border = BorderStroke(1.dp, if (isSelected) InkPureWhite else NoirBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.updateProject({ it.copy(shape = shape) }) }
                ) {
                    Text(
                        text = shape.displayName.replace(" ", "\n"),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) InkPureBlack else InkLightGray
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (project.shape == IconShape.ROUNDED_SQUARE || project.shape == IconShape.SQUIRCLE) {
            SliderControl(
                title = "Corner Radius",
                value = project.cornerRadius,
                range = 5f..50f,
                onValueChange = { r -> viewModel.updateProject({ it.copy(cornerRadius = r) }, addToHistory = false) },
                unit = "%",
                defaultValue = 24f,
                onReset = { viewModel.updateProject({ it.copy(cornerRadius = 24f) }) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(
            title = "Border Style",
            subtitle = "Outer comic stroke and ink accents"
        )

        IconBorder.values().forEach { border ->
            val isSelected = project.border == border
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) NoirSurfaceElevated else NoirCard,
                border = BorderStroke(1.dp, if (isSelected) InkPureWhite else NoirBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { viewModel.updateProject({ it.copy(border = border) }) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = border.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) InkPureWhite else InkLightGray
                        )
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.updateProject({ it.copy(border = border) }) },
                        colors = RadioButtonDefaults.colors(selectedColor = InkPureWhite, unselectedColor = InkMediumGray)
                    )
                }
            }
        }

        if (project.border != IconBorder.NONE) {
            Spacer(modifier = Modifier.height(8.dp))
            SliderControl(
                title = "Border Thickness",
                value = project.borderWidth,
                range = 1f..12f,
                onValueChange = { w -> viewModel.updateProject({ it.copy(borderWidth = w) }, addToHistory = false) },
                unit = "px",
                defaultValue = 4f,
                onReset = { viewModel.updateProject({ it.copy(borderWidth = 4f) }) }
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 6: Background
// -------------------------------------------------------------
@Composable
fun BackgroundTab(
    project: IconProject,
    viewModel: IconEditorViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Background",
            subtitle = "Solid noir tones, vintage newspaper print, or comic texture"
        )

        IconBackground.values().forEach { bg ->
            val isSelected = project.background == bg
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) NoirSurfaceElevated else NoirCard,
                border = BorderStroke(1.5.dp, if (isSelected) InkPureWhite else NoirBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { viewModel.updateProject({ it.copy(background = bg) }) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Small color/texture badge
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = when (bg) {
                                IconBackground.BLACK -> Color.Black
                                IconBackground.WHITE -> Color.White
                                IconBackground.DARK_GRAY -> Color(0xFF1E1E1E)
                                IconBackground.LIGHT_GRAY -> Color(0xFFD0D0D0)
                                IconBackground.TRANSPARENT -> Color.Transparent
                                IconBackground.NEWSPAPER_TEXTURE -> NewsPaperPaper
                                IconBackground.COMIC_TEXTURE -> Color(0xFF181818)
                            },
                            border = BorderStroke(1.dp, NoirBorderLight)
                        ) {}

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = bg.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) InkPureWhite else InkLightGray
                            )
                        )
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.updateProject({ it.copy(background = bg) }) },
                        colors = RadioButtonDefaults.colors(selectedColor = InkPureWhite, unselectedColor = InkMediumGray)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 7: Symbol & Text
// -------------------------------------------------------------
@Composable
fun SymbolAndTextTab(
    project: IconProject,
    viewModel: IconEditorViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "App Symbol / Overlay",
            subtitle = "Heroic silhouettes & original iconography"
        )

        // Symbols selection grid
        val symbols = AppSymbol.values()
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(symbols) { sym ->
                val isSelected = project.symbol == sym
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) InkPureWhite else NoirCard,
                    border = BorderStroke(1.dp, if (isSelected) InkPureWhite else NoirBorder),
                    modifier = Modifier.clickable {
                        viewModel.updateProject({ it.copy(symbol = sym) })
                    }
                ) {
                    Text(
                        text = sym.displayName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) InkPureBlack else InkLightGray
                        )
                    )
                }
            }
        }

        if (project.symbol != AppSymbol.NONE) {
            Spacer(modifier = Modifier.height(10.dp))

            SliderControl(
                title = "Symbol Scale",
                value = project.symbolSize,
                range = 25f..90f,
                onValueChange = { s -> viewModel.updateProject({ it.copy(symbolSize = s) }, addToHistory = false) },
                unit = "%",
                defaultValue = 55f
            )

            SliderControl(
                title = "Symbol Opacity",
                value = project.symbolOpacity * 100f,
                range = 10f..100f,
                onValueChange = { op -> viewModel.updateProject({ it.copy(symbolOpacity = op / 100f) }, addToHistory = false) },
                unit = "%",
                defaultValue = 100f
            )

            // Symbol Position & Color Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color Toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NoirCard,
                    border = BorderStroke(1.dp, NoirBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.updateProject({ it.copy(isSymbolWhite = !it.isSymbolWhite) }) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (project.isSymbolWhite) "Color: White" else "Color: Black",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = InkPureWhite
                            )
                        )
                    }
                }

                // Position Selector
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NoirCard,
                    border = BorderStroke(1.dp, NoirBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val nextPos = when (project.symbolPosition) {
                                SymbolPosition.CENTER -> SymbolPosition.BOTTOM_RIGHT
                                SymbolPosition.BOTTOM_RIGHT -> SymbolPosition.TOP_RIGHT
                                SymbolPosition.TOP_RIGHT -> SymbolPosition.TOP_LEFT
                                SymbolPosition.TOP_LEFT -> SymbolPosition.BOTTOM_LEFT
                                SymbolPosition.BOTTOM_LEFT -> SymbolPosition.CENTER
                            }
                            viewModel.updateProject({ it.copy(symbolPosition = nextPos) })
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Pos: ${project.symbolPosition.displayName}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = InkPureWhite
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionHeader(
            title = "App Text Label",
            subtitle = "Optional typography overlay inside the icon"
        )

        OutlinedTextField(
            value = project.appText,
            onValueChange = { text -> viewModel.updateProject({ it.copy(appText = text) }) },
            label = { Text("App Name / Text (e.g. BATMAN, NOTES, 12)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = InkPureWhite,
                unfocusedBorderColor = NoirBorder,
                focusedLabelColor = InkPureWhite,
                unfocusedLabelColor = InkMediumGray,
                focusedTextColor = InkPureWhite,
                unfocusedTextColor = InkPureWhite
            )
        )

        if (project.appText.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (project.isUppercase) InkPureWhite else NoirCard,
                    border = BorderStroke(1.dp, if (project.isUppercase) InkPureWhite else NoirBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.updateProject({ it.copy(isUppercase = !it.isUppercase) }) }
                ) {
                    Text(
                        text = "UPPERCASE",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (project.isUppercase) InkPureBlack else InkLightGray
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (project.isTextBold) InkPureWhite else NoirCard,
                    border = BorderStroke(1.dp, if (project.isTextBold) InkPureWhite else NoirBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.updateProject({ it.copy(isTextBold = !it.isTextBold) }) }
                ) {
                    Text(
                        text = "BOLD INK",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (project.isTextBold) InkPureBlack else InkLightGray
                        )
                    )
                }
            }

            SliderControl(
                title = "Font Size",
                value = project.fontSize,
                range = 8f..28f,
                onValueChange = { fs -> viewModel.updateProject({ it.copy(fontSize = fs) }, addToHistory = false) },
                unit = "sp",
                defaultValue = 14f
            )

            SliderControl(
                title = "Letter Spacing",
                value = project.letterSpacing,
                range = 0f..10f,
                onValueChange = { ls -> viewModel.updateProject({ it.copy(letterSpacing = ls) }, addToHistory = false) },
                defaultValue = 2f
            )
        }
    }
}
