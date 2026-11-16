package com.example.model

import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class InspectionStatus(val pdfLabel: String, val uiLabel: String) {
    UNANSWERED("PENDING", "Pending"),
    PASS("YES", "Pass (Yes)"),
    FAIL("NO", "Fail (No)"),
    NA("N/A", "N/A"),
}

enum class RiskLevel(val label: String, val sortOrder: Int) {
    HIGH("High", 0),
    MEDIUM("Medium", 1),
    LOW("Low", 2),
}

fun riskLevelFromScore(score: Int): RiskLevel = when {
    score >= 15 -> RiskLevel.HIGH
    score >= 7 -> RiskLevel.MEDIUM
    else -> RiskLevel.LOW
}

fun probabilityLabel(value: Int): String = when (value) {
    1 -> "1 · Low"
    2 -> "2 · Unlikely"
    3 -> "3 · Possible"
    4 -> "4 · Likely"
    5 -> "5 · High"
    else -> value.toString()
}

fun severityLabel(value: Int): String = when (value) {
    1 -> "1 · Least"
    2 -> "2 · Minor"
    3 -> "3 · Moderate"
    4 -> "4 · Major"
    5 -> "5 · Severe"
    else -> value.toString()
}

enum class ActionRequired(val label: String) {
    MONITOR("Monitor"),
    REPAIR("Repair"),
    TAKE_OUT_OF_USE("Take Out of Use"),
}

enum class OverallActionRequired(val label: String) {
    NONE("None"),
    MINOR_REPAIR("Minor Repair"),
    REMOVE_ISOLATE("Remove / Isolate"),
}

@JsonClass(generateAdapter = true)
data class ChecklistQuestionTemplate(
    val id: String,
    val categoryId: String,
    val referenceCode: String,
    val questionText: String,
)

@JsonClass(generateAdapter = true)
data class EquipmentCategory(
    val id: String,
    val sectionNumber: Int,
    val title: String,
    val subtitle: String,
    val isCoreSiteSection: Boolean = false,
    val questions: List<ChecklistQuestionTemplate>,
)

@JsonClass(generateAdapter = true)
data class InspectionItem(
    val id: String,
    val categoryId: String,
    val sectionNumber: Int,
    val categoryTitle: String,
    val referenceCode: String,
    val question: String,
    val status: InspectionStatus = InspectionStatus.UNANSWERED,
    val inlineNote: String = "",
    val defectDetails: String = "",
    val probability: Int? = null,
    val severity: Int? = null,
    val riskLevel: RiskLevel? = null,
    val actionRequired: ActionRequired? = null,
) {
    val computedRiskScore: Int?
        get() = if (probability != null && severity != null) {
            probability.coerceIn(1, 5) * severity.coerceIn(1, 5)
        } else {
            when (riskLevel) {
                RiskLevel.HIGH -> 20
                RiskLevel.MEDIUM -> 9
                RiskLevel.LOW -> 4
                null -> null
            }
        }

    val effectiveRiskLevel: RiskLevel?
        get() = computedRiskScore?.let(::riskLevelFromScore) ?: riskLevel

    val isFailureComplete: Boolean
        get() = status != InspectionStatus.FAIL || (
            defectDetails.isNotBlank() &&
                probability != null &&
                severity != null &&
                actionRequired != null
        )
}

@JsonClass(generateAdapter = true)
data class RiskAssessmentEntry(
    val itemId: String,
    val sectionNumber: Int,
    val categoryTitle: String,
    val referenceCode: String,
    val question: String,
    val defectDetails: String,
    val probability: Int,
    val severity: Int,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val actionRequired: ActionRequired,
)

@JsonClass(generateAdapter = true)
data class MaintenanceLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val equipmentItem: String = "",
    val repairCarriedOut: String = "",
    val partsUsed: String = "",
    val completedByDate: String = "",
)

