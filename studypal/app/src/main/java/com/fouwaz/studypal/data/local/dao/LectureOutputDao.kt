package com.fouwaz.studypal.data.local.dao

import androidx.room.*
import com.fouwaz.studypal.data.local.entity.LectureOutputEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureOutputDao {
    @Query("SELECT * FROM lecture_outputs WHERE lecture_id = :lectureId ORDER BY created_at ASC")
    fun getOutputsForLecture(lectureId: Long): Flow<List<LectureOutputEntity>>

    @Query("SELECT * FROM lecture_outputs WHERE lecture_id = :lectureId AND output_type = :outputType")
    suspend fun getOutputByType(lectureId: Long, outputType: String): LectureOutputEntity?

    @Query("SELECT * FROM lecture_outputs WHERE lecture_id = :lectureId AND output_type = :outputType")
    fun getOutputByTypeFlow(lectureId: Long, outputType: String): Flow<LectureOutputEntity?>

    @Query("SELECT * FROM lecture_outputs WHERE id = :outputId")
    suspend fun getOutputById(outputId: Long): LectureOutputEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutput(output: LectureOutputEntity): Long

    @Update
    suspend fun updateOutput(output: LectureOutputEntity)

    @Delete
    suspend fun deleteOutput(output: LectureOutputEntity)

    @Query("DELETE FROM lecture_outputs WHERE id = :outputId")
    suspend fun deleteOutputById(outputId: Long)

    @Query("DELETE FROM lecture_outputs WHERE lecture_id = :lectureId")
    suspend fun deleteAllOutputsForLecture(lectureId: Long)
}
