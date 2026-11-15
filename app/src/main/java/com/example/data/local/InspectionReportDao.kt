package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionReportDao {
    @Query("SELECT * FROM inspection_reports ORDER BY updatedAtMillis DESC")
    fun observeAllReports(): Flow<List<InspectionReportEntity>>

    @Query("SELECT * FROM inspection_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: String): InspectionReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReport(report: InspectionReportEntity)

    @Query("DELETE FROM inspection_reports WHERE id = :id")
    suspend fun deleteReportById(id: String)
}
