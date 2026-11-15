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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.InspectionReportEntity
import com.example.model.RiskLevel
import com.example.ui.components.RiskBadge
import com.example.ui.components.StatBadge
import com.example.ui.theme.FailCrimson
import com.example.ui.theme.FailCrimsonSurface
import com.example.ui.theme.NaSlate
import com.example.ui.theme.NaSlateSurface
import com.example.ui.theme.PassEmerald
import com.example.ui.theme.PassEmeraldSurface

@Composable
fun HistoryScreen(
    savedReports: List<InspectionReportEntity>,
    onResumeReport: (String) -> Unit,
    onExportPdf: (InspectionReportEntity) -> Unit,
    onExportJson: (InspectionReportEntity) -> Unit,
    onDeleteReport: (String) -> Unit,
    onStartInspectionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<InspectionReportEntity?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (savedReports.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 500.dp)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Saved Inspections Yet",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Saved drafts and exported playground audits are stored locally on your device for offline access and re-exporting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onStartInspectionClick,
                    modifier = Modifier.testTag("history_empty_start_button"),
                ) {
                    Text("Go to Active Inspection")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "history_header") {
                    Text(
                        text = "Offline Inspection Archive (${savedReports.size})",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }

                items(
                    items = savedReports,
                    key = { it.id },
                ) { entity ->
                    SavedReportCard(
                        entity = entity,
                        onResume = { onResumeReport(entity.id) },
                        onExportPdf = { onExportPdf(entity) },
                        onExportJson = { onExportJson(entity) },
                        onDeleteClick = { pendingDelete = entity },
                    )
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Saved Inspection?") },
            text = {
                Text("Remove the saved audit for \"${target.siteName}\" (${target.inspectionDate}) from local storage?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteReport(target.id)
                        pendingDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SavedReportCard(
    entity: InspectionReportEntity,
    onResume: () -> Unit,
    onExportPdf: () -> Unit,
    onExportJson: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val riskEnum = when (entity.highestRisk) {
        RiskLevel.HIGH.label -> RiskLevel.HIGH
        RiskLevel.MEDIUM.label -> RiskLevel.MEDIUM
        RiskLevel.LOW.label -> RiskLevel.LOW
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_report_card_${entity.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entity.siteName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${entity.inspectionDate} ${entity.inspectionTime} • ${entity.inspectorName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Weather: ${entity.weatherConditions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RiskBadge(riskLevel = riskEnum)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatBadge(
                    label = "${entity.passCount} Pass",
                    bgColor = PassEmeraldSurface,
                    fgColor = PassEmerald,
                )
                StatBadge(
                    label = "${entity.failCount} Fail",
                    bgColor = FailCrimsonSurface,
                    fgColor = FailCrimson,
                )
                StatBadge(
                    label = "${entity.naCount} N/A",
                    bgColor = NaSlateSurface,
                    fgColor = NaSlate,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onExportJson,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("JSON", style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete saved report",
                        tint = FailCrimson,
                    )
                }
            }
        }
    }
}
