package com.cristiancogollo.biblion.feature.reader

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

const val READER_MIN_LINE_SPACING = 1.15f
const val READER_MAX_LINE_SPACING = 2.0f
const val READER_DEFAULT_LINE_SPACING = 1.5f

const val READER_MIN_TEXT_WIDTH_DP = 320f
const val READER_MAX_TEXT_WIDTH_DP = 960f
const val READER_DEFAULT_TEXT_WIDTH_DP = 720f

enum class ReaderFontFamily(val storageValue: String) {
    SERIF("serif"),
    SANS_SERIF("sans_serif"),
    ACCESSIBLE("accessible");

    fun asComposeFontFamily(): FontFamily = when (this) {
        SERIF -> FontFamily.Serif
        SANS_SERIF -> FontFamily.SansSerif
        ACCESSIBLE -> FontFamily.Monospace
    }

    fun bodyWeight(): FontWeight = when (this) {
        ACCESSIBLE -> FontWeight.Medium
        SERIF,
        SANS_SERIF -> FontWeight.Normal
    }

    companion object {
        fun fromStorage(value: String?): ReaderFontFamily =
            entries.firstOrNull { it.storageValue == value } ?: SERIF
    }
}

enum class ReaderTextLayout(val storageValue: String) {
    VERSE_BLOCKS("verse_blocks"),
    FLOWING("flowing");

    companion object {
        fun fromStorage(value: String?): ReaderTextLayout =
            entries.firstOrNull { it.storageValue == value } ?: VERSE_BLOCKS
    }
}

@Immutable
data class ReaderPreferences(
    val fontFamily: ReaderFontFamily = ReaderFontFamily.SERIF,
    val lineSpacingMultiplier: Float = READER_DEFAULT_LINE_SPACING,
    val textWidthDp: Float = READER_DEFAULT_TEXT_WIDTH_DP,
    val textLayout: ReaderTextLayout = ReaderTextLayout.VERSE_BLOCKS,
    val continuousScrolling: Boolean = false,
    val showSectionHeadings: Boolean = true,
    val showVerseNumbers: Boolean = true,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val secondaryVersionKey: String = "",
    val secondaryBookName: String = "Salmos",
    val secondaryChapter: Int = 1,
) {
    fun normalized(): ReaderPreferences = copy(
        // Lectura fluida queda desactivada hasta contar con cortes de párrafo fiables.
        textLayout = ReaderTextLayout.VERSE_BLOCKS,
        lineSpacingMultiplier = lineSpacingMultiplier.coerceIn(
            READER_MIN_LINE_SPACING,
            READER_MAX_LINE_SPACING,
        ),
        textWidthDp = textWidthDp.coerceIn(
            READER_MIN_TEXT_WIDTH_DP,
            READER_MAX_TEXT_WIDTH_DP,
        ),
        secondaryBookName = secondaryBookName.ifBlank { "Salmos" },
        secondaryChapter = secondaryChapter.coerceAtLeast(1),
    )
}

object ReaderPreferencesStore {
    private const val PREFS_NAME = "reader_display_preferences"
    private const val KEY_FONT_FAMILY = "font_family"
    private const val KEY_LINE_SPACING_MULTIPLIER = "line_spacing_multiplier"
    private const val KEY_TEXT_WIDTH_DP = "text_width_dp"
    private const val KEY_TEXT_LAYOUT = "text_layout"
    private const val KEY_CONTINUOUS_SCROLLING = "continuous_scrolling"
    private const val KEY_SHOW_SECTION_HEADINGS = "show_section_headings"
    private const val KEY_SHOW_VERSE_NUMBERS = "show_verse_numbers"
    private const val KEY_HIGH_CONTRAST = "high_contrast"
    private const val KEY_REDUCE_MOTION = "reduce_motion"
    private const val KEY_SECONDARY_VERSION = "secondary_version"
    private const val KEY_SECONDARY_BOOK = "secondary_book"
    private const val KEY_SECONDARY_CHAPTER = "secondary_chapter"

