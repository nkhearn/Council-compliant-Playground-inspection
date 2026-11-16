package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.InspectionReport
import java.io.File

object ReportShareHelper {

    fun cleanupOldExports(context: Context, maxAgeMillis: Long = 24 * 60 * 60 * 1000L) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) return
        val cutoff = System.currentTimeMillis() - maxAgeMillis
        exportDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    fun getShareUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String,
        report: InspectionReport,
    ) {
        val uri = getShareUri(context, file)
        val siteLabel = report.metadata.siteName.ifBlank { "Playground" }
        val subject = "PlaySafe Audit Report — $siteLabel (${report.metadata.inspectionDate})"
        val body = buildString {
            appendLine("Routine Play Area Safety Inspection Report")
            appendLine("Site / Play Area: $siteLabel")
            appendLine("Inspector: ${report.metadata.inspectorName.ifBlank { "Not specified" }}")
            appendLine("Date & Time: ${report.metadata.inspectionDate} ${report.metadata.inspectionTime}")
            appendLine("Weather: ${report.metadata.weatherConditions}")
            appendLine()
            appendLine("Results Summary: ${report.passCount} Pass (Yes) | ${report.failCount} Fail (No) | ${report.naCount} N/A")
            val peakRisk = report.highestRiskScore?.let { "$it/25 (${report.highestRiskLevel?.label ?: ""})" } ?: "None (All Passed)"
            appendLine("Highest Risk Score: $peakRisk")
            if (report.failCount > 0) {
                appendLine()
                appendLine("Failed Checks Requiring Attention:")
                report.riskAssessmentEntries.forEach { entry ->
                    appendLine("- [Risk ${entry.probability}x${entry.severity}=${entry.riskScore}/25 ${entry.riskLevel.label}] ${entry.referenceCode} (${entry.categoryTitle}): ${entry.defectDetails} -> ${entry.actionRequired.label}")
                }
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
