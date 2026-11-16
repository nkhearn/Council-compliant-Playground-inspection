package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ActionRequired
import com.example.model.InspectionReport
import com.example.model.RiskAssessmentEntry
import com.example.model.RiskLevel
import com.example.ui.components.ActionRequiredSelector
import com.example.ui.components.FivePointScaleSelector
import com.example.ui.components.RiskBadge
import com.example.ui.components.RiskScoreBanner
import com.example.ui.components.StatBadge
import com.example.ui.components.scaleColorsForValue
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
fun RiskExportScreen(
    report: InspectionReport,
    isExporting: Boolean,
    onSharePdfClick: () -> Unit,
    onExportJsonClick: () -> Unit,
    onSaveOfflineClick: () -> Unit,
    onNewInspectionClick: () -> Unit,
    onFixInChecklistClick: () -> Unit,
    onUpdateDefectDetails: (String, String) -> Unit,
    onUpdateProbability: (String, Int) -> Unit,
    onUpdateSeverity: (String, Int) -> Unit,
    onUpdateActionRequired: (String, ActionRequired) -> Unit,
    modifier: Modifier = Modifier,
) {
    val riskEntries = remember(report) { report.riskAssessmentEntries }
    var editingEntry by remember { mutableStateOf<RiskAssessmentEntry?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Summary Overview Card
            item(key = "risk_summary_overview") {
                RiskOverviewCard(report = report)
            }

            // 2. Incomplete Failure Warning (if any FAIL item lacks required fields)
            if (report.incompleteFailureCount > 0) {
                item(key = "incomplete_warning") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = RiskMediumSurface,
                        border = BorderStroke(1.dp, RiskMediumColor),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = RiskMediumColor,
                                )
                                Text(
                                    text = "${report.incompleteFailureCount} failed check(s) still missing defect details, probability/severity (1–5), or action.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF78350F),
                                )
                            }
                            TextButton(onClick = onFixInChecklistClick) {
                                Text("Review")
                            }
                        }
                    }
                }
            }

            // 3. Export & Share Section
            item(key = "export_actions") {
                ExportActionsCard(
                    isExporting = isExporting,
                    onSharePdfClick = onSharePdfClick,
                    onExportJsonClick = onExportJsonClick,
                    onSaveOfflineClick = onSaveOfflineClick,
                    onNewInspectionClick = onNewInspectionClick,
                )
            }

            // 4. Compiled Risk Assessment & Action Plan Section Header
            item(key = "risk_plan_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Risk Assessment & Action Plan",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    StatBadge(
                        label = "${riskEntries.size} Issue${if (riskEntries.size == 1) "" else "s"}",
                        bgColor = if (riskEntries.isEmpty()) PassEmeraldSurface else FailCrimsonSurface,
                        fgColor = if (riskEntries.isEmpty()) PassEmerald else FailCrimson,
                    )
                }
            }

            if (riskEntries.isEmpty()) {
                item(key = "no_defects") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = PassEmeraldSurface.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, PassEmerald.copy(alpha = 0.4f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = PassEmerald,
                                modifier = Modifier.size(40.dp),
                            )
                            Text(
                                text = "Zero Failed Checks Recorded",
                                style = MaterialTheme.typography.titleMedium,
                                color = PassEmerald,
                            )
                            Text(
                                text = "Any checklist item marked Fail (No) automatically compiles here with its Probability (1–5) × Severity (1–5) = Risk Score (1–25) and required action.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(
                    items = riskEntries,
                    key = { it.itemId },
                ) { entry ->
                    val rawItem = report.items.firstOrNull { it.id == entry.itemId }
                    RiskAssessmentRowCard(
                        entry = entry,
                        isComplete = rawItem?.isFailureComplete ?: true,
                        onEditClick = { editingEntry = entry },
                    )
                }
            }
        }
    }

    editingEntry?.let { entry ->
        val rawItem = report.items.firstOrNull { it.id == entry.itemId }
        EditRiskEntryDialog(
            entry = entry,
            initialDetails = rawItem?.defectDetails ?: "",
            initialProbability = rawItem?.probability ?: entry.probability,
            initialSeverity = rawItem?.severity ?: entry.severity,
            initialAction = rawItem?.actionRequired ?: entry.actionRequired,
            onDismiss = { editingEntry = null },
            onSave = { details, prob, sev, action ->
                onUpdateDefectDetails(entry.itemId, details)
                onUpdateProbability(entry.itemId, prob)
                onUpdateSeverity(entry.itemId, sev)
                if (action != null) onUpdateActionRequired(entry.itemId, action)
                editingEntry = null
            },
        )
    }
}

