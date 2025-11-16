package com.fouwaz.studypal.data.local.dao

import androidx.room.*
import com.fouwaz.studypal.data.local.entity.VoiceStreamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceStreamDao {
    @Query("SELECT * FROM voice_streams WHERE project_id = :projectId ORDER BY sequence_number ASC")
    fun getStreamsByProject(projectId: Long): Flow<List<VoiceStreamEntity>>

    @Query("SELECT * FROM voice_streams WHERE project_id = :projectId ORDER BY sequence_number ASC")
    suspend fun getStreamsByProjectSync(projectId: Long): List<VoiceStreamEntity>

    @Query("SELECT * FROM voice_streams WHERE id = :streamId")
    suspend fun getStreamById(streamId: Long): VoiceStreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStream(stream: VoiceStreamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<VoiceStreamEntity>)

    @Update
    suspend fun updateStream(stream: VoiceStreamEntity)

    @Delete
    suspend fun deleteStream(stream: VoiceStreamEntity)

    @Query("DELETE FROM voice_streams WHERE project_id = :projectId")
    suspend fun deleteStreamsByProject(projectId: Long)

    @Query("SELECT MAX(sequence_number) FROM voice_streams WHERE project_id = :projectId")
    suspend fun getLastSequenceNumber(projectId: Long): Int?

    @Query("SELECT COUNT(*) FROM voice_streams WHERE project_id = :projectId")
    suspend fun getStreamCountByProject(projectId: Long): Int
}
