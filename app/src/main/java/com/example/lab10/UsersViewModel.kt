package com.example.lab10

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Loading : UiState
    data class Success(val users: List<User>) : UiState
    data class Error(val message: String) : UiState
}

class UsersViewModel : ViewModel() {

    private val service = ApiInstance.createService(GitHubService::class.java)

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state

    // bosilgan userning to'liq ma'lumoti
    private val _selected = MutableStateFlow<User?>(null)
    val selected: StateFlow<User?> = _selected

    // nechta marta Retry bosilgani
    private val _tries = MutableStateFlow(0)
    val tries: StateFlow<Int> = _tries

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val result = safeApiCall { service.users(5) }
            when (result) {
                is ApiResult.Success -> {
                    _state.value = UiState.Success(result.data)
                    _tries.value = 0
                }
                is ApiResult.NetworkError -> {
                    _tries.value = _tries.value + 1
                    _state.value = UiState.Error("No network")
                    Log.d("UsersViewModel", "network error: " + result.message)
                }
                is ApiResult.HttpError -> {
                    _tries.value = _tries.value + 1
                    _state.value = UiState.Error("Server error: " + result.code)
                }
            }
        }
    }

    // ro'yxatdan login bosilganda bitta userni olib kelamiz
    fun openUser(login: String) {
        viewModelScope.launch {
            val result = safeApiCall { service.user(login) }
            if (result is ApiResult.Success) {
                _selected.value = result.data
            } else {
                _selected.value = null
                Log.d("UsersViewModel", "user error: " + result)
            }
        }
    }

    // Wi-Fi sozlamasidan qaytganda Retry qayta chiqishi uchun
    fun resetTries() {
        _tries.value = 0
    }

    // 404 ni ko'rsatish uchun debug tugma
    fun loadBadUser() {
        viewModelScope.launch {
            val result = safeApiCall { service.user("no-such-login-xyz") }
            when (result) {
                is ApiResult.Success -> _selected.value = result.data
                is ApiResult.HttpError -> {
                    _tries.value = _tries.value + 1
                    _state.value = UiState.Error("Server error: " + result.code)
                }
                is ApiResult.NetworkError -> {
                    _tries.value = _tries.value + 1
                    _state.value = UiState.Error("No network")
                }
            }
        }
    }
}
