package com.fouwaz.studypal.data.local.dao

import androidx.room.*
import com.fouwaz.studypal.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE is_active = 1 ORDER BY updated_at DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectByIdFlow(projectId: Long): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET is_active = 0 WHERE id = :projectId")
    suspend fun softDeleteProject(projectId: Long)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    @Query("SELECT COUNT(*) FROM projects WHERE is_active = 1")
    suspend fun getActiveProjectCount(): Int
}
