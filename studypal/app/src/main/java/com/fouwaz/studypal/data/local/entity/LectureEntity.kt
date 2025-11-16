package com.fouwaz.studypal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "lectures")
data class LectureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "transcription")
    val transcription: String,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int,

    @ColumnInfo(name = "word_count")
    val wordCount: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "course_id")
    val courseId: String? = null,

    @ColumnInfo(name = "project_id")
    val projectId: Long? = null,

    @ColumnInfo(name = "audio_url")
    val audioUrl: String? = null
)
