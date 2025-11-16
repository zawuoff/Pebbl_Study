package com.fouwaz.studypal.domain.model

import com.fouwaz.studypal.data.local.entity.ProjectEntity

data class Project(
    val id: Long = 0,
    val title: String,
    val tags: List<String> = emptyList(),
    val description: String? = null,
    val courseId: String? = null,
    val linkedLectureIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    fun toEntity(): ProjectEntity {
        return ProjectEntity(
            id = id,
            title = title,
            tags = tags.joinToString(","),
            description = description,
            courseId = courseId,
            linkedLectureIds = linkedLectureIds.joinToString(","),
            createdAt = createdAt,
            updatedAt = updatedAt,
            isActive = isActive
        )
    }

    companion object {
        fun fromEntity(entity: ProjectEntity): Project {
            return Project(
                id = entity.id,
                title = entity.title,
                tags = entity.tags.split(",").filter { it.isNotBlank() },
                description = entity.description,
                courseId = entity.courseId,
                linkedLectureIds = entity.linkedLectureIds.split(",").filter { it.isNotBlank() },
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                isActive = entity.isActive
            )
        }
    }
}
