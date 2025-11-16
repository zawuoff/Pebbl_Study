package com.fouwaz.studypal.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fouwaz.studypal.domain.model.DraftTone
import com.fouwaz.studypal.domain.model.FinalDraftConfig
import com.fouwaz.studypal.domain.model.RefinementLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "draft_config")

class DraftConfigPreferences(private val context: Context) {

    companion object {
        private fun wordGoalKey(projectId: Long) = intPreferencesKey("word_goal_$projectId")
        private fun toneKey(projectId: Long) = stringPreferencesKey("tone_$projectId")
        private fun refinementKey(projectId: Long) = stringPreferencesKey("refinement_$projectId")
        private fun includeSummaryKey(projectId: Long) = booleanPreferencesKey("include_summary_$projectId")
        private fun includeHighlightsKey(projectId: Long) = booleanPreferencesKey("include_highlights_$projectId")
    }

    fun getConfig(projectId: Long): Flow<FinalDraftConfig> =
        context.dataStore.data.map { preferences ->
            FinalDraftConfig(
                wordGoal = preferences[wordGoalKey(projectId)] ?: 500,
                tone = preferences[toneKey(projectId)]?.let {
                    DraftTone.values().find { tone -> tone.name == it }
                } ?: DraftTone.ACADEMIC,
                refinementLevel = preferences[refinementKey(projectId)]?.let {
                    RefinementLevel.values().find { level -> level.name == it }
                } ?: RefinementLevel.MODERATE,
                includeSummary = preferences[includeSummaryKey(projectId)] ?: false,
                includeHighlights = preferences[includeHighlightsKey(projectId)] ?: false
            )
        }

    suspend fun saveConfig(projectId: Long, config: FinalDraftConfig) {
        context.dataStore.edit { preferences ->
            preferences[wordGoalKey(projectId)] = config.wordGoal
            preferences[toneKey(projectId)] = config.tone.name
            preferences[refinementKey(projectId)] = config.refinementLevel.name
            preferences[includeSummaryKey(projectId)] = config.includeSummary
            preferences[includeHighlightsKey(projectId)] = config.includeHighlights
        }
    }
}
