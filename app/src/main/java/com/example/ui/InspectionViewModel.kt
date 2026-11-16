package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChecklistCatalog
import com.example.data.InspectionRepository
import com.example.data.local.AppDatabase
import com.example.data.local.InspectionReportEntity
import com.example.data.local.InspectorSettings
import com.example.data.local.SettingsDataStore
import com.example.export.JsonReportExporter
import com.example.export.PdfReportGenerator
import com.example.export.ReportShareHelper
import com.example.model.ActionRequired
import com.example.model.ChecklistQuestionTemplate
import com.example.model.InspectionItem
import com.example.model.InspectionMetadata
import com.example.model.InspectionReport
import com.example.model.InspectionStatus
import com.example.model.MaintenanceLogEntry
import com.example.model.OverallActionRequired
import com.example.model.RiskLevel
import com.example.model.currentFormattedDate
import com.example.model.currentFormattedTime
import com.example.model.riskLevelFromScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
    data class ShareExportedFile(
        val file: File,
        val mimeType: String,
        val chooserTitle: String,
        val report: InspectionReport,
    ) : UiEvent
    data object NavigateToInspect : UiEvent
    data object NavigateToRiskExport : UiEvent
}

class InspectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InspectionRepository
    private val pdfGenerator = PdfReportGenerator(application)
    private val jsonExporter = JsonReportExporter(application)

    val settings: StateFlow<InspectorSettings>
    val savedReports: StateFlow<List<InspectionReportEntity>>

    private val _activeReport = MutableStateFlow(createBlankReport(InspectorSettings()))
    val activeReport: StateFlow<InspectionReport> = _activeReport.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var hasUserEditedInspector = false

    init {
        val db = AppDatabase.getInstance(application)
        val dataStore = SettingsDataStore(application)
        repository = InspectionRepository(db.inspectionReportDao(), dataStore)

        settings = repository.settingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = InspectorSettings(),
        )

        savedReports = repository.savedReportsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

        viewModelScope.launch(Dispatchers.IO) {
            ReportShareHelper.cleanupOldExports(application)
        }

        viewModelScope.launch {
            repository.settingsFlow.collect { latestSettings ->
                syncChecklistWithSettings(latestSettings)
            }
        }
    }

    private fun createBlankReport(currentSettings: InspectorSettings): InspectionReport {
        return InspectionReport(
            id = UUID.randomUUID().toString(),
            metadata = InspectionMetadata(
                organizationName = currentSettings.defaultOrganization,
                inspectorName = currentSettings.defaultInspectorName,
                inspectionDate = currentFormattedDate(),
                inspectionTime = currentFormattedTime(),
            ),
            items = ChecklistCatalog.buildInspectionItems(
                enabledCategoryIds = currentSettings.enabledCategoryIds,
                disabledQuestionIds = currentSettings.disabledQuestionIds,
                customQuestions = currentSettings.customQuestions,
            ),
        )
    }

    private fun syncChecklistWithSettings(latestSettings: InspectorSettings) {
        _activeReport.update { current ->
            val existingMap = current.items.associateBy { it.id }
            val rebuiltItems = ChecklistCatalog.buildInspectionItems(
                enabledCategoryIds = latestSettings.enabledCategoryIds,
                disabledQuestionIds = latestSettings.disabledQuestionIds,
                customQuestions = latestSettings.customQuestions,
                existingItemsById = existingMap,
            )
            val updatedInspector = if (!hasUserEditedInspector && current.metadata.inspectorName.isBlank()) {
                latestSettings.defaultInspectorName
            } else {
                current.metadata.inspectorName
            }
            current.copy(
                metadata = current.metadata.copy(
                    inspectorName = updatedInspector,
                    organizationName = latestSettings.defaultOrganization,
                ),
                items = rebuiltItems,
            )
        }
        val activeFilter = _selectedCategoryFilter.value
        if (activeFilter != null && activeFilter !in latestSettings.enabledCategoryIds) {
            _selectedCategoryFilter.value = null
        }
    }

    fun selectCategoryFilter(categoryId: String?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun updateInspectorName(name: String) {
        hasUserEditedInspector = true
        _activeReport.update {
            it.copy(
                updatedAtMillis = System.currentTimeMillis(),
                metadata = it.metadata.copy(inspectorName = name),
            )
        }
    }

    fun updateSiteName(siteName: String) {
        _activeReport.update {
            it.copy(
                updatedAtMillis = System.currentTimeMillis(),
                metadata = it.metadata.copy(siteName = siteName),
            )
        }
    }

    fun updateInspectionDate(date: String) {
        _activeReport.update {
            it.copy(
                updatedAtMillis = System.currentTimeMillis(),
                metadata = it.metadata.copy(inspectionDate = date),
            )
        }
    }

    fun updateInspectionTime(time: String) {
        _activeReport.update {
            it.copy(
                updatedAtMillis = System.currentTimeMillis(),
                metadata = it.metadata.copy(inspectionTime = time),
            )
        }
    }

    fun updateWeatherConditions(weather: String) {
        _activeReport.update {
            it.copy(
                updatedAtMillis = System.currentTimeMillis(),
                metadata = it.metadata.copy(weatherConditions = weather),
            )
        }
    }

    fun updateOverallComments(comments: String) {
        _activeReport.update {
            it.copy(
                updatedAtMillis = System.currentTimeMillis(),
                metadata = it.metadata.copy(overallComments = comments),
            )
        }
    }

    fun updateOverallActionRequired(action: OverallActionRequired) {
        _activeReport.update {
            it.copy(
                updatedAtMillis = System.currentTimeMillis(),
                metadata = it.metadata.copy(overallActionRequired = action),
            )
        }
    }

    fun updateInspectorSignature(signature: String) {
        _activeReport.update {
            it.copy(
                updatedAtMillis = System.currentTimeMillis(),
                metadata = it.metadata.copy(inspectorSignature = signature),
            )
        }
    }

    fun setItemStatus(itemId: String, status: InspectionStatus) {
        updateItem(itemId) { item ->
            val toggledStatus = if (item.status == status) InspectionStatus.UNANSWERED else status
            item.copy(status = toggledStatus)
        }
    }

    fun updateItemNote(itemId: String, note: String) {
        updateItem(itemId) { it.copy(inlineNote = note) }
    }

    fun updateItemDefectDetails(itemId: String, details: String) {
        updateItem(itemId) { it.copy(defectDetails = details) }
    }

    fun updateItemProbability(itemId: String, probability: Int) {
        val clamped = probability.coerceIn(1, 5)
        updateItem(itemId) { item ->
            val updatedRisk = item.severity?.let { sev -> riskLevelFromScore(clamped * sev) } ?: item.riskLevel
            item.copy(probability = clamped, riskLevel = updatedRisk)
        }
    }

    fun updateItemSeverity(itemId: String, severity: Int) {
        val clamped = severity.coerceIn(1, 5)
        updateItem(itemId) { item ->
            val updatedRisk = item.probability?.let { prob -> riskLevelFromScore(prob * clamped) } ?: item.riskLevel
            item.copy(severity = clamped, riskLevel = updatedRisk)
        }
    }

    fun updateItemRiskLevel(itemId: String, riskLevel: RiskLevel) {
        updateItem(itemId) { it.copy(riskLevel = riskLevel) }
    }

    fun updateItemActionRequired(itemId: String, actionRequired: ActionRequired) {
        updateItem(itemId) { it.copy(actionRequired = actionRequired) }
    }

    fun markCategoryAllPass(categoryId: String) {
        _activeReport.update { report ->
            val updatedItems = report.items.map { item ->
                if (item.categoryId == categoryId && item.status == InspectionStatus.UNANSWERED) {
                    item.copy(status = InspectionStatus.PASS)
                } else {
                    item
                }
            }
            report.copy(updatedAtMillis = System.currentTimeMillis(), items = updatedItems)
        }
    }

    fun toggleCategoryNa(categoryId: String) {
        _activeReport.update { report ->
            val sectionItems = report.items.filter { it.categoryId == categoryId }
            val isCurrentlyAllNa = sectionItems.isNotEmpty() &&
                sectionItems.all { it.status == InspectionStatus.NA }
            val targetStatus = if (isCurrentlyAllNa) {
                InspectionStatus.UNANSWERED
            } else {
                InspectionStatus.NA
            }
            val updatedItems = report.items.map { item ->
                if (item.categoryId == categoryId) {
                    item.copy(status = targetStatus)
                } else {
                    item
                }
            }
            report.copy(updatedAtMillis = System.currentTimeMillis(), items = updatedItems)
        }
    }

    fun addMaintenanceLog(entry: MaintenanceLogEntry) {
        _activeReport.update { report ->
            report.copy(
                updatedAtMillis = System.currentTimeMillis(),
                maintenanceLogs = report.maintenanceLogs + entry,
            )
        }
    }

    fun removeMaintenanceLog(entryId: String) {
        _activeReport.update { report ->
            report.copy(
                updatedAtMillis = System.currentTimeMillis(),
                maintenanceLogs = report.maintenanceLogs.filterNot { it.id == entryId },
            )
        }
    }

    private fun updateItem(itemId: String, transform: (InspectionItem) -> InspectionItem) {
        _activeReport.update { report ->
            val updatedItems = report.items.map { item ->
                if (item.id == itemId) transform(item) else item
            }
            report.copy(updatedAtMillis = System.currentTimeMillis(), items = updatedItems)
        }
    }

    fun saveActiveReport() {
        viewModelScope.launch {
            repository.saveReport(_activeReport.value)
            _events.emit(UiEvent.ShowMessage("Inspection report saved offline."))
        }
    }

    fun loadSavedReport(reportId: String) {
        viewModelScope.launch {
            val loaded = repository.loadReport(reportId)
            if (loaded != null) {
                hasUserEditedInspector = true
                _activeReport.value = loaded
                _selectedCategoryFilter.value = null
                _events.emit(UiEvent.NavigateToInspect)
                _events.emit(UiEvent.ShowMessage("Loaded report for ${loaded.metadata.siteName.ifBlank { "Play Area" }}."))
            } else {
                _events.emit(UiEvent.ShowMessage("Unable to load saved inspection."))
            }
        }
    }

    fun deleteSavedReport(reportId: String) {
        viewModelScope.launch {
            repository.deleteReport(reportId)
            _events.emit(UiEvent.ShowMessage("Saved report deleted."))
        }
    }

    fun startNewInspection() {
        hasUserEditedInspector = false
        _selectedCategoryFilter.value = null
        _activeReport.value = createBlankReport(settings.value)
        viewModelScope.launch {
            _events.emit(UiEvent.NavigateToInspect)
            _events.emit(UiEvent.ShowMessage("Started a fresh routine inspection."))
        }
    }

    fun navigateToRiskExport() {
        viewModelScope.launch {
            _events.emit(UiEvent.NavigateToRiskExport)
        }
    }

    fun exportAndSharePdf(targetReport: InspectionReport = _activeReport.value) {
        if (_isExporting.value) return
        viewModelScope.launch {
            _isExporting.value = true
            try {
                if (targetReport.id == _activeReport.value.id && targetReport.answeredCount > 0) {
                    repository.saveReport(targetReport)
                }
                val pdfFile = withContext(Dispatchers.IO) {
                    pdfGenerator.generatePdf(targetReport)
                }
                _events.emit(
                    UiEvent.ShareExportedFile(
                        file = pdfFile,
                        mimeType = "application/pdf",
                        chooserTitle = "Share / Email Inspection PDF",
                        report = targetReport,
                    ),
                )
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage("PDF export failed: ${e.localizedMessage ?: "Unknown error"}"))
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportAndShareJson(targetReport: InspectionReport = _activeReport.value) {
        if (_isExporting.value) return
        viewModelScope.launch {
            _isExporting.value = true
            try {
                if (targetReport.id == _activeReport.value.id && targetReport.answeredCount > 0) {
                    repository.saveReport(targetReport)
                }
                val jsonFile = withContext(Dispatchers.IO) {
                    jsonExporter.exportToJsonFile(targetReport)
                }
                _events.emit(
                    UiEvent.ShareExportedFile(
                        file = jsonFile,
                        mimeType = "application/json",
                        chooserTitle = "Export / Share Inspection JSON",
                        report = targetReport,
                    ),
                )
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage("JSON export failed: ${e.localizedMessage ?: "Unknown error"}"))
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportSavedEntityPdf(entity: InspectionReportEntity) {
        val decoded = repository.decodeEntity(entity)
        if (decoded != null) {
            exportAndSharePdf(decoded)
        } else {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("Could not decode saved report."))
            }
        }
    }

    fun exportSavedEntityJson(entity: InspectionReportEntity) {
        val decoded = repository.decodeEntity(entity)
        if (decoded != null) {
            exportAndShareJson(decoded)
        } else {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("Could not decode saved report."))
            }
        }
    }

    fun setCategoryEnabled(categoryId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setCategoryEnabled(categoryId, enabled)
        }
    }

    fun setQuestionEnabled(questionId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setQuestionEnabled(questionId, enabled)
        }
    }

    fun addCustomQuestion(categoryId: String, questionText: String) {
        val trimmed = questionText.trim()
        if (trimmed.isEmpty()) return
        val category = ChecklistCatalog.categories.firstOrNull { it.id == categoryId } ?: return
        val existingCustomCount = settings.value.customQuestions.count { it.categoryId == categoryId }
        val nextIndex = category.questions.size + existingCustomCount + 1
        val newQuestion = ChecklistQuestionTemplate(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            categoryId = categoryId,
            referenceCode = "${category.sectionNumber}.$nextIndex",
            questionText = trimmed,
        )
        viewModelScope.launch {
            repository.addCustomQuestion(newQuestion)
            _events.emit(UiEvent.ShowMessage("Added question to ${category.title}."))
        }
    }

    fun removeCustomQuestion(questionId: String) {
        viewModelScope.launch {
            repository.removeCustomQuestion(questionId)
            _events.emit(UiEvent.ShowMessage("Removed custom question."))
        }
    }

    fun resetChecklistDefaults() {
        viewModelScope.launch {
            repository.resetChecklistDefaults()
            _events.emit(UiEvent.ShowMessage("All 6 equipment sections & default checks restored."))
        }
    }

    fun setDefaultInspectorName(name: String) {
        viewModelScope.launch {
            repository.setDefaultInspectorName(name)
        }
    }

    fun setDefaultOrganization(org: String) {
        viewModelScope.launch {
            repository.setDefaultOrganization(org)
        }
    }
}
