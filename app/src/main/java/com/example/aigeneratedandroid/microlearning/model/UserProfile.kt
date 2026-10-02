package com.example.aigeneratedandroid.microlearning.model

/**
 * Answers from the onboarding quiz. Drives the offline curation of the bundled library; the
 * daily studio episodes are written from the matching pipeline/profile.json.
 */
data class UserProfile(
    val followedAuthors: List<String>,
    val topics: List<String>,
    /** Segment.style values the listener prefers, e.g. "story", "counterintuitive". */
    val preferredStyles: List<String>,
    val sessionMinutes: Int,
    val chunkMinutes: Int
) {
    val segmentsPerSession: Int
        get() = (sessionMinutes / chunkMinutes).coerceIn(3, 10)

    companion object {
        fun default(): UserProfile = UserProfile(
            followedAuthors = listOf(
                "Kerry Patterson",
                "Gene Kim",
                "Stephen R. Covey",
                "Warren Berger",
                "John C. Maxwell"
            ),
            topics = listOf(
                "Business & Entrepreneurship",
                "Technology & AI",
                "Leadership",
                "Creativity",
                "Systems & Measurement"
            ),
            preferredStyles = listOf("story", "counterintuitive"),
            sessionMinutes = 10,
            chunkMinutes = 2
        )
    }
}
