package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActionRequired
import com.example.model.InspectionReport
import com.example.model.InspectionStatus
import com.example.model.RiskLevel
import com.example.ui.theme.FailCrimson
import com.example.ui.theme.FailCrimsonSurface
import com.example.ui.theme.NaSlate
import com.example.ui.theme.NaSlateSurface
import com.example.ui.theme.PassEmerald
import com.example.ui.theme.PassEmeraldSurface
import com.example.ui.theme.RiskHighColor
import com.example.ui.theme.RiskHighSurface
import com.example.ui.theme.RiskLowColor
import com.example.ui.theme.RiskLowSurface
import com.example.ui.theme.RiskMediumColor
import com.example.ui.theme.RiskMediumSurface

@Composable
fun InspectionProgressBanner(
    report: InspectionReport,
    onReviewRiskClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = report.items.size.coerceAtLeast(1)
    val progress = report.answeredCount.toFloat() / total.toFloat()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatBadge(
                        label = "${report.answeredCount}/${report.items.size}",
                        bgColor = MaterialTheme.colorScheme.primaryContainer,
                        fgColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    StatBadge(
                        label = "Pass ${report.passCount}",
                        bgColor = PassEmeraldSurface,
                        fgColor = PassEmerald,
                    )
                    StatBadge(
                        label = "Fail ${report.failCount}",
                        bgColor = FailCrimsonSurface,
                        fgColor = FailCrimson,
                    )
                    if (report.naCount > 0) {
                        StatBadge(
                            label = "N/A ${report.naCount}",
                            bgColor = NaSlateSurface,
                            fgColor = NaSlate,
                        )
                    }
                }

                Surface(
                    onClick = onReviewRiskClick,
                    shape = RoundedCornerShape(8.dp),
                    color = if (report.failCount > 0) FailCrimson else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("banner_review_risk_button"),
                ) {
                    Text(
                        text = if (report.failCount > 0) "Risk (${report.failCount})" else "Export",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (report.failCount > 0) FailCrimson else PassEmerald,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun StatBadge(
    label: String,
    bgColor: Color,
    fgColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fgColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun StatusTriToggleRow(
    itemId: String,
    currentStatus: InspectionStatus,
    onSelectStatus: (InspectionStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusToggleButton(
            label = "Yes",
            selected = currentStatus == InspectionStatus.PASS,
            selectedBg = PassEmerald,
            selectedFg = Color.White,
            unselectedBorder = MaterialTheme.colorScheme.outline,
            icon = if (currentStatus == InspectionStatus.PASS) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
            onClick = { onSelectStatus(InspectionStatus.PASS) },
            modifier = Modifier
                .weight(1f)
                .testTag("status_pass_$itemId"),
        )
        StatusToggleButton(
            label = "No",
            selected = currentStatus == InspectionStatus.FAIL,
            selectedBg = FailCrimson,
            selectedFg = Color.White,
            unselectedBorder = MaterialTheme.colorScheme.outline,
            icon = if (currentStatus == InspectionStatus.FAIL) Icons.Filled.ReportProblem else Icons.Outlined.ErrorOutline,
            onClick = { onSelectStatus(InspectionStatus.FAIL) },
            modifier = Modifier
                .weight(1f)
                .testTag("status_fail_$itemId"),
        )
        StatusToggleButton(
            label = "N/A",
            selected = currentStatus == InspectionStatus.NA,
            selectedBg = NaSlate,
            selectedFg = Color.White,
            unselectedBorder = MaterialTheme.colorScheme.outline,
            icon = Icons.Outlined.Block,
            onClick = { onSelectStatus(InspectionStatus.NA) },
            modifier = Modifier
                .weight(0.9f)
                .testTag("status_na_$itemId"),
        )
    }
}

@Composable
private fun StatusToggleButton(
    label: String,
    selected: Boolean,
    selectedBg: Color,
    selectedFg: Color,
    unselectedBorder: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) selectedBg else MaterialTheme.colorScheme.surface,
        label = "status_bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedFg else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "status_fg",
    )
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) selectedBg else unselectedBorder,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

@Composable
fun RiskLevelSelector(
    itemId: String,
    selectedRisk: RiskLevel?,
    onSelectRisk: (RiskLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH).forEach { level ->
            val isSelected = selectedRisk == level
            val (activeBg, activeFg) = when (level) {
                RiskLevel.LOW -> RiskLowColor to Color.White
                RiskLevel.MEDIUM -> RiskMediumColor to Color.White
                RiskLevel.HIGH -> RiskHighColor to Color.White
            }
            val subtleBorder = when (level) {
                RiskLevel.LOW -> RiskLowColor.copy(alpha = 0.5f)
                RiskLevel.MEDIUM -> RiskMediumColor.copy(alpha = 0.5f)
                RiskLevel.HIGH -> RiskHighColor.copy(alpha = 0.5f)
            }
            Surface(
                onClick = { onSelectRisk(level) },
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("risk_${level.name.lowercase()}_$itemId"),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) activeBg else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) activeBg else subtleBorder,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = level.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) activeFg else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionRequiredSelector(
    itemId: String,
    selectedAction: ActionRequired?,
    onSelectAction: (ActionRequired) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionRequired.entries.forEach { action ->
            val isSelected = selectedAction == action
            val icon = when (action) {
                ActionRequired.MONITOR -> Icons.Filled.Visibility
                ActionRequired.REPAIR -> Icons.Filled.Build
                ActionRequired.TAKE_OUT_OF_USE -> Icons.Filled.DoNotDisturbOn
            }
            val activeBg = when (action) {
                ActionRequired.MONITOR -> RiskLowColor
                ActionRequired.REPAIR -> RiskMediumColor
                ActionRequired.TAKE_OUT_OF_USE -> FailCrimson
            }
            Surface(
                onClick = { onSelectAction(action) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("action_${action.name.lowercase()}_$itemId"),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) activeBg else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) activeBg else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
fun RiskBadge(
    riskLevel: RiskLevel?,
    modifier: Modifier = Modifier,
) {
    val (label, bg, fg) = when (riskLevel) {
        RiskLevel.HIGH -> Triple("HIGH RISK", RiskHighSurface, RiskHighColor)
        RiskLevel.MEDIUM -> Triple("MEDIUM RISK", RiskMediumSurface, RiskMediumColor)
        RiskLevel.LOW -> Triple("LOW RISK", RiskLowSurface, RiskLowColor)
        null -> Triple("ALL CLEAR", PassEmeraldSurface, PassEmerald)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bg,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
