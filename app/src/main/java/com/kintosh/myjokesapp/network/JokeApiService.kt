package com.kintosh.myjokesapp.network

import retrofit2.http.GET
import retrofit2.http.Path

interface JokeApiService {

    @GET("joke/{category}?type=twopart")
    suspend fun getRandomJoke(
        @Path("category") category: String = "Any"
    ): LiveJokeResponse
}