    // Claves de la primera implementacion. Se leen una vez para conservar preferencias existentes.
    private const val LEGACY_KEY_LINE_SPACING = "line_spacing"
    private const val LEGACY_KEY_TEXT_WIDTH = "text_width"
    private const val LEGACY_KEY_PARALLEL_READING = "parallel_reading"

    fun load(context: Context): ReaderPreferences {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lineSpacing = if (prefs.contains(KEY_LINE_SPACING_MULTIPLIER)) {
            prefs.getFloat(KEY_LINE_SPACING_MULTIPLIER, READER_DEFAULT_LINE_SPACING)
        } else {
            when (prefs.getString(LEGACY_KEY_LINE_SPACING, null)) {
                "compact" -> 1.3f
                "airy" -> 1.8f
                else -> READER_DEFAULT_LINE_SPACING
            }
        }
        val textWidth = if (prefs.contains(KEY_TEXT_WIDTH_DP)) {
            prefs.getFloat(KEY_TEXT_WIDTH_DP, READER_DEFAULT_TEXT_WIDTH_DP)
        } else {
            when (prefs.getString(LEGACY_KEY_TEXT_WIDTH, null)) {
                "wide" -> 840f
                "focused" -> 600f
                else -> READER_DEFAULT_TEXT_WIDTH_DP
            }
        }

        return ReaderPreferences(
            fontFamily = ReaderFontFamily.fromStorage(prefs.getString(KEY_FONT_FAMILY, null)),
            lineSpacingMultiplier = lineSpacing,
            textWidthDp = textWidth,
            textLayout = ReaderTextLayout.fromStorage(prefs.getString(KEY_TEXT_LAYOUT, null)),
            continuousScrolling = prefs.getBoolean(KEY_CONTINUOUS_SCROLLING, false),
            showSectionHeadings = prefs.getBoolean(KEY_SHOW_SECTION_HEADINGS, true),
            showVerseNumbers = prefs.getBoolean(KEY_SHOW_VERSE_NUMBERS, true),
            highContrast = prefs.getBoolean(KEY_HIGH_CONTRAST, false),
            reduceMotion = prefs.getBoolean(KEY_REDUCE_MOTION, false),
            secondaryVersionKey = prefs.getString(KEY_SECONDARY_VERSION, "").orEmpty(),
            secondaryBookName = prefs.getString(KEY_SECONDARY_BOOK, "Salmos").orEmpty(),
            secondaryChapter = prefs.getInt(KEY_SECONDARY_CHAPTER, 1),
        ).normalized()
    }

    fun save(context: Context, preferences: ReaderPreferences) {
        val normalized = preferences.normalized()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FONT_FAMILY, normalized.fontFamily.storageValue)
            .putFloat(KEY_LINE_SPACING_MULTIPLIER, normalized.lineSpacingMultiplier)
            .putFloat(KEY_TEXT_WIDTH_DP, normalized.textWidthDp)
            .putString(KEY_TEXT_LAYOUT, normalized.textLayout.storageValue)
            .putBoolean(KEY_CONTINUOUS_SCROLLING, normalized.continuousScrolling)
            .putBoolean(KEY_SHOW_SECTION_HEADINGS, normalized.showSectionHeadings)
            .putBoolean(KEY_SHOW_VERSE_NUMBERS, normalized.showVerseNumbers)
            .putBoolean(KEY_HIGH_CONTRAST, normalized.highContrast)
            .putBoolean(KEY_REDUCE_MOTION, normalized.reduceMotion)
            .putString(KEY_SECONDARY_VERSION, normalized.secondaryVersionKey)
            .putString(KEY_SECONDARY_BOOK, normalized.secondaryBookName)
            .putInt(KEY_SECONDARY_CHAPTER, normalized.secondaryChapter)
            .remove(LEGACY_KEY_LINE_SPACING)
            .remove(LEGACY_KEY_TEXT_WIDTH)
            .remove(LEGACY_KEY_PARALLEL_READING)
            .apply()
    }
}
