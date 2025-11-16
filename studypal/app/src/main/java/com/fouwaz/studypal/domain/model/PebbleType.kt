package com.fouwaz.studypal.domain.model

import androidx.compose.ui.graphics.Color

data class PebbleType(
    val id: String,
    val name: String,
    val description: String,
    val wordMilestone: Int,
    val color: Color,
    val rarity: PebbleRarity
)

enum class PebbleRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
}

object PebbleTypes {
    val SMOOTH_GRAY = PebbleType(
        id = "gray_smooth",
        name = "Smooth Gray Pebble",
        description = "Every journey begins with a single stone",
        wordMilestone = 1000,
        color = Color(0xFF9E9E9E),
        rarity = PebbleRarity.COMMON
    )

    val CREAM_SPECKLED = PebbleType(
        id = "cream_speckled",
        name = "Cream Speckled Pebble",
        description = "Patience reveals hidden beauty",
        wordMilestone = 2500,
        color = Color(0xFFE9DED9),
        rarity = PebbleRarity.COMMON
    )

    val ROSE_QUARTZ = PebbleType(
        id = "rose_quartz",
        name = "Rose Quartz Pebble",
        description = "Warmth earned through dedication",
        wordMilestone = 5000,
        color = Color(0xFFF4C2C2),
        rarity = PebbleRarity.UNCOMMON
    )

    val AMBER = PebbleType(
        id = "amber",
        name = "Amber Pebble",
        description = "Polished by thousands of words",
        wordMilestone = 10000,
        color = Color(0xFFFFBF00),
        rarity = PebbleRarity.RARE
    )

    val JADE = PebbleType(
        id = "jade",
        name = "Jade Pebble",
        description = "A treasure of perseverance",
        wordMilestone = 25000,
        color = Color(0xFF00A86B),
        rarity = PebbleRarity.EPIC
    )

    val OBSIDIAN = PebbleType(
        id = "obsidian",
        name = "Obsidian Pebble",
        description = "Forged in the fires of commitment",
        wordMilestone = 50000,
        color = Color(0xFF0B1215),
        rarity = PebbleRarity.EPIC
    )

    val MOTHER_OF_PEARL = PebbleType(
        id = "mother_of_pearl",
        name = "Mother of Pearl",
        description = "A rare treasure, hard-won and luminous",
        wordMilestone = 100000,
        color = Color(0xFFFFF0E6),
        rarity = PebbleRarity.LEGENDARY
    )

    val ALL_PEBBLES = listOf(
        SMOOTH_GRAY,
        CREAM_SPECKLED,
        ROSE_QUARTZ,
        AMBER,
        JADE,
        OBSIDIAN,
        MOTHER_OF_PEARL
    )

    fun getPebbleByMilestone(wordCount: Int): PebbleType? {
        return ALL_PEBBLES.lastOrNull { it.wordMilestone <= wordCount }
    }

    fun getPebbleById(id: String): PebbleType? {
        return ALL_PEBBLES.find { it.id == id }
    }

    fun getNextMilestone(currentWords: Int): PebbleType? {
        return ALL_PEBBLES.firstOrNull { it.wordMilestone > currentWords }
    }
}
