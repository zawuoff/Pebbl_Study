package com.fouwaz.studypal.data.local.dao

import androidx.room.*
import com.fouwaz.studypal.data.local.entity.LectureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures WHERE is_active = 1 ORDER BY created_at DESC")
    fun getAllLectures(): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures WHERE id = :lectureId")
    suspend fun getLectureById(lectureId: Long): LectureEntity?

    @Query("SELECT * FROM lectures WHERE id = :lectureId")
    fun getLectureByIdFlow(lectureId: Long): Flow<LectureEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: LectureEntity): Long

    @Update
    suspend fun updateLecture(lecture: LectureEntity)

    @Query("UPDATE lectures SET is_active = 0 WHERE id = :lectureId")
    suspend fun softDeleteLecture(lectureId: Long)

    @Delete
    suspend fun deleteLecture(lecture: LectureEntity)

    @Query("DELETE FROM lectures WHERE id = :lectureId")
    suspend fun deleteLectureById(lectureId: Long)

    @Query("SELECT COUNT(*) FROM lectures WHERE is_active = 1")
    suspend fun getActiveLectureCount(): Int

    @Query("SELECT SUM(word_count) FROM lectures WHERE is_active = 1")
    suspend fun getTotalWordCount(): Int?
}
