package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.image.ImageProcessor
import com.example.ui.components.NoirButton
import com.example.ui.components.NoirCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.IconEditorViewModel
import com.example.ui.viewmodel.ScreenState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: IconEditorViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsState()

    var selectedLibraryCategory by remember { mutableStateOf("All") }
    var selectedProjectForActions by remember { mutableStateOf<IconProject?>(null) }

    // Pre-render small thumbnails
    val thumbnailMap = remember { mutableStateMapOf<String, Bitmap>() }

    LaunchedEffect(projects) {
        projects.forEach { p ->
            if (!thumbnailMap.containsKey(p.id)) {
                coroutineScope.launch {
                    try {
                        val bmp = ImageProcessor.renderIconBitmap(context, p, targetSize = 160)
                        thumbnailMap[p.id] = bmp
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = InkPureBlack,
                            border = BorderStroke(1.dp, InkPureWhite)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(R.drawable.img_app_icon),
                                    contentDescription = "MonoIcon Studio",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "MONOICON STUDIO",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "MONOCHROME NOIR & COMIC ICONS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 0.5.sp,
                                    color = InkMediumGray
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenState.HOME_SCREEN_PREVIEW) },
                        modifier = Modifier.testTag("preview_launcher_button")
                    ) {
                        Icon(
                            Icons.Default.PhoneIphone,
                            contentDescription = "Home Screen Preview",
                            tint = InkPureWhite
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // HERO BANNER
            item {
                HeroBannerCard(
                    onCreateNew = { viewModel.startNewProject() },
                    onOpenPreview = { viewModel.navigateTo(ScreenState.HOME_SCREEN_PREVIEW) }
                )
            }

            // SECTION 1: MY ICONS
            item {
                SectionHeader(
                    title = "My Icons (${projects.size})",
                    subtitle = "Your custom crafted vintage icons",
                    actionText = "+ New",
                    onActionClick = { viewModel.startNewProject() }
                )
            }

            item {
                if (projects.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = NoirCard,
                        border = BorderStroke(1.dp, NoirBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = InkMediumGray, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No saved icons yet", style = MaterialTheme.typography.bodyMedium.copy(color = InkMediumGray))
                            Spacer(modifier = Modifier.height(12.dp))
                            NoirButton(
                                text = "Create Your First Icon",
                                isPrimary = true,
                                onClick = { viewModel.startNewProject() }
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(projects) { project ->
                            MyIconGridCard(
                                project = project,
                                bitmap = thumbnailMap[project.id],
                                onEdit = { viewModel.openProject(project) },
                                onActionClick = { selectedProjectForActions = project }
                            )
                        }
                    }
                }
            }

            // SECTION 2: VISUAL STYLES
            item {
                SectionHeader(
                    title = "Visual Styles & Presets",
                    subtitle = "One-click aesthetics inspired by comic panels & newsprint"
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PresetStyle.values().forEach { preset ->
                        PresetStyleCard(
                            preset = preset,
                            onClick = {
                                viewModel.startNewProject()
                                viewModel.applyPreset(preset)
                            }
                        )
                    }
                }
            }

            // SECTION 3: ICON TEMPLATE LIBRARY
            item {
                SectionHeader(
                    title = "Template Library",
                    subtitle = "Ready-made noir icons organized by category"
                )
            }

            // Category Filter Pills
            item {
                val categories = listOf("All", "Media", "Productivity", "System", "Entertainment", "Custom")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedLibraryCategory == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) InkPureWhite else NoirCard,
                            border = BorderStroke(1.dp, if (isSelected) InkPureWhite else NoirBorder),
                            modifier = Modifier.clickable { selectedLibraryCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) InkPureBlack else InkLightGray
                                )
                            )
                        }
                    }
                }
            }

            // Library Grid Items
            item {
                val filteredArtworks = BuiltInArtwork.values().filter {
                    selectedLibraryCategory == "All" || it.category == selectedLibraryCategory
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredArtworks.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { art ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.startNewProject(builtInArtwork = art) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = NoirCard,
                                    border = BorderStroke(1.dp, NoirBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(38.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = NoirBlack,
                                            border = BorderStroke(0.5.dp, InkMediumGray)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Brush,
                                                    contentDescription = null,
                                                    tint = InkPureWhite,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = art.displayName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = InkPureWhite
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = art.category,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = InkMediumGray
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Action BottomSheet for Project (Edit, Duplicate, Delete, Favorite)
    if (selectedProjectForActions != null) {
        val proj = selectedProjectForActions!!
        ModalBottomSheet(
            onDismissRequest = { selectedProjectForActions = null },
            containerColor = NoirSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = proj.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "${proj.filter.displayName} • ${proj.shape.displayName}",
                    style = MaterialTheme.typography.bodySmall.copy(color = InkMediumGray)
                )

                Spacer(modifier = Modifier.height(16.dp))

                ListItem(
                    headlineContent = { Text("Open in Editor", color = InkPureWhite) },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null, tint = InkPureWhite) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        viewModel.openProject(proj)
                        selectedProjectForActions = null
                    }
                )
                ListItem(
                    headlineContent = { Text("Duplicate Project", color = InkPureWhite) },
                    leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = InkPureWhite) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        viewModel.duplicateProject(proj)
                        selectedProjectForActions = null
                    }
                )
                ListItem(
                    headlineContent = { Text(if (proj.isFavorite) "Remove Favorite" else "Add to Favorites", color = InkPureWhite) },
                    leadingContent = {
                        Icon(
                            if (proj.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = InkPureWhite
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        viewModel.toggleFavorite(proj.id)
                        selectedProjectForActions = null
                    }
                )
                ListItem(
                    headlineContent = { Text("Delete Project", color = Color(0xFFFF5252)) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        viewModel.deleteProject(proj.id)
                        selectedProjectForActions = null
                    }
                )
            }
        }
    }
}

