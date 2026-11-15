package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.ChecklistCatalog
import com.example.data.local.InspectorSettings
import com.example.model.ChecklistQuestionTemplate
import com.example.model.EquipmentCategory
import com.example.ui.components.StatBadge
import com.example.ui.theme.FailCrimson

@Composable
fun SettingsScreen(
    settings: InspectorSettings,
    onToggleCategory: (String, Boolean) -> Unit,
    onToggleQuestion: (String, Boolean) -> Unit,
    onAddCustomQuestion: (String, String) -> Unit,
    onRemoveCustomQuestion: (String) -> Unit,
    onResetDefaults: () -> Unit,
    onDefaultInspectorNameChange: (String) -> Unit,
    onDefaultOrganizationChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeQuestionCount = remember(settings) {
        ChecklistCatalog.buildInspectionItems(
            enabledCategoryIds = settings.enabledCategoryIds,
            disabledQuestionIds = settings.disabledQuestionIds,
            customQuestions = settings.customQuestions,
        ).size
    }
    val totalCatalogQuestions = remember(settings.customQuestions) {
        ChecklistCatalog.categories.sumOf { it.questions.size } + settings.customQuestions.size
    }

    // Local text field state synced cleanly with DataStore so typing never drops characters
    var inspectorDraft by remember { mutableStateOf(settings.defaultInspectorName) }
    var orgDraft by remember { mutableStateOf(settings.defaultOrganization) }

    LaunchedEffect(settings.defaultInspectorName) {
        if (settings.defaultInspectorName != inspectorDraft) {
            inspectorDraft = settings.defaultInspectorName
        }
    }
    LaunchedEffect(settings.defaultOrganization) {
        if (settings.defaultOrganization != orgDraft) {
            orgDraft = settings.defaultOrganization
        }
    }

    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }
    val allExpanded = remember(expandedMap.toMap()) {
        ChecklistCatalog.categories.all { expandedMap[it.id] == true }
    }
    var addQuestionTargetCategory by remember { mutableStateOf<EquipmentCategory?>(null) }

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
            // 1. Inspector & Authority Defaults
            item(key = "inspector_defaults") {
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
                        Text(
                            text = "Inspector Profile Defaults",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OutlinedTextField(
                            value = inspectorDraft,
                            onValueChange = {
                                inspectorDraft = it
                                onDefaultInspectorNameChange(it)
                            },
                            label = { Text("Default Inspector Name") },
                            placeholder = { Text("Auto-fills new inspections") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_default_inspector_input"),
                        )
                        OutlinedTextField(
                            value = orgDraft,
                            onValueChange = {
                                orgDraft = it
                                onDefaultOrganizationChange(it)
                            },
                            label = { Text("Council / Organization Header") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_default_org_input"),
                        )
                    }
                }
            }

            // 2. Equipment & Checklist Configuration Header
            item(key = "equipment_config_header") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "Play Equipment & Questions",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                        StatBadge(
                            label = "$activeQuestionCount / $totalCatalogQuestions",
                            bgColor = MaterialTheme.colorScheme.primaryContainer,
                            fgColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Toggle equipment sections or individual checks. All settings persist offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                val next = !allExpanded
                                ChecklistCatalog.categories.forEach { cat ->
                                    expandedMap[cat.id] = next
                                }
                            },
                            modifier = Modifier.testTag("settings_expand_all_questions"),
                        ) {
                            Text(if (allExpanded) "Collapse All" else "Expand All Checks")
                        }
                    }
                }
            }

            // 3. Equipment Categories List
            items(
                items = ChecklistCatalog.categories,
                key = { it.id },
            ) { category ->
                val isCategoryEnabled = category.id in settings.enabledCategoryIds
                val customForCat = settings.customQuestions.filter { it.categoryId == category.id }
                val isExpanded = expandedMap[category.id] == true

                EquipmentToggleCard(
                    category = category,
                    customQuestions = customForCat,
                    isEnabled = isCategoryEnabled,
                    isExpanded = isExpanded,
                    disabledQuestionIds = settings.disabledQuestionIds,
                    onToggleExpand = { expandedMap[category.id] = !isExpanded },
                    onToggleCategory = { enabled -> onToggleCategory(category.id, enabled) },
                    onToggleQuestion = onToggleQuestion,
                    onAddQuestionClick = { addQuestionTargetCategory = category },
                    onRemoveCustomQuestion = onRemoveCustomQuestion,
                )
            }

            // 4. Restore Defaults Button
            item(key = "restore_defaults") {
                OutlinedButton(
                    onClick = onResetDefaults,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restore_defaults_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable All Equipment & Restore Default Checks")
                }
            }
        }
    }

    addQuestionTargetCategory?.let { category ->
        AddCustomQuestionDialog(
            category = category,
            onDismiss = { addQuestionTargetCategory = null },
            onAdd = { text ->
                onAddCustomQuestion(category.id, text)
                expandedMap[category.id] = true
                addQuestionTargetCategory = null
            },
        )
    }
}

