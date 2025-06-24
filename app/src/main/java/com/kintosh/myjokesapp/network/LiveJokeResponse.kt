package com.kintosh.myjokesapp.network

data class LiveJokeResponse(
    val setup: String,
    val punchline: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis()
)