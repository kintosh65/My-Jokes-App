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

    private val refreshIntervalMs = TimeUnit.SECONDS.toMillis(2) // Refresh every 10 seconds
    private var isUpdating = false

    init {
        startJokeUpdates()
    }

    private fun startJokeUpdates() {
        viewModelScope.launch {
            while (true) {
                fetchJokeInternal()
                delay(refreshIntervalMs)
            }
        }
    }

    fun fetchJoke() {
        viewModelScope.launch {
            fetchJokeInternal()
        }
    }

    private suspend fun fetchJokeInternal() {
        if (isUpdating) {
            return // Prevent concurrent updates
        }
        isUpdating = true
        try {
            val response = RetrofitInstance.api.getRandomJoke()
            _joke.value = response.copy(timestamp = System.currentTimeMillis())
        } catch (e: Exception) {
            e.printStackTrace()
            // Consider emitting an error state to the UI if needed
        } finally {
            isUpdating = false
        }
    }

    override fun onCleared() {
        // Any cleanup if needed
        super.onCleared()
    }
}