@Composable
private fun RiskOverviewCard(report: InspectionReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.metadata.siteName.ifBlank { "Unnamed Play Area" },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "${report.metadata.inspectionDate} • ${report.metadata.inspectionTime} • ${report.metadata.weatherConditions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Inspector: ${report.metadata.inspectorName.ifBlank { "Not specified" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RiskBadge(
                    riskLevel = report.highestRiskLevel,
                    riskScore = report.highestRiskScore,
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricTile(
                    title = "Pass (Yes)",
                    value = report.passCount.toString(),
                    bgColor = PassEmeraldSurface,
                    fgColor = PassEmerald,
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    title = "Fail (No)",
                    value = report.failCount.toString(),
                    bgColor = FailCrimsonSurface,
                    fgColor = FailCrimson,
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    title = "N/A",
                    value = report.naCount.toString(),
                    bgColor = NaSlateSurface,
                    fgColor = NaSlate,
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    title = "Pending",
                    value = report.unansweredCount.toString(),
                    bgColor = MaterialTheme.colorScheme.surfaceVariant,
                    fgColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }

            if (report.failCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Risk (1–25):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    StatBadge("${report.highRiskCount} High (15+)", RiskHighSurface, RiskHighColor)
                    StatBadge("${report.mediumRiskCount} Med (7–14)", RiskMediumSurface, RiskMediumColor)
                    StatBadge("${report.lowRiskCount} Low (1–6)", RiskLowSurface, RiskLowColor)
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    bgColor: Color,
    fgColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = fgColor,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = fgColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ExportActionsCard(
    isExporting: Boolean,
    onSharePdfClick: () -> Unit,
    onExportJsonClick: () -> Unit,
    onSaveOfflineClick: () -> Unit,
    onNewInspectionClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Generate & Share Report",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Generates a compact A4 PDF (metadata, checklist, and 1–25 Risk Assessment table) or structured JSON in local cache for instant email/share.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = onSharePdfClick,
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("share_pdf_button"),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating Report...")
                } else {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share / Email PDF Report")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onExportJsonClick,
                    enabled = !isExporting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_json_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export JSON")
                }

                OutlinedButton(
                    onClick = onSaveOfflineClick,
                    enabled = !isExporting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_offline_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Offline")
                }
            }

            TextButton(
                onClick = onNewInspectionClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("start_new_inspection_button"),
            ) {
                Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Fresh Inspection")
            }
        }
    }
}

@Composable
private fun RiskAssessmentRowCard(
    entry: RiskAssessmentEntry,
    isComplete: Boolean,
    onEditClick: () -> Unit,
) {
    val accentColor = when (entry.riskLevel) {
        RiskLevel.HIGH -> RiskHighColor
        RiskLevel.MEDIUM -> RiskMediumColor
        RiskLevel.LOW -> RiskLowColor
    }
    val (pFg, pBg) = scaleColorsForValue(entry.probability)
    val (sFg, sBg) = scaleColorsForValue(entry.severity)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("risk_entry_card_${entry.itemId}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                    modifier = Modifier.weight(1f),
                ) {
                    StatBadge(
                        label = entry.referenceCode,
                        bgColor = MaterialTheme.colorScheme.primaryContainer,
                        fgColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = entry.categoryTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.testTag("edit_risk_entry_${entry.itemId}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit defect assessment",
                    )
                }
            }

            Text(
                text = entry.question,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "DEFECT DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = entry.defectDetails,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isComplete) MaterialTheme.colorScheme.onSurface else FailCrimson,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatBadge(label = "P: ${entry.probability}/5", bgColor = pBg, fgColor = pFg)
                Text("×", style = MaterialTheme.typography.labelMedium)
                StatBadge(label = "S: ${entry.severity}/5", bgColor = sBg, fgColor = sFg)
                Text("=", style = MaterialTheme.typography.labelMedium)
                RiskBadge(
                    riskLevel = entry.riskLevel,
                    riskScore = entry.riskScore,
                )
            }

            StatBadge(
                label = "Action: ${entry.actionRequired.label}",
                bgColor = if (entry.actionRequired == ActionRequired.TAKE_OUT_OF_USE) {
                    FailCrimsonSurface
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                fgColor = if (entry.actionRequired == ActionRequired.TAKE_OUT_OF_USE) {
                    FailCrimson
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
        }
    }
}

@Composable
private fun EditRiskEntryDialog(
    entry: RiskAssessmentEntry,
    initialDetails: String,
    initialProbability: Int,
    initialSeverity: Int,
    initialAction: ActionRequired?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, ActionRequired?) -> Unit,
) {
    var details by remember { mutableStateOf(initialDetails) }
    var probability by remember { mutableStateOf(initialProbability.coerceIn(1, 5)) }
    var severity by remember { mutableStateOf(initialSeverity.coerceIn(1, 5)) }
    var action by remember { mutableStateOf(initialAction ?: entry.actionRequired) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Risk Entry (${entry.referenceCode})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = entry.question,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Defect Details") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Probability (1–5)", style = MaterialTheme.typography.titleSmall)
                FivePointScaleSelector(
                    tagPrefix = "dialog_prob_${entry.itemId}",
                    selectedValue = probability,
                    lowHint = "1 · Low",
                    highHint = "5 · High",
                    onSelectValue = { probability = it },
                )
                Text("Severity (1–5)", style = MaterialTheme.typography.titleSmall)
                FivePointScaleSelector(
                    tagPrefix = "dialog_sev_${entry.itemId}",
                    selectedValue = severity,
                    lowHint = "1 · Least",
                    highHint = "5 · Severe",
                    onSelectValue = { severity = it },
                )
                RiskScoreBanner(probability = probability, severity = severity)
                Text("Action Required", style = MaterialTheme.typography.titleSmall)
                ActionRequiredSelector(
                    itemId = "dialog_${entry.itemId}",
                    selectedAction = action,
                    onSelectAction = { action = it },
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(details, probability, severity, action) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
