package com.fouwaz.studypal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "tags")
    val tags: String = "", // Comma-separated tags

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "course_id")
    val courseId: String? = null,

    @ColumnInfo(name = "linked_lecture_ids")
    val linkedLectureIds: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
