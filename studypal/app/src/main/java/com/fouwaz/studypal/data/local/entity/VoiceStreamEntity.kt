package com.fouwaz.studypal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "voice_streams",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["project_id"])]
)
data class VoiceStreamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "project_id")
    val projectId: Long,

    @ColumnInfo(name = "transcribed_text")
    val transcribedText: String,

    @ColumnInfo(name = "ai_question")
    val aiQuestion: String? = null,

    @ColumnInfo(name = "ai_question_1")
    val aiQuestion1: String? = null,

    @ColumnInfo(name = "ai_question_2")
    val aiQuestion2: String? = null,

    @ColumnInfo(name = "ai_question_3")
    val aiQuestion3: String? = null,

    @ColumnInfo(name = "selected_question_index")
    val selectedQuestionIndex: Int? = null,

    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
