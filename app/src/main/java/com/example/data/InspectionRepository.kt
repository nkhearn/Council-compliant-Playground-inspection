package com.example.data

import com.example.data.local.InspectionReportDao
import com.example.data.local.InspectionReportEntity
import com.example.data.local.InspectorSettings
import com.example.data.local.SettingsDataStore
import com.example.model.InspectionReport
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InspectionRepository(
    private val dao: InspectionReportDao,
    private val settingsDataStore: SettingsDataStore,
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val reportAdapter = moshi.adapter(InspectionReport::class.java)

    val settingsFlow: Flow<InspectorSettings> = settingsDataStore.settingsFlow

    val savedReportsFlow: Flow<List<InspectionReportEntity>> = dao.observeAllReports()

    suspend fun saveReport(report: InspectionReport) {
        val updated = report.copy(updatedAtMillis = System.currentTimeMillis())
        val json = reportAdapter.toJson(updated)
        val entity = InspectionReportEntity(
            id = updated.id,
            siteName = updated.metadata.siteName.ifBlank { "Unnamed Play Area" },
            inspectorName = updated.metadata.inspectorName.ifBlank { "Unassigned Inspector" },
            inspectionDate = updated.metadata.inspectionDate,
            inspectionTime = updated.metadata.inspectionTime,
            weatherConditions = updated.metadata.weatherConditions,
            totalChecks = updated.items.size,
            passCount = updated.passCount,
            failCount = updated.failCount,
            naCount = updated.naCount,
            highestRisk = updated.highestRiskLevel?.label,
            updatedAtMillis = updated.updatedAtMillis,
            reportJson = json,
        )
        dao.upsertReport(entity)
    }

    suspend fun loadReport(id: String): InspectionReport? {
        val entity = dao.getReportById(id) ?: return null
        return runCatching { reportAdapter.fromJson(entity.reportJson) }.getOrNull()
    }

    fun decodeEntity(entity: InspectionReportEntity): InspectionReport? {
        return runCatching { reportAdapter.fromJson(entity.reportJson) }.getOrNull()
    }

    suspend fun deleteReport(id: String) {
        dao.deleteReportById(id)
    }

    suspend fun setCategoryEnabled(categoryId: String, enabled: Boolean) {
        settingsDataStore.setCategoryEnabled(categoryId, enabled)
    }

    suspend fun setQuestionEnabled(questionId: String, enabled: Boolean) {
        settingsDataStore.setQuestionEnabled(questionId, enabled)
    }

    suspend fun addCustomQuestion(question: com.example.model.ChecklistQuestionTemplate) {
        settingsDataStore.addCustomQuestion(question)
    }

    suspend fun removeCustomQuestion(questionId: String) {
        settingsDataStore.removeCustomQuestion(questionId)
    }

    suspend fun resetChecklistDefaults() {
        settingsDataStore.resetChecklistDefaults()
    }

    suspend fun setDefaultInspectorName(name: String) {
        settingsDataStore.setDefaultInspectorName(name)
    }

    suspend fun setDefaultOrganization(org: String) {
        settingsDataStore.setDefaultOrganization(org)
    }
}
