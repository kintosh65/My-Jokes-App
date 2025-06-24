package com.kintosh.myjokesapp.network

import retrofit2.http.GET

interface JokeApiService {

    @GET("jokes/random")
    suspend fun getRandomJoke(): LiveJokeResponse
}
