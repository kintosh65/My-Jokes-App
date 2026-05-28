package com.kintosh.myjokesapp.network

import com.google.gson.annotations.SerializedName

data class LiveJokeResponse(
    val category: String = "Misc",
    val setup: String? = null,
    @SerializedName("delivery")
    val punchline: String? = null,
    val type: String? = null,
    val joke: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val safe: Boolean = true,
    val lang: String = "en",
    val flags: JokeFlags? = null
)

data class JokeFlags(
    val nsfw: Boolean = false,
    val religious: Boolean = false,
    val political: Boolean = false,
    val racist: Boolean = false,
    val sexist: Boolean = false,
    val explicit: Boolean = false
)
