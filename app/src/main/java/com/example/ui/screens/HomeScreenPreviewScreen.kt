package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BuiltInArtwork
import com.example.data.model.IconProject
import com.example.image.ImageProcessor
import com.example.ui.components.NoirButton
import com.example.ui.components.SliderControl
import com.example.ui.theme.*
import com.example.ui.viewmodel.IconEditorViewModel
import com.example.ui.viewmodel.ScreenState
import kotlinx.coroutines.launch

@Composable
fun HomeScreenPreviewScreen(
    viewModel: IconEditorViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsState()

    var wallpaperBlur by remember { mutableStateOf(0f) }
    var wallpaperBrightness by remember { mutableStateOf(-15f) }
    var selectedWallpaperType by remember { mutableStateOf(0) } // 0: Newspaper, 1: Dark Comic, 2: Minimal Black, 3: Grunge
    var customWallpaperUri by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customWallpaperUri = uri.toString()
            selectedWallpaperType = -1
        }
    }

    // Cache rendered bitmaps for home screen icons
    val iconBitmaps = remember { mutableStateMapOf<String, Bitmap>() }

    LaunchedEffect(projects) {
        projects.take(16).forEach { p ->
            if (!iconBitmaps.containsKey(p.id)) {
                coroutineScope.launch {
                    try {
                        val bmp = ImageProcessor.renderIconBitmap(context, p, targetSize = 180)
                        iconBitmaps[p.id] = bmp
                    } catch (_: Exception) {}
                }
            }
        }
    }

    Scaffold(
        containerColor = NoirBlack,
        topBar = {
            Surface(
                color = NoirSurface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, NoirBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateTo(ScreenState.DASHBOARD) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = InkPureWhite
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HOME SCREEN PREVIEW",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Realistic monochrome home setup",
                            style = MaterialTheme.typography.bodySmall.copy(color = InkMediumGray)
                        )
                    }
                    IconButton(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload Wallpaper", tint = InkPureWhite)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Phone Simulator Canvas (Takes weighted top space)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .border(2.dp, NoirBorderLight, RoundedCornerShape(36.dp))
            ) {
                // Wallpaper Layer
                WallpaperLayer(
                    selectedType = selectedWallpaperType,
                    customUri = customWallpaperUri,
                    blur = wallpaperBlur,
                    brightness = wallpaperBrightness
                )

                // Home Screen Content Inside Phone Frame
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Status Bar Simulation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "9:41",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = InkPureWhite
                            )
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = InkPureWhite, modifier = Modifier.size(14.dp))
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = InkPureWhite, modifier = Modifier.size(14.dp))
                            Icon(Icons.Default.BatteryFull, contentDescription = null, tint = InkPureWhite, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Vintage Comic Widgets Row (Matching User's Reference!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Big Bat Hero Widget Left
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(R.drawable.img_app_icon),
                                    contentDescription = "Widget",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    text = "Color Widgets",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 4.dp)
                                )
                            }
                        }

                        // Comic LOVE / Typography Widget Right
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "MONO",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            letterSpacing = 4.sp
                                        )
                                    )
                                    Text(
                                        text = "STUDIO",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.7f),
                                            letterSpacing = 3.sp
                                        )
                                    )
                                }
                                Text(
                                    text = "Color Widgets",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 4.dp)
                                )
                            }
                        }
                    }

                    // 4x3 App Icons Grid
                    val gridIcons = projects.take(8)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (row in 0 until 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (col in 0 until 4) {
                                    val index = row * 4 + col
                                    val iconProj = gridIcons.getOrNull(index)
                                    HomeIconItem(
                                        project = iconProj,
                                        cachedBitmap = iconProj?.let { iconBitmaps[it.id] },
                                        onClick = {
                                            if (iconProj != null) {
                                                viewModel.openProject(iconProj)
                                            } else {
                                                viewModel.startNewProject()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Page Indicator Dots
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.4f))
                        )
                    }

                    // Bottom Dock Frame with 4 Essential Icons
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        shape = RoundedCornerShape(26.dp),
                        color = Color.Black.copy(alpha = 0.45f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val dockProjects = projects.take(4)
                            for (i in 0 until 4) {
                                val p = dockProjects.getOrNull(i)
                                HomeIconItem(
                                    project = p,
                                    cachedBitmap = p?.let { iconBitmaps[it.id] },
                                    showLabel = false,
                                    sizeDp = 48,
                                    onClick = {
                                        if (p != null) viewModel.openProject(p)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Wallpaper Customizer Controls
            Surface(
                color = NoirSurface,
                border = BorderStroke(1.dp, NoirBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "WALLPAPER CUSTOMIZER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = InkPureWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Wallpaper Pills
                    val wallpapers = listOf(
                        "Newspaper Collage",
                        "Dark Comic",
                        "Minimal Noir",
                        "Vintage Ink"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(wallpapers.size) { idx ->
                            val isSelected = selectedWallpaperType == idx
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) InkPureWhite else NoirCard,
                                border = BorderStroke(1.dp, if (isSelected) InkPureWhite else NoirBorder),
                                modifier = Modifier.clickable {
                                    selectedWallpaperType = idx
                                    customWallpaperUri = null
                                }
                            ) {
                                Text(
                                    text = wallpapers[idx],
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) InkPureBlack else InkLightGray
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Blur Slider
                        Column(modifier = Modifier.weight(1f)) {
                            SliderControl(
                                title = "Blur",
                                value = wallpaperBlur,
                                range = 0f..20f,
                                onValueChange = { wallpaperBlur = it },
                                unit = "px"
                            )
                        }
                        // Brightness Slider
                        Column(modifier = Modifier.weight(1f)) {
                            SliderControl(
                                title = "Brightness",
                                value = wallpaperBrightness,
                                range = -50f..30f,
                                onValueChange = { wallpaperBrightness = it },
                                unit = "%"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperLayer(
    selectedType: Int,
    customUri: String?,
    blur: Float,
    brightness: Float
) {
    val modifier = Modifier
        .fillMaxSize()
        .then(if (blur > 0.5f) Modifier.blur(blur.dp) else Modifier)

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            customUri != null -> {
                // Uploaded wallpaper
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.img_tex_news),
                    contentDescription = "Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            }
            selectedType == 0 -> {
                // Newspaper texture
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.img_tex_news),
                    contentDescription = "Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            }
            selectedType == 1 -> {
                // Dark Comic Hero
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.img_art_hero),
                    contentDescription = "Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            }
            selectedType == 2 -> {
                // Minimal Noir
                Box(modifier = modifier.background(Color(0xFF0A0A0A)))
            }
            else -> {
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.img_tex_news),
                    contentDescription = "Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            }
        }

        // Brightness / tint overlay
        val overlayAlpha = ((-brightness) / 100f).coerceIn(0f, 0.85f)
        if (overlayAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = overlayAlpha))
            )
        }
    }
}

@Composable
fun HomeIconItem(
    project: IconProject?,
    cachedBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    sizeDp: Int = 52,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .width(sizeDp.dp + 12.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(sizeDp.dp * 0.22f)),
            color = Color.Black,
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(sizeDp.dp * 0.22f)
        ) {
            if (cachedBitmap != null) {
                Image(
                    bitmap = cachedBitmap.asImageBitmap(),
                    contentDescription = project?.name ?: "App Icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size((sizeDp * 0.5f).dp)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = project?.name ?: "App",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
