package com.fouwaz.studypal.data.local.dao

import androidx.room.*
import com.fouwaz.studypal.data.local.entity.DraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts WHERE project_id = :projectId ORDER BY created_at DESC")
    fun getDraftsByProject(projectId: Long): Flow<List<DraftEntity>>

    @Query("SELECT * FROM drafts WHERE project_id = :projectId AND is_current = 1 LIMIT 1")
    suspend fun getCurrentDraft(projectId: Long): DraftEntity?

    @Query("SELECT * FROM drafts WHERE project_id = :projectId AND is_current = 1 LIMIT 1")
    fun getCurrentDraftFlow(projectId: Long): Flow<DraftEntity?>

    @Query("SELECT * FROM drafts WHERE id = :draftId")
    suspend fun getDraftById(draftId: Long): DraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftEntity): Long

    @Update
    suspend fun updateDraft(draft: DraftEntity)

    @Delete
    suspend fun deleteDraft(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE project_id = :projectId")
    suspend fun deleteDraftsByProject(projectId: Long)

    @Query("UPDATE drafts SET is_current = 0 WHERE project_id = :projectId")
    suspend fun markAllDraftsAsOld(projectId: Long)

    @Transaction
    suspend fun insertNewDraft(draft: DraftEntity): Long {
        markAllDraftsAsOld(draft.projectId)
        return insertDraft(draft)
    }

    @Query("SELECT COUNT(*) FROM drafts WHERE project_id = :projectId")
    suspend fun getDraftCountByProject(projectId: Long): Int
}
