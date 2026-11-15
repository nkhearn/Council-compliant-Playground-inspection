package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspection_reports")
data class InspectionReportEntity(
    @PrimaryKey val id: String,
    val siteName: String,
    val inspectorName: String,
    val inspectionDate: String,
    val inspectionTime: String,
    val weatherConditions: String,
    val totalChecks: Int,
    val passCount: Int,
    val failCount: Int,
    val naCount: Int,
    val highestRisk: String?,
    val updatedAtMillis: Long,
    val reportJson: String,
)
