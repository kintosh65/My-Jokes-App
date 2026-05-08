package com.kintosh.myjokesapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintosh.myjokesapp.network.LiveJokeResponse
import com.kintosh.myjokesapp.network.RetrofitInstance
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LiveJokeViewModel : ViewModel() {

    private val _joke = MutableStateFlow<LiveJokeResponse?>(null)
    val joke: StateFlow<LiveJokeResponse?> = _joke

    private val refreshIntervalMs = TimeUnit.SECONDS.toMillis(30)
    private var isUpdating = false
    private var currentCategory: String = "Any"

    init {
        startJokeUpdates()
    }

    private fun startJokeUpdates() {
        viewModelScope.launch {
            while (true) {
                fetchJokeInternal(currentCategory)
                delay(refreshIntervalMs)
            }
        }
    }

    fun fetchJoke(category: String = "Any") {
        currentCategory = mapToApiCategory(category)
        viewModelScope.launch {
            fetchJokeInternal(currentCategory)
        }
    }

    private fun mapToApiCategory(userCategory: String): String {
        return when (userCategory) {
            "Puns" -> "Pun"
            "Dark humor" -> "Dark"
            "All" -> "Any"
            else -> "Any" // Default to Any if no perfect mapping
        }
    }

    private suspend fun fetchJokeInternal(category: String) {
        if (isUpdating) {
            return
        }
        isUpdating = true
        try {
            val response = RetrofitInstance.api.getRandomJoke(category)
            _joke.value = response.copy(timestamp = System.currentTimeMillis())
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isUpdating = false
        }
    }
}