@Composable
private fun EquipmentToggleCard(
    category: EquipmentCategory,
    customQuestions: List<ChecklistQuestionTemplate>,
    isEnabled: Boolean,
    isExpanded: Boolean,
    disabledQuestionIds: Set<String>,
    onToggleExpand: () -> Unit,
    onToggleCategory: (Boolean) -> Unit,
    onToggleQuestion: (String, Boolean) -> Unit,
    onAddQuestionClick: () -> Unit,
    onRemoveCustomQuestion: (String) -> Unit,
) {
    val allQuestions = remember(category, customQuestions) {
        category.questions + customQuestions
    }
    val activeQuestions = allQuestions.count { it.id !in disabledQuestionIds }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_category_card_${category.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isEnabled) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            },
        ),
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "${category.sectionNumber}. ${category.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        text = category.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleCategory,
                    modifier = Modifier.testTag("switch_category_${category.id}"),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatBadge(
                    label = if (isEnabled) "$activeQuestions / ${allQuestions.size} checks active" else "Disabled",
                    bgColor = MaterialTheme.colorScheme.surfaceVariant,
                    fgColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                TextButton(
                    onClick = onToggleExpand,
                    enabled = isEnabled,
                    modifier = Modifier.testTag("expand_questions_${category.id}"),
                ) {
                    Text(if (isExpanded) "Hide Questions" else "Toggle Questions (${allQuestions.size})")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded && isEnabled) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    allQuestions.forEach { q ->
                        val qEnabled = q.id !in disabledQuestionIds
                        val isCustom = q.id.startsWith("custom_")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = qEnabled,
                                onCheckedChange = { checked -> onToggleQuestion(q.id, checked) },
                                modifier = Modifier.testTag("checkbox_question_${q.id}"),
                            )
                            Text(
                                text = "${q.referenceCode}  ${q.questionText}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = if (qEnabled) TextDecoration.None else TextDecoration.LineThrough,
                                ),
                                color = if (qEnabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (isCustom) {
                                IconButton(
                                    onClick = { onRemoveCustomQuestion(q.id) },
                                    modifier = Modifier.testTag("delete_custom_question_${q.id}"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete custom question",
                                        tint = FailCrimson,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onAddQuestionClick,
                            modifier = Modifier.testTag("add_custom_question_${category.id}"),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Custom Question")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomQuestionDialog(
    category: EquipmentCategory,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var questionText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Question to ${category.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "New questions are persisted in Settings and included in your active checklist, PDF, and JSON exports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Inspection Check / Question") },
                    placeholder = { Text("e.g., Are swing bearings adequately oiled/greased?") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_question_dialog_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(questionText) },
                enabled = questionText.trim().isNotEmpty(),
                modifier = Modifier.testTag("custom_question_dialog_add_button"),
            ) {
                Text("Add Question")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
