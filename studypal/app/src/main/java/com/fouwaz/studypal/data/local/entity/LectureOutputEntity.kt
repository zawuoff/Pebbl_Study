package com.fouwaz.studypal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "lecture_outputs",
    foreignKeys = [
        ForeignKey(
            entity = LectureEntity::class,
            parentColumns = ["id"],
            childColumns = ["lecture_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["lecture_id"])]
)
data class LectureOutputEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "lecture_id")
    val lectureId: Long,

    @ColumnInfo(name = "output_type")
    val outputType: String, // "flashcards", "notes", "detailed_notes", "summary"

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
