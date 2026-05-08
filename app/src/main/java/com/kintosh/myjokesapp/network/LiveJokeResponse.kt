package com.kintosh.myjokesapp.network

import com.google.gson.annotations.SerializedName

data class LiveJokeResponse(
    val category: String,
    val setup: String? = null,
    @SerializedName("delivery")
    val punchline: String? = null,
    val type: String,
    val joke: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)