package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ChecklistCatalog
import com.example.data.local.SettingsDataStore
import com.example.export.JsonReportExporter
import com.example.export.PdfReportGenerator
import com.example.export.ReportShareHelper
import com.example.model.ActionRequired
import com.example.model.ChecklistQuestionTemplate
import com.example.model.InspectionMetadata
import com.example.model.InspectionReport
import com.example.model.InspectionStatus
import com.example.model.MaintenanceLogEntry
import com.example.model.OverallActionRequired
import com.example.model.RiskLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches PlaySafe Audit`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PlaySafe Audit", appName)
    }

    @Test
    fun `catalog uses generic questions without colour names or combined repair and grease checks`() {
        val allItems = ChecklistCatalog.buildInspectionItems()
        assertEquals(6, ChecklistCatalog.categories.size)
        assertEquals(41, allItems.size)

        // Ensure no question combines good repair/smoothness and greased/oiled in the same question
        allItems.forEach { item ->
            val lower = item.question.lowercase()
            val mentionsRepairOrSmooth = lower.contains("repair") || lower.contains("smoothly") || lower.contains("wear")
            val mentionsGrease = lower.contains("greased") || lower.contains("oiled")
            assertFalse(
                "Question '${item.question}' combines repair/operation and lubrication!",
                mentionsRepairOrSmooth && mentionsGrease,
            )
        }

        // Ensure swing and hand grip questions are generic (no hardcoded colours)
        val swingSeat = allItems.first { it.id == "swg_01" }.question.lowercase()
        assertFalse(swingSeat.contains("red"))
        val seesawGrips = allItems.first { it.id == "ssw_02" }.question.lowercase()
        assertFalse(seesawGrips.contains("green"))
    }

    @Test
    fun `settings datastore persists category toggles, individual question toggles, and custom questions`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsDataStore(context)

        store.setCategoryEnabled("space_net", false)
        store.setQuestionEnabled("gen_05", false)
        store.setDefaultInspectorName("M. Henderson")
        store.addCustomQuestion(
            ChecklistQuestionTemplate(
                id = "custom_test_1",
                categoryId = "basket_swing",
                referenceCode = "2.8",
                questionText = "Are swing frame safety signs legible?",
            ),
        )

        // Read back with a fresh SettingsDataStore instance to verify disk persistence
        val reloaded = SettingsDataStore(context).settingsFlow.first()
        assertFalse("space_net" in reloaded.enabledCategoryIds)
        assertTrue("gen_05" in reloaded.disabledQuestionIds)
        assertEquals("M. Henderson", reloaded.defaultInspectorName)
        assertTrue(reloaded.customQuestions.any { it.id == "custom_test_1" })

        val builtItems = ChecklistCatalog.buildInspectionItems(
            enabledCategoryIds = reloaded.enabledCategoryIds,
            disabledQuestionIds = reloaded.disabledQuestionIds,
            customQuestions = reloaded.customQuestions,
        )
        assertTrue(builtItems.none { it.categoryId == "space_net" })
        assertTrue(builtItems.none { it.id == "gen_05" })
        assertTrue(builtItems.any { it.id == "custom_test_1" })
    }

    @Test
    fun `risk assessment compiles failed checks using 1-5 probability x 1-5 severity = 1-25 score`() {
        val items = ChecklistCatalog.buildInspectionItems().map { item ->
            when (item.id) {
                "gen_05" -> item.copy(
                    status = InspectionStatus.FAIL,
                    defectDetails = "Gate latch spring broken",
                    probability = 2,
                    severity = 2,
                    actionRequired = ActionRequired.REPAIR,
                )
                "swg_03" -> item.copy(
                    status = InspectionStatus.FAIL,
                    defectDetails = "Shackle pin worn past 30% tolerance",
                    probability = 4,
                    severity = 5,
                    actionRequired = ActionRequired.TAKE_OUT_OF_USE,
                )
                else -> item.copy(status = InspectionStatus.PASS)
            }
        }

        val report = InspectionReport(
            metadata = InspectionMetadata(
                inspectorName = "A. Fraser",
                siteName = "Stromness Harbour Play Park",
                weatherConditions = "Windy",
            ),
            items = items,
        )

        assertEquals(2, report.failCount)
        assertEquals(39, report.passCount)
        assertEquals(20, report.highestRiskScore)
        assertEquals(RiskLevel.HIGH, report.highestRiskLevel)
        assertEquals(2, report.riskAssessmentEntries.size)
        assertEquals("swg_03", report.riskAssessmentEntries[0].itemId)
        assertEquals(20, report.riskAssessmentEntries[0].riskScore)
        assertEquals("gen_05", report.riskAssessmentEntries[1].itemId)
        assertEquals(4, report.riskAssessmentEntries[1].riskScore)
        assertEquals(RiskLevel.LOW, report.riskAssessmentEntries[1].riskLevel)
    }

    @Test
    fun `pdf pagination, json export, and FileProvider URI resolve cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val items = ChecklistCatalog.buildInspectionItems().mapIndexed { index, item ->
            when (index) {
                0 -> item.copy(status = InspectionStatus.PASS, inlineNote = "Access clear")
                4 -> item.copy(
                    status = InspectionStatus.FAIL,
                    defectDetails = "Self-closing gate latch sticking",
                    probability = 3,
                    severity = 4,
                    actionRequired = ActionRequired.REPAIR,
                )
                7 -> item.copy(status = InspectionStatus.NA)
                else -> item.copy(status = InspectionStatus.PASS)
            }
        }

        val report = InspectionReport(
            metadata = InspectionMetadata(
                inspectorName = "J. Sinclair",
                siteName = "Kirkwall Peedie Sea Play Park",
                weatherConditions = "Dry / Clear",
                overallComments = "Minor gate latch adjustment required.",
                overallActionRequired = OverallActionRequired.MINOR_REPAIR,
                inspectorSignature = "J. Sinclair",
            ),
            items = items,
            maintenanceLogs = listOf(
                MaintenanceLogEntry(
                    equipmentItem = "Nest Swing",
                    repairCarriedOut = "Lubricated top swivel bearings",
                    partsUsed = "Marine grease",
                    completedByDate = "J. Sinclair",
                ),
            ),
        )

        val pageCount = PdfReportGenerator(context).calculatePageCount(report)
        assertTrue("Expected compact multi-page report (2-3 pages), got $pageCount", pageCount in 2..3)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dummyPdf = File(exportDir, "test_report.pdf").apply { writeText("%PDF-1.4\n") }
        val pdfUri = ReportShareHelper.getShareUri(context, dummyPdf)
        assertTrue(pdfUri.toString().contains("fileprovider"))

        val jsonFile = JsonReportExporter(context).exportToJsonFile(report)
        assertTrue(jsonFile.exists())
        val jsonContent = jsonFile.readText()
        assertTrue(jsonContent.contains("Kirkwall Peedie Sea Play Park"))
        assertTrue(jsonContent.contains("Self-closing gate latch sticking"))
        assertTrue(jsonContent.contains("riskAssessmentAndActionPlan"))
    }
}