@Composable
fun HeroBannerCard(
    onCreateNew: () -> Unit,
    onOpenPreview: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = NoirCard,
        border = BorderStroke(1.dp, NoirBorderLight)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle newspaper texture background
            Image(
                painter = painterResource(R.drawable.img_tex_news),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.12f,
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = InkPureWhite
                ) {
                    Text(
                        text = "VINTAGE COMIC & NOIR ICON PACKS",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = InkPureBlack,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Craft Your Monochrome Aesthetic",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = InkPureWhite
                    )
                )

                Text(
                    text = "Convert photos or custom vectors into high-contrast ink drawings, halftone newsprint, and brooding vigilante app icons.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = InkMediumGray,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NoirButton(
                        text = "+ Create Icon",
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        onClick = onCreateNew,
                        testTag = "create_icon_hero_button"
                    )

                    NoirButton(
                        text = "Home Screen",
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        onClick = onOpenPreview,
                        testTag = "home_screen_preview_button"
                    )
                }
            }
        }
    }
}

@Composable
fun MyIconGridCard(
    project: IconProject,
    bitmap: Bitmap?,
    onEdit: () -> Unit,
    onActionClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NoirCard,
        border = BorderStroke(1.dp, NoirBorder),
        modifier = Modifier
            .width(135.dp)
            .clickable { onEdit() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon thumbnail
            Surface(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = NoirBlack,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NoirBorderLight)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = project.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = InkPureWhite,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = project.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = InkPureWhite
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.filter.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = InkMediumGray,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )

                IconButton(
                    onClick = onActionClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Actions",
                        tint = InkLightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PresetStyleCard(
    preset: PresetStyle,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NoirCard,
        border = BorderStroke(1.dp, NoirBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.displayName.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = InkPureWhite
                    )
                )
                Text(
                    text = preset.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = InkMediumGray),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = InkLightGray
            )
        }
    }
}
