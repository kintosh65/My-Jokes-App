package com.kintosh.myjokesapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintosh.myjokesapp.network.LiveJokeResponse
import com.kintosh.myjokesapp.network.RetrofitInstance
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LiveJokeViewModel : ViewModel() {

    private val _joke = MutableStateFlow<LiveJokeResponse?>(null)
    val joke: StateFlow<LiveJokeResponse?> = _joke

    private val _searchResults = MutableStateFlow<List<LiveJokeResponse>>(emptyList())
    val searchResults: StateFlow<List<LiveJokeResponse>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val refreshIntervalMs = TimeUnit.SECONDS.toMillis(30)
    private var isUpdating = false
    private var currentCategory: String = "Any"
    private var searchJob: Job? = null

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

    fun searchRemoteJokes(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            _isSearching.value = true
            try {
                val response = RetrofitInstance.api.searchJokes(query, lang = "en")
                val swResponse = RetrofitInstance.api.searchJokes(query, lang = "sw")
                
                val combinedJokes = (response.jokes ?: emptyList()) + (swResponse.jokes ?: emptyList())
                _searchResults.value = combinedJokes.distinctBy { it.setup }
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    private fun mapToApiCategory(userCategory: String): String {
        return when (userCategory) {
            "Puns" -> "Pun"
            "Dark humor" -> "Dark"
            "All" -> "Any"
            "Programming" -> "Programming"
            "Misc" -> "Misc"
            "Spooky" -> "Spooky"
            "Christmas" -> "Christmas"
            else -> "Any"
        }
    }

    private suspend fun fetchJokeInternal(category: String) {
        if (isUpdating) return
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
