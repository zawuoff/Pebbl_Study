package com.fouwaz.studypal.domain.model

data class FinalDraftConfig(
    val wordGoal: Int = 500,
    val tone: DraftTone = DraftTone.ACADEMIC,
    val refinementLevel: RefinementLevel = RefinementLevel.MODERATE,
    val includeSummary: Boolean = false,
    val includeHighlights: Boolean = false
)

enum class DraftTone(val displayName: String, val promptDescription: String) {
    NEUTRAL("Neutral", "neutral, balanced tone"),
    ACADEMIC("Academic", "formal academic tone with scholarly language"),
    CONVERSATIONAL("Conversational", "conversational, approachable tone")
}

enum class RefinementLevel(val displayName: String, val promptDescription: String) {
    LIGHT_POLISH("Light Polish", "minimal editing—fix only grammar and basic flow"),
    MODERATE("Moderate", "moderate refinement—improve clarity and structure"),
    STRUCTURED("Structured", "thorough restructuring—create well-organized paragraphs with strong transitions")
}
