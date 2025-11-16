package com.fouwaz.studypal.data.local.dao

import androidx.room.*
import com.fouwaz.studypal.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY unlocked_at DESC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements ORDER BY unlocked_at DESC")
    suspend fun getAllAchievementsSync(): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE milestone_type = :milestoneType LIMIT 1")
    suspend fun getAchievementByMilestone(milestoneType: String): AchievementEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM achievements WHERE milestone_type = :milestoneType)")
    suspend fun isMilestoneUnlocked(milestoneType: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity): Long

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("UPDATE achievements SET is_new = 0 WHERE id = :achievementId")
    suspend fun markAsSeen(achievementId: Long)

    @Query("UPDATE achievements SET is_new = 0")
    suspend fun markAllAsSeen()

    @Query("DELETE FROM achievements")
    suspend fun deleteAll()
}
