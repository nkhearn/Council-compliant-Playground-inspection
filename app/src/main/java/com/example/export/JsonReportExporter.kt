package com.example.export

import android.content.Context
import com.example.model.InspectionJsonExport
import com.example.model.InspectionReport
import com.example.model.JsonReportSummary
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JsonReportExporter(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(InspectionJsonExport::class.java).indent("  ")

    fun exportToJsonFile(report: InspectionReport): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedSite = report.metadata.siteName
            .ifBlank { "Playground" }
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .take(28)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(exportDir, "PlaySafe_${sanitizedSite}_$timestamp.json")

        val isoTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())
        val payload = InspectionJsonExport(
            exportedAtIso = isoTimestamp,
            reportId = report.id,
            metadata = report.metadata,
            summary = JsonReportSummary(
                totalChecks = report.items.size,
                answeredChecks = report.answeredCount,
                passCount = report.passCount,
                failCount = report.failCount,
                naCount = report.naCount,
                unansweredCount = report.unansweredCount,
                highestRiskLevel = report.highestRiskLevel?.label ?: "None",
                highRiskCount = report.highRiskCount,
                mediumRiskCount = report.mediumRiskCount,
                lowRiskCount = report.lowRiskCount,
            ),
            checklistItems = report.items,
            riskAssessmentAndActionPlan = report.riskAssessmentEntries,
            maintenanceLogs = report.maintenanceLogs,
        )

        outputFile.writeText(adapter.toJson(payload), Charsets.UTF_8)
        return outputFile
    }
}