@JsonClass(generateAdapter = true)
data class InspectionMetadata(
    val formRef: String = "OIC-PA-CUSTOM-PXL-V2",
    val organizationName: String = "Orkney Islands Council — Education, Communities & Housing",
    val checklistTitle: String = "Play Area - Routine Safety Inspection",
    val inspectorName: String = "",
    val siteName: String = "",
    val inspectionDate: String = currentFormattedDate(),
    val inspectionTime: String = currentFormattedTime(),
    val weatherConditions: String = "Dry / Clear",
    val overallComments: String = "",
    val overallActionRequired: OverallActionRequired = OverallActionRequired.NONE,
    val inspectorSignature: String = "",
)

@JsonClass(generateAdapter = true)
data class InspectionReport(
    val id: String = UUID.randomUUID().toString(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val metadata: InspectionMetadata = InspectionMetadata(),
    val items: List<InspectionItem> = emptyList(),
    val maintenanceLogs: List<MaintenanceLogEntry> = emptyList(),
) {
    val failedItems: List<InspectionItem>
        get() = items.filter { it.status == InspectionStatus.FAIL }

    val riskAssessmentEntries: List<RiskAssessmentEntry>
        get() = failedItems
            .map { item ->
                val p = (item.probability ?: when (item.riskLevel) {
                    RiskLevel.HIGH -> 4
                    RiskLevel.LOW -> 2
                    else -> 3
                }).coerceIn(1, 5)
                val s = (item.severity ?: when (item.riskLevel) {
                    RiskLevel.HIGH -> 5
                    RiskLevel.LOW -> 2
                    else -> 3
                }).coerceIn(1, 5)
                val score = p * s
                RiskAssessmentEntry(
                    itemId = item.id,
                    sectionNumber = item.sectionNumber,
                    categoryTitle = item.categoryTitle,
                    referenceCode = item.referenceCode,
                    question = item.question,
                    defectDetails = item.defectDetails.ifBlank { "Defect details pending entry" },
                    probability = p,
                    severity = s,
                    riskScore = score,
                    riskLevel = riskLevelFromScore(score),
                    actionRequired = item.actionRequired ?: ActionRequired.MONITOR,
                )
            }
            .sortedByDescending { it.riskScore }

    val passCount: Int
        get() = items.count { it.status == InspectionStatus.PASS }

    val failCount: Int
        get() = items.count { it.status == InspectionStatus.FAIL }

    val naCount: Int
        get() = items.count { it.status == InspectionStatus.NA }

    val unansweredCount: Int
        get() = items.count { it.status == InspectionStatus.UNANSWERED }

    val answeredCount: Int
        get() = items.size - unansweredCount

    val incompleteFailureCount: Int
        get() = failedItems.count { !it.isFailureComplete }

    val highestRiskScore: Int?
        get() = riskAssessmentEntries.maxOfOrNull { it.riskScore }

    val highestRiskLevel: RiskLevel?
        get() = highestRiskScore?.let(::riskLevelFromScore)

    val highRiskCount: Int
        get() = riskAssessmentEntries.count { it.riskLevel == RiskLevel.HIGH }

    val mediumRiskCount: Int
        get() = riskAssessmentEntries.count { it.riskLevel == RiskLevel.MEDIUM }

    val lowRiskCount: Int
        get() = riskAssessmentEntries.count { it.riskLevel == RiskLevel.LOW }
}

@JsonClass(generateAdapter = true)
data class JsonReportSummary(
    val totalChecks: Int,
    val answeredChecks: Int,
    val passCount: Int,
    val failCount: Int,
    val naCount: Int,
    val unansweredCount: Int,
    val highestRiskScore: Int?,
    val highestRiskLevel: String,
    val highRiskCount: Int,
    val mediumRiskCount: Int,
    val lowRiskCount: Int,
)

@JsonClass(generateAdapter = true)
data class InspectionJsonExport(
    val schemaVersion: String = "1.1",
    val exportedAtIso: String,
    val reportId: String,
    val metadata: InspectionMetadata,
    val summary: JsonReportSummary,
    val checklistItems: List<InspectionItem>,
    val riskAssessmentAndActionPlan: List<RiskAssessmentEntry>,
    val maintenanceLogs: List<MaintenanceLogEntry>,
)

fun currentFormattedDate(): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

fun currentFormattedTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
