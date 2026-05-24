package com.devidea.timeleft.preferences

object UserPreferences {
    const val KEY_THEME = "theme"
    const val KEY_COLOR_THEME = "color_theme"
    const val KEY_HOME_SORT = "home_sort"
    const val KEY_START_SCREEN = "start_screen"
    const val KEY_EXPIRED_ITEMS = "expired_items"
    const val KEY_PROGRESS_DISPLAY = "progress_display"
    const val KEY_DEFAULT_DATE_REMINDER = "default_date_reminder"
    const val KEY_DEFAULT_TIME_REMINDER = "default_time_reminder"
    const val KEY_DATE_REMINDER_TIME = "date_reminder_time"

    const val THEME_AUTO = "auto"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    const val COLOR_THEME_INDIGO = "indigo"
    const val COLOR_THEME_EMERALD = "emerald"
    const val COLOR_THEME_ROSE = "rose"
    const val COLOR_THEME_AMBER = "amber"
    const val COLOR_THEME_SLATE = "slate"

    const val SORT_NEAREST = "Nearest"
    const val SORT_CREATED = "Created"
    const val SORT_TITLE = "Title"
    const val SORT_PROGRESS = "Progress"

    const val START_SCREEN_OVERVIEW = "Overview"
    const val START_SCREEN_ITEMS = "Items"

    const val EXPIRED_ITEMS_SHOW = "show"
    const val EXPIRED_ITEMS_BOTTOM = "bottom"
    const val EXPIRED_ITEMS_HIDE = "hide"

    const val PROGRESS_DISPLAY_FULL = "full"
    const val PROGRESS_DISPLAY_BAR_ONLY = "bar_only"
    const val PROGRESS_DISPLAY_HIDDEN = "hidden"

    const val DEFAULT_REMINDER_OFFSET = -1
    const val DEFAULT_DATE_REMINDER_TIME = "09:00"
}
