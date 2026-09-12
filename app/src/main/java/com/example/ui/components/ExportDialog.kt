package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.IconEditorViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    viewModel: IconEditorViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedResolution by remember { mutableStateOf(1024) }
    var transparentBg by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }

    val resolutions = listOf(
        Pair(1024, "1024 x 1024 (HD Master)"),
        Pair(512, "512 x 512 (Standard)"),
        Pair(256, "256 x 256 (App Icon)"),
        Pair(180, "180 x 180 (iOS Retina)"),
        Pair(120, "120 x 120 (Compact)")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NoirSurface,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EXPORT ICON",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "High-quality monochrome PNG export",
                        style = MaterialTheme.typography.bodyMedium.copy(color = InkMediumGray)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = InkLightGray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SELECT RESOLUTION",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = InkMediumGray,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            resolutions.forEach { (res, label) ->
                val isSelected = selectedResolution == res
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) NoirSurfaceElevated else NoirCard,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) InkPureWhite else NoirBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedResolution = res }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) InkPureWhite else InkLightGray
                            )
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedResolution = res },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = InkPureWhite,
                                unselectedColor = InkMediumGray
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transparent background option
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NoirCard,
                border = BorderStroke(1.dp, NoirBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { transparentBg = !transparentBg }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Transparent Background",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = InkPureWhite
                            )
                        )
                        Text(
                            text = "Mask with transparent margins outside the icon shape",
                            style = MaterialTheme.typography.bodySmall.copy(color = InkMediumGray)
                        )
                    }
                    Switch(
                        checked = transparentBg,
                        onCheckedChange = { transparentBg = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = InkPureBlack,
                            checkedTrackColor = InkPureWhite,
                            uncheckedThumbColor = InkLightGray,
                            uncheckedTrackColor = NoirSurfaceElevated
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isExporting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = InkPureWhite,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                }
            } else if (exportedFile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NoirButton(
                        text = "Share PNG",
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null, tint = InkPureBlack)
                        },
                        onClick = {
                            viewModel.shareExportedFile(context, exportedFile!!)
                        }
                    )
                    NoirButton(
                        text = "Done",
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )
                }
            } else {
                NoirButton(
                    text = "Export PNG (${selectedResolution}x${selectedResolution})",
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Download, contentDescription = null, tint = InkPureBlack)
                    },
                    onClick = {
                        isExporting = true
                        viewModel.exportPng(context, selectedResolution, transparentBg) { file ->
                            isExporting = false
                            if (file != null) {
                                exportedFile = file
                                Toast.makeText(context, "Rendered ${file.name} successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Export failed. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
