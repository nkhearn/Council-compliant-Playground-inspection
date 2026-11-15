package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ChecklistCatalog
import com.example.model.ActionRequired
import com.example.model.InspectionItem
import com.example.model.InspectionReport
import com.example.model.InspectionStatus
import com.example.model.MaintenanceLogEntry
import com.example.model.OverallActionRequired
import com.example.model.RiskLevel
import com.example.ui.components.ActionRequiredSelector
import com.example.ui.components.InspectionProgressBanner
import com.example.ui.components.RiskLevelSelector
import com.example.ui.components.StatBadge
import com.example.ui.components.StatusTriToggleRow
import com.example.ui.theme.FailCrimson
import com.example.ui.theme.FailCrimsonSurface
import com.example.ui.theme.NaSlate
import com.example.ui.theme.NaSlateSurface
import com.example.ui.theme.PassEmerald
import com.example.ui.theme.PassEmeraldSurface
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SafetyAmberContainer

@Composable
fun InspectScreen(
    report: InspectionReport,
    selectedCategoryFilter: String?,
    onSelectCategoryFilter: (String?) -> Unit,
    onInspectorNameChange: (String) -> Unit,
    onSiteNameChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onWeatherChange: (String) -> Unit,
    onItemStatusChange: (String, InspectionStatus) -> Unit,
    onItemNoteChange: (String, String) -> Unit,
    onItemDefectDetailsChange: (String, String) -> Unit,
    onItemRiskLevelChange: (String, RiskLevel) -> Unit,
    onItemActionRequiredChange: (String, ActionRequired) -> Unit,
    onMarkCategoryAllPass: (String) -> Unit,
    onToggleCategoryNa: (String) -> Unit,
    onAddMaintenanceLog: (MaintenanceLogEntry) -> Unit,
    onRemoveMaintenanceLog: (String) -> Unit,
    onOverallCommentsChange: (String) -> Unit,
    onOverallActionChange: (OverallActionRequired) -> Unit,
    onSignatureChange: (String) -> Unit,
    onSaveReport: () -> Unit,
    onNavigateToRiskExport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    val expandedNaSections = remember { mutableStateMapOf<String, Boolean>() }

    val groupedItems = remember(report.items, selectedCategoryFilter) {
        val filtered = if (selectedCategoryFilter == null) {
            report.items
        } else {
            report.items.filter { it.categoryId == selectedCategoryFilter }
        }
        filtered.groupBy { it.categoryId }
    }

    val categoryCounts = remember(report.items) {
        report.items.groupBy { it.categoryId }
    }

    Column(modifier = modifier.fillMaxSize()) {
        InspectionProgressBanner(
            report = report,
            onReviewRiskClick = onNavigateToRiskExport,
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 1. Metadata Header Card
                item(key = "metadata_header") {
                    InspectionMetadataCard(
                        report = report,
                        onInspectorNameChange = onInspectorNameChange,
                        onSiteNameChange = onSiteNameChange,
                        onDateChange = onDateChange,
                        onTimeChange = onTimeChange,
                        onWeatherChange = onWeatherChange,
                    )
                }

                // 2. Category Quick Filter Strip
                if (categoryCounts.isNotEmpty()) {
                    item(key = "category_filter_strip") {
                        CategoryFilterStrip(
                            report = report,
                            categoryCounts = categoryCounts,
                            selectedCategoryFilter = selectedCategoryFilter,
                            onSelectCategoryFilter = onSelectCategoryFilter,
                        )
                    }
                }

                // Empty State if all equipment sections were disabled in Settings
                if (report.items.isEmpty()) {
                    item(key = "empty_checklist") {
                        EmptyChecklistCard(onOpenSettings = onNavigateToSettings)
                    }
                } else {
                    // 3. Dynamic Equipment Sections & Conditional Failure Items
                    groupedItems.forEach { (categoryId, sectionItems) ->
                        val first = sectionItems.first()
                        val sectionPassed = sectionItems.count { it.status == InspectionStatus.PASS }
                        val sectionFailed = sectionItems.count { it.status == InspectionStatus.FAIL }
                        val sectionNa = sectionItems.count { it.status == InspectionStatus.NA }
                        val sectionAnswered = sectionItems.count { it.status != InspectionStatus.UNANSWERED }
                        val isSectionAllNa = sectionItems.isNotEmpty() && sectionNa == sectionItems.size
                        val isNaSectionExpanded = expandedNaSections[categoryId] == true

                        item(key = "header_$categoryId") {
                            SectionBannerHeader(
                                categoryId = categoryId,
                                sectionNumber = first.sectionNumber,
                                title = first.categoryTitle,
                                answered = sectionAnswered,
                                total = sectionItems.size,
                                failed = sectionFailed,
                                passed = sectionPassed,
                                isSectionAllNa = isSectionAllNa,
                                isNaSectionExpanded = isNaSectionExpanded,
                                hasUnanswered = sectionAnswered < sectionItems.size,
                                onToggleSectionNa = { onToggleCategoryNa(categoryId) },
                                onToggleExpandNaSection = {
                                    expandedNaSections[categoryId] = !isNaSectionExpanded
                                },
                                onMarkRemainingPass = { onMarkCategoryAllPass(categoryId) },
                            )
                        }

                        if (!isSectionAllNa || isNaSectionExpanded) {
                            items(
                                items = sectionItems,
                                key = { it.id },
                            ) { item ->
                                InspectionItemCard(
                                    item = item,
                                    onSelectStatus = { status -> onItemStatusChange(item.id, status) },
                                    onNoteChange = { note -> onItemNoteChange(item.id, note) },
                                    onDefectDetailsChange = { details -> onItemDefectDetailsChange(item.id, details) },
                                    onRiskLevelChange = { risk -> onItemRiskLevelChange(item.id, risk) },
                                    onActionRequiredChange = { action -> onItemActionRequiredChange(item.id, action) },
                                )
                            }
                        }
                    }

                    // 4. Maintenance Log & Inspector Sign-Off
                    item(key = "maintenance_signoff") {
                        MaintenanceAndSignOffCard(
                            report = report,
                            onAddLogClick = { showMaintenanceDialog = true },
                            onRemoveLog = onRemoveMaintenanceLog,
                            onOverallCommentsChange = onOverallCommentsChange,
                            onOverallActionChange = onOverallActionChange,
                            onSignatureChange = onSignatureChange,
                        )
                    }

                    // 5. Bottom Action Bar
                    item(key = "bottom_actions") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = onSaveReport,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("save_draft_button"),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Draft")
                            }

                            Button(
                                onClick = onNavigateToRiskExport,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(50.dp)
                                    .testTag("compile_risk_export_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Text("Risk & Export")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMaintenanceDialog) {
        AddMaintenanceLogDialog(
            defaultDate = report.metadata.inspectionDate,
            defaultInspector = report.metadata.inspectorName,
            onDismiss = { showMaintenanceDialog = false },
            onAdd = { entry ->
                onAddMaintenanceLog(entry)
                showMaintenanceDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InspectionMetadataCard(
    report: InspectionReport,
    onInspectorNameChange: (String) -> Unit,
    onSiteNameChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onWeatherChange: (String) -> Unit,
) {
    val meta = report.metadata
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Navy Banner
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = meta.formRef,
                        style = MaterialTheme.typography.labelSmall,
                        color = SafetyAmberContainer,
                    )
                    Text(
                        text = "Routine Playground Inspection",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Text(
                        text = meta.organizationName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = meta.siteName,
                    onValueChange = onSiteNameChange,
                    label = { Text("Site / Playground Name") },
                    placeholder = { Text("e.g., Kirkwall East Play Area") },
                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("site_name_input"),
                )

                OutlinedTextField(
                    value = meta.inspectorName,
                    onValueChange = onInspectorNameChange,
                    label = { Text("Inspector Name") },
                    placeholder = { Text("Full name of inspecting officer") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inspector_name_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = meta.inspectionDate,
                        onValueChange = onDateChange,
                        label = { Text("Date") },
                        leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("inspection_date_input"),
                    )
                    OutlinedTextField(
                        value = meta.inspectionTime,
                        onValueChange = onTimeChange,
                        label = { Text("Time") },
                        leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inspection_time_input"),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WbSunny,
                            contentDescription = null,
                            tint = SafetyAmber,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Weather Conditions",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ChecklistCatalog.weatherPresets.forEach { preset ->
                            val selected = meta.weatherConditions.equals(preset, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { onWeatherChange(preset) },
                                label = { Text(preset, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.testTag("weather_chip_${preset.lowercase().replace(" ", "_")}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                    OutlinedTextField(
                        value = meta.weatherConditions,
                        onValueChange = onWeatherChange,
                        label = { Text("Weather Notes / Custom") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weather_input"),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterStrip(
    report: InspectionReport,
    categoryCounts: Map<String, List<InspectionItem>>,
    selectedCategoryFilter: String?,
    onSelectCategoryFilter: (String?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedCategoryFilter == null,
                onClick = { onSelectCategoryFilter(null) },
                label = {
                    Text("All (${report.items.size})")
                },
                modifier = Modifier.testTag("filter_all_categories"),
            )
        }
        ChecklistCatalog.categories.forEach { cat ->
            val itemsInCat = categoryCounts[cat.id] ?: return@forEach
            val failCount = itemsInCat.count { it.status == InspectionStatus.FAIL }
            val shortName = "${cat.sectionNumber}. ${cat.title.substringBefore(" (")}"
            item(key = cat.id) {
                FilterChip(
                    selected = selectedCategoryFilter == cat.id,
                    onClick = {
                        onSelectCategoryFilter(if (selectedCategoryFilter == cat.id) null else cat.id)
                    },
                    label = {
                        Text(
                            text = if (failCount > 0) "$shortName ($failCount!)" else shortName,
                            maxLines = 1,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (failCount > 0) FailCrimsonSurface else MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = if (failCount > 0) FailCrimson else MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.testTag("filter_cat_${cat.id}"),
                )
            }
        }
    }
}

@Composable
private fun SectionBannerHeader(
    categoryId: String,
    sectionNumber: Int,
    title: String,
    answered: Int,
    total: Int,
    failed: Int,
    passed: Int,
    isSectionAllNa: Boolean,
    isNaSectionExpanded: Boolean,
    hasUnanswered: Boolean,
    onToggleSectionNa: () -> Unit,
    onToggleExpandNaSection: () -> Unit,
    onMarkRemainingPass: () -> Unit,
) {
    val containerColor = if (isSectionAllNa) {
        NaSlateSurface.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    }
    val borderColor = if (isSectionAllNa) {
        NaSlate.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "$sectionNumber. $title",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSectionAllNa) NaSlate else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isSectionAllNa) {
                            StatBadge(
                                label = "Section N/A ($total)",
                                bgColor = NaSlate,
                                fgColor = Color.White,
                            )
                        } else {
                            StatBadge(
                                label = "$answered/$total checked",
                                bgColor = MaterialTheme.colorScheme.surface,
                                fgColor = MaterialTheme.colorScheme.onSurface,
                            )
                            if (passed > 0) {
                                StatBadge(
                                    label = "$passed Pass",
                                    bgColor = PassEmeraldSurface,
                                    fgColor = PassEmerald,
                                )
                            }
                            if (failed > 0) {
                                StatBadge(
                                    label = "$failed Fail",
                                    bgColor = FailCrimsonSurface,
                                    fgColor = FailCrimson,
                                )
                            }
                        }
                    }
                }

                // Individual Heading N/A Toggle
                Surface(
                    onClick = onToggleSectionNa,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("section_na_$categoryId"),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSectionAllNa) NaSlate else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isSectionAllNa) NaSlate else MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Block,
                            contentDescription = "Mark section N/A",
                            tint = if (isSectionAllNa) Color.White else NaSlate,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = if (isSectionAllNa) "N/A ✓" else "N/A",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSectionAllNa) Color.White else NaSlate,
                        )
                    }
                }
            }

            if (isSectionAllNa || hasUnanswered) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isSectionAllNa) {
                        TextButton(
                            onClick = onToggleExpandNaSection,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("toggle_expand_na_$categoryId"),
                        ) {
                            Text(
                                text = if (isNaSectionExpanded) "Hide N/A Checks" else "Show $total Checks",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    } else if (hasUnanswered) {
                        TextButton(
                            onClick = onMarkRemainingPass,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("pass_remaining_sec_$sectionNumber"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pass Remaining", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectionItemCard(
    item: InspectionItem,
    onSelectStatus: (InspectionStatus) -> Unit,
    onNoteChange: (String) -> Unit,
    onDefectDetailsChange: (String) -> Unit,
    onRiskLevelChange: (RiskLevel) -> Unit,
    onActionRequiredChange: (ActionRequired) -> Unit,
) {
    var showOptionalNote by remember(item.id) { mutableStateOf(item.inlineNote.isNotBlank()) }

    val borderColor = when (item.status) {
        InspectionStatus.FAIL -> FailCrimson
        InspectionStatus.PASS -> PassEmerald.copy(alpha = 0.55f)
        InspectionStatus.NA -> MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        InspectionStatus.UNANSWERED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("checklist_item_card_${item.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (item.status == InspectionStatus.FAIL) 1.5.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = item.referenceCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }

            StatusTriToggleRow(
                itemId = item.id,
                currentStatus = item.status,
                onSelectStatus = onSelectStatus,
            )

            // Conditional Failure Input Panel (Expands directly beneath when Fail/No is selected)
            AnimatedVisibility(
                visible = item.status == InspectionStatus.FAIL,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = FailCrimsonSurface.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, FailCrimson.copy(alpha = 0.45f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = FailCrimson,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Defect & Risk Assessment",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = FailCrimson,
                                )
                            }
                            StatBadge(
                                label = if (item.isFailureComplete) "Complete" else "Required",
                                bgColor = if (item.isFailureComplete) PassEmeraldSurface else Color.White,
                                fgColor = if (item.isFailureComplete) PassEmerald else FailCrimson,
                            )
                        }

                        OutlinedTextField(
                            value = item.defectDetails,
                            onValueChange = onDefectDetailsChange,
                            label = { Text("Defect Details *") },
                            placeholder = { Text("Describe the damage, wear, or hazard observed...") },
                            isError = item.defectDetails.isBlank(),
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("defect_details_input_${item.id}"),
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Risk Level *",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            RiskLevelSelector(
                                itemId = item.id,
                                selectedRisk = item.riskLevel,
                                onSelectRisk = onRiskLevelChange,
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Action Required *",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            ActionRequiredSelector(
                                itemId = item.id,
                                selectedAction = item.actionRequired,
                                onSelectAction = onActionRequiredChange,
                            )
                        }
                    }
                }
            }

            // Optional inline note for Pass / N/A / Unanswered
            if (item.status != InspectionStatus.FAIL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { showOptionalNote = !showOptionalNote },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("toggle_note_${item.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showOptionalNote) "Hide Note" else if (item.inlineNote.isBlank()) "Add Inline Note" else "Edit Note",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                AnimatedVisibility(visible = showOptionalNote) {
                    OutlinedTextField(
                        value = item.inlineNote,
                        onValueChange = onNoteChange,
                        label = { Text("Inspector Note (Optional)") },
                        placeholder = { Text("Observations, wear monitor note, or N/A reason...") },
                        minLines = 1,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inline_note_input_${item.id}"),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaintenanceAndSignOffCard(
    report: InspectionReport,
    onAddLogClick: () -> Unit,
    onRemoveLog: (String) -> Unit,
    onOverallCommentsChange: (String) -> Unit,
    onOverallActionChange: (OverallActionRequired) -> Unit,
    onSignatureChange: (String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "7. Maintenance Log & Sign-Off",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                TextButton(
                    onClick = onAddLogClick,
                    modifier = Modifier.testTag("add_maintenance_log_button"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Entry")
                }
            }

            if (report.maintenanceLogs.isNotEmpty()) {
                report.maintenanceLogs.forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.equipmentItem.ifBlank { "General Equipment" },
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = entry.repairCarriedOut,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (entry.partsUsed.isNotBlank() || entry.completedByDate.isNotBlank()) {
                                    Text(
                                        text = listOfNotNull(
                                            entry.partsUsed.takeIf { it.isNotBlank() }?.let { "Parts: $it" },
                                            entry.completedByDate.takeIf { it.isNotBlank() }?.let { "Completed: $it" },
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(onClick = { onRemoveLog(entry.id) }) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Remove maintenance entry",
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = report.metadata.overallComments,
                onValueChange = onOverallCommentsChange,
                label = { Text("Inspector Overall Comments") },
                placeholder = { Text("Summary of site condition or follow-up observations...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overall_comments_input"),
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Overall Action Required",
                    style = MaterialTheme.typography.titleSmall,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OverallActionRequired.entries.forEach { action ->
                        FilterChip(
                            selected = report.metadata.overallActionRequired == action,
                            onClick = { onOverallActionChange(action) },
                            label = { Text(action.label) },
                            leadingIcon = if (report.metadata.overallActionRequired == action) {
                                { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else {
                                null
                            },
                            modifier = Modifier.testTag("overall_action_${action.name.lowercase()}"),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = report.metadata.inspectorSignature,
                onValueChange = onSignatureChange,
                label = { Text("Inspector Sign-Off / Initials") },
                placeholder = { Text("Enter name or initials to sign off report") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inspector_signature_input"),
            )
        }
    }
}

@Composable
private fun EmptyChecklistCard(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            )
            Text(
                text = "No Equipment Sections Enabled",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Turn on at least one play equipment or site audit category in Settings to begin your inspection.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("empty_open_settings_button"),
            ) {
                Text("Configure Equipment in Settings")
            }
        }
    }
}

@Composable
private fun AddMaintenanceLogDialog(
    defaultDate: String,
    defaultInspector: String,
    onDismiss: () -> Unit,
    onAdd: (MaintenanceLogEntry) -> Unit,
) {
    var equipmentItem by remember { mutableStateOf("") }
    var repairCarriedOut by remember { mutableStateOf("") }
    var partsUsed by remember { mutableStateOf("") }
    var completedByDate by remember {
        mutableStateOf(
            listOfNotNull(
                defaultInspector.takeIf { it.isNotBlank() },
                defaultDate.takeIf { it.isNotBlank() },
            ).joinToString(" — "),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Maintenance Log Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = equipmentItem,
                    onValueChange = { equipmentItem = it },
                    label = { Text("Equipment Item") },
                    placeholder = { Text("e.g., Basket Swing Shackle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = repairCarriedOut,
                    onValueChange = { repairCarriedOut = it },
                    label = { Text("Specific Maintenance / Minor Repair") },
                    placeholder = { Text("e.g., Greased top crossbar bearings") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = partsUsed,
                    onValueChange = { partsUsed = it },
                    label = { Text("Parts Used") },
                    placeholder = { Text("e.g., Marine grease, M10 bolt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = completedByDate,
                    onValueChange = { completedByDate = it },
                    label = { Text("Completed By / Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        MaintenanceLogEntry(
                            equipmentItem = equipmentItem.trim(),
                            repairCarriedOut = repairCarriedOut.trim(),
                            partsUsed = partsUsed.trim(),
                            completedByDate = completedByDate.trim(),
                        ),
                    )
                },
                enabled = equipmentItem.isNotBlank() || repairCarriedOut.isNotBlank(),
            ) {
                Text("Add Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
