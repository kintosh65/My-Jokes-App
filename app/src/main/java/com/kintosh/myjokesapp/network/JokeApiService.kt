package com.kintosh.myjokesapp.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JokeApiService {

    @GET("joke/{category}?type=twopart")
    suspend fun getRandomJoke(
        @Path("category") category: String = "Any",
        @Query("contains") query: String? = null,
        @Query("amount") amount: Int = 1
    ): LiveJokeResponse

    @GET("joke/Any?type=twopart")
    suspend fun searchJokes(
        @Query("contains") query: String,
        @Query("amount") amount: Int = 10,
        @Query("lang") lang: String = "en"
    ): MultiJokeResponse
}

data class MultiJokeResponse(
    val error: Boolean,
    val amount: Int,
    val jokes: List<LiveJokeResponse>? = null
)
