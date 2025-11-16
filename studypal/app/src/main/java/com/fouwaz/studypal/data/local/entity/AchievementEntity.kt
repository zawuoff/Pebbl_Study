package com.fouwaz.studypal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "milestone_type")
    val milestoneType: String, // "words_1000", "words_2500", etc.

    @ColumnInfo(name = "pebble_type")
    val pebbleType: String,    // "gray_smooth", "cream_speckled", etc.

    @ColumnInfo(name = "unlocked_at")
    val unlockedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_new")
    val isNew: Boolean = true   // for showing "new" badge
)
