package com.fouwaz.studypal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fouwaz.studypal.data.local.dao.AchievementDao
import com.fouwaz.studypal.data.local.dao.DraftDao
import com.fouwaz.studypal.data.local.dao.LectureDao
import com.fouwaz.studypal.data.local.dao.LectureOutputDao
import com.fouwaz.studypal.data.local.dao.ProjectDao
import com.fouwaz.studypal.data.local.dao.VoiceStreamDao
import com.fouwaz.studypal.data.local.entity.AchievementEntity
import com.fouwaz.studypal.data.local.entity.DraftEntity
import com.fouwaz.studypal.data.local.entity.LectureEntity
import com.fouwaz.studypal.data.local.entity.LectureOutputEntity
import com.fouwaz.studypal.data.local.entity.ProjectEntity
import com.fouwaz.studypal.data.local.entity.VoiceStreamEntity

@Database(
    entities = [
        ProjectEntity::class,
        VoiceStreamEntity::class,
        DraftEntity::class,
        AchievementEntity::class,
        LectureEntity::class,
        LectureOutputEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class VoiceStreamDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun voiceStreamDao(): VoiceStreamDao
    abstract fun draftDao(): DraftDao
    abstract fun achievementDao(): AchievementDao
    abstract fun lectureDao(): LectureDao
    abstract fun lectureOutputDao(): LectureOutputDao

    companion object {
        @Volatile
        private var INSTANCE: VoiceStreamDatabase? = null

        fun getDatabase(context: Context): VoiceStreamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceStreamDatabase::class.java,
                    "voicestream_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
