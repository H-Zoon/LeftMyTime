package com.devidea.timeleft.ui.components

import androidx.annotation.StringRes
import com.devidea.timeleft.R
import kotlin.math.abs
import kotlin.math.ln

internal data class TimeStory(
    val id: String,
    val durationSeconds: Double,
    @param:StringRes val durationRes: Int,
    @param:StringRes val textRes: Int,
    @param:StringRes val sourceNameRes: Int,
    val sourceUrl: String,
)

private const val SecondsPerDay = 86_400.0
private const val MaxDurationRatio = 2.0

// Reviewed sources and the scope of each duration: design/TIME_STORIES.md.
// Learning routines and stories of starting, practicing and revising; never promises of results.
private val stories = listOf(
    TimeStory("wright-first", 12.0, R.string.time_story_wright_first_duration, R.string.time_story_wright_first,
        R.string.time_story_source_nps, "https://www.nps.gov/wrbr/learn/historyculture/thefirstflight.htm"),
    TimeStory("wright-fourth", 59.0, R.string.time_story_wright_fourth_duration, R.string.time_story_wright_fourth,
        R.string.time_story_source_nps, "https://www.nps.gov/wrbr/learn/historyculture/thefirstflight.htm"),
    TimeStory("two-minute-start", 120.0, R.string.time_story_two_minute_duration, R.string.time_story_two_minute,
        R.string.time_story_source_james_clear, "https://jamesclear.com/how-to-stop-procrastinating"),
    // The midpoint is only for matching; the source describes a 10–20 minute planning routine.
    TimeStory("newport-planning", 15 * 60.0, R.string.time_story_planning_duration, R.string.time_story_planning,
        R.string.time_story_source_cal_newport, "https://calnewport.com/deep-habits-the-importance-of-planning-every-minute-of-your-work-day/"),
    TimeStory("pomodoro-focus", 25 * 60.0, R.string.time_story_pomodoro_duration, R.string.time_story_pomodoro,
        R.string.time_story_source_pomodoro, "https://www.pomodorotechnique.com/what-is-the-pomodoro-technique/"),
    // One study cycle includes both 50 minutes of work and a 10-minute break.
    TimeStory("study-and-rest", 3_600.0, R.string.time_story_study_rest_duration, R.string.time_story_study_rest,
        R.string.time_story_source_cal_newport, "https://calnewport.com/on-deep-breaks/"),
    // The midpoint represents the writer's reported 5–6 hours, not a target for users.
    TimeStory("murakami-routine", 5.5 * 3_600.0, R.string.time_story_murakami_duration, R.string.time_story_murakami,
        R.string.time_story_source_paris_review, "https://www.theparisreview.org/blog/2021/08/03/redux-the-runner-trying-to-disappear/"),
    TimeStory("beatles-session", SecondsPerDay, R.string.time_story_beatles_duration, R.string.time_story_beatles,
        R.string.time_story_source_beatles, "https://www.thebeatles.com/please-please-me"),
    TimeStory("rocky-draft", 3.5 * SecondsPerDay, R.string.time_story_rocky_duration, R.string.time_story_rocky,
        R.string.time_story_source_afi, "https://catalog.afi.com/Film/53862-ROCKY?cxt=topsearches"),
    TimeStory("handel-messiah", 24 * SecondsPerDay, R.string.time_story_handel_duration, R.string.time_story_handel,
        R.string.time_story_source_symphony, "https://saskatoonsymphony.org/handels-messiah-premiere/"),
    TimeStory("picasso-guernica", 34 * SecondsPerDay, R.string.time_story_picasso_duration, R.string.time_story_picasso,
        R.string.time_story_source_reina_sofia, "https://guernica.museoreinasofia.es/en/document/dora-maars-report-guernicas-progression"),
    TimeStory("dickens-carol", 42 * SecondsPerDay, R.string.time_story_dickens_duration, R.string.time_story_dickens,
        R.string.time_story_source_dickens, "https://dickensmuseum.com/blogs/news/the-lost-portrait"),
    // Approximate comparison only; this does not define calendar month duration.
    TimeStory("minecraft-pocket", 90 * SecondsPerDay, R.string.time_story_minecraft_duration, R.string.time_story_minecraft,
        R.string.time_story_source_minecraft, "https://www.minecraft.net/en-us/article/pocket-edition-5-years-old"),
    TimeStory("empire-state", 410 * SecondsPerDay, R.string.time_story_empire_duration, R.string.time_story_empire,
        R.string.time_story_source_empire_state, "https://www.esbnyc.com/about/history"),
)

