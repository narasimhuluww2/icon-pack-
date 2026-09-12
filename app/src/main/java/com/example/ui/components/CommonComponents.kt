package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun NoirCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    border: BorderStroke = BorderStroke(1.dp, NoirBorder),
    backgroundColor: Color = NoirCard,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        color = backgroundColor,
        border = border,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun NoirButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    testTag: String = "action_button"
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .heightIn(min = 48.dp)
                .testTag(testTag),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = InkPureWhite,
                contentColor = InkPureBlack,
                disabledContainerColor = NoirBorder,
                disabledContentColor = InkMediumGray
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .heightIn(min = 48.dp)
                .testTag(testTag),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (enabled) NoirBorderLight else NoirBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = InkPureWhite,
                disabledContentColor = InkMediumGray
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun SliderControl(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    unit: String = "",
    defaultValue: Float? = null,
    onReset: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = InkLightGray
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NoirSurfaceElevated,
                    border = BorderStroke(0.5.dp, NoirBorder)
                ) {
                    Text(
                        text = "${value.toInt()}$unit",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = InkPureWhite
                        )
                    )
                }
                if (onReset != null && defaultValue != null && value != defaultValue) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset",
                        modifier = Modifier
                            .clickable { onReset() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = InkMediumGray
                        )
                    )
                }
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            colors = SliderDefaults.colors(
                thumbColor = InkPureWhite,
                activeTrackColor = InkPureWhite,
                inactiveTrackColor = NoirBorderLight
            )
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = InkPureWhite
                )
            )
            if (actionText != null && onActionClick != null) {
                Text(
                    text = actionText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onActionClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = InkLightGray,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = InkMediumGray
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
