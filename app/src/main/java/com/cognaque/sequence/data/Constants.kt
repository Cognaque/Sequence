package com.cognaque.sequence.data

import androidx.compose.ui.graphics.Color

object AppStrings {
    const val PRIORITY_TITLE = "PRIORITY"
    const val FOCUS_STACK = "FOCUS STACK"
    const val DUMP_PLACEHOLDER = "Dump task..."
    const val DELETE_ALL_TITLE = "Delete Everything?"
    const val DELETE_ALL_BODY = "This will wipe all tasks and memory. Cannot be undone."
    const val DELETE_CONFIRM = "DELETE ALL"
    const val EXPORT_READY = "Data Export Ready"
    const val EXPORT_BODY = "Your data has been converted to JSON. Copy it to your clipboard."
    const val IMPORT_TITLE = "Import Data"
    const val IMPORT_BODY = "Paste JSON data to restore your brain."
    const val DAILY_CHORE_PROTOCOL = "DAILY CHORE"
    const val DATA_SOVEREIGNTY = "MANAGE DATA"
    const val NO_THREATS = "I am a brain Watson, the rest of me is a mere appendix"
    const val SLEEP_MODE = "🌙 Sleep Mode Active"
}

object AppColors {
    val Primary = Color(0xFF81C784)
    val Secondary = Color(0xFF64B5F6)
    val Tertiary = Color(0xFFF48FB1)
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1E1E1E)
    val Error = Color(0xFFEF5350)
    val AddAction = Color(0xFFFFD54F)
    val SurfaceVariant = Color(0xFF2C2F33)
    val TextPrimary = Color(0xFFEEEEEE)
    val TextSecondary = Color(0xFFB0B0B0)
}

object AppConstants {
    const val MAX_INPUT_LENGTH = 500
    const val DB_VERSION = 13
    const val FLOAT_TOLERANCE = 0.0001f
    const val MAX_KEYWORD_STORAGE = 2000
    const val INTENT_MAX_STR_LEN = 1000
    const val PRIORITY_LIST_LIMIT = 7
}