// Retired trivia stays available only to readers who already saved it. Never reuse these IDs.
private val archivedStories = listOf(
    TimeStory("park-cube", 3.13, R.string.time_story_park_duration, R.string.time_story_park,
        R.string.time_story_source_guinness, "https://www.guinnessworldrecords.com/news/2023/6/max-park-makes-history-by-solving-cube-in-fastest-time-ever-752905"),
    TimeStory("bannister-mile", 239.4, R.string.time_story_bannister_duration, R.string.time_story_bannister,
        R.string.time_story_source_world_athletics, "https://worldathletics.org/news/news/3594"),
    TimeStory("cage-silence", 273.0, R.string.time_story_cage_duration, R.string.time_story_cage,
        R.string.time_story_source_moma, "https://www.moma.org/collection/works/163616"),
    TimeStory("versailles-balloon", 8 * 60.0, R.string.time_story_balloon_duration, R.string.time_story_balloon,
        R.string.time_story_source_versailles, "https://en.chateauversailles.fr/discover/history/key-dates/first-hot-air-balloon-flight"),
    TimeStory("shepard-flight", 922.0, R.string.time_story_shepard_duration, R.string.time_story_shepard,
        R.string.time_story_source_nasa, "https://www.nasa.gov/history/may-5-1961-alan-shepard-becomes-the-first-american-in-space/"),
    TimeStory("queen-live-aid", 21 * 60.0, R.string.time_story_queen_duration, R.string.time_story_queen,
        R.string.time_story_source_queen, "https://www.queenonline.com/news/watch-queen-the-greatest-live-aid-episode-30"),
    TimeStory("white-spacewalk", 23 * 60.0, R.string.time_story_white_duration, R.string.time_story_white,
        R.string.time_story_source_nasa, "https://apod.nasa.gov/apod/ap050604.html"),
    TimeStory("ganna-hour", 3_600.0, R.string.time_story_ganna_duration, R.string.time_story_ganna,
        R.string.time_story_source_uci, "https://www.uci.org/pressrelease/filippo-ganna-breaks-the-uci-hour-record-timed-by-tissot/82aysypXuU0sdkVD69YVI"),
    TimeStory("gagarin-flight", 108 * 60.0, R.string.time_story_gagarin_duration, R.string.time_story_gagarin,
        R.string.time_story_source_nasa, "https://science.nasa.gov/resource/yuri-gagarin-first-human-in-space/"),
    TimeStory("honnold-el-capitan", (3 * 60 + 56) * 60.0, R.string.time_story_honnold_duration, R.string.time_story_honnold,
        R.string.time_story_source_national_geographic, "https://www.nationalgeographic.com/magazine/2019/02/alex-honnold-made-ultimate-climb-el-capitan-without-rope/"),
    TimeStory("isner-mahut", (11 * 60 + 5) * 60.0, R.string.time_story_tennis_duration, R.string.time_story_tennis,
        R.string.time_story_source_guinness, "https://www.guinnessworldrecords.com/world-records/94409-longest-tennis-wimbledon-match-singles"),
    TimeStory("lindbergh-flight", (33 * 60 + 30) * 60.0, R.string.time_story_lindbergh_duration, R.string.time_story_lindbergh,
        R.string.time_story_source_smithsonian, "https://www.si.edu/newsdesk/releases/national-air-and-space-museum-lowers-spirit-st-louis-ground-level"),
    TimeStory("mozart-linz", 4 * SecondsPerDay, R.string.time_story_mozart_duration, R.string.time_story_mozart,
        R.string.time_story_source_sydney_symphony, "https://files.baskercdn.com/sydneysymphony/files/3316_sso_programs2023_17mozartsgreatmassincminor_art_web_singles.pdf"),
    TimeStory("gemini-four", 4 * SecondsPerDay, R.string.time_story_gemini_duration, R.string.time_story_gemini,
        R.string.time_story_source_nasa, "https://www.nasa.gov/mission/gemini-iv/"),
    TimeStory("apollo-eleven", 195 * 3_600.0 + 18 * 60 + 35, R.string.time_story_apollo_duration, R.string.time_story_apollo,
        R.string.time_story_source_nasa, "https://www.nasa.gov/history/apollo-11-mission-overview/"),
    TimeStory("bly-journey", 72 * SecondsPerDay, R.string.time_story_bly_duration, R.string.time_story_bly,
        R.string.time_story_source_nps, "https://home.nps.gov/people/nellie-bly.htm"),
    TimeStory("perseverance-journey", 203 * SecondsPerDay, R.string.time_story_perseverance_duration, R.string.time_story_perseverance,
        R.string.time_story_source_nasa, "https://www.nasa.gov/news-release/touchdown-nasas-mars-perseverance-rover-safely-lands-on-red-planet/"),
)

/** Rank by relative closeness; omit unrelated durations instead of stretching a fact to fit. */
internal fun timeStories(remaining: Long, inDays: Boolean): List<TimeStory> {
    if (remaining <= 0) return emptyList()
    // Calendar days are only a comparison scale, not a conversion to available working hours.
    val referenceSeconds = remaining.toDouble() * if (inDays) SecondsPerDay else 1.0
    return stories.filter { it.durationSeconds / referenceSeconds in (1.0 / MaxDurationRatio)..MaxDurationRatio }
        .sortedBy { abs(ln(it.durationSeconds / referenceSeconds)) }
}

internal fun allTimeStories(includeArchived: Boolean = false): List<TimeStory> =
    if (includeArchived) stories + archivedStories else stories
