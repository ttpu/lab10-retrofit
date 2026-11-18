package com.example.lab10

import retrofit2.HttpException
import java.io.IOException

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class HttpError(val code: Int) : ApiResult<Nothing>
    data class NetworkError(val message: String) : ApiResult<Nothing>
}

// har bir chaqiruvni shu funksiya ichida yozamiz, shunda app crash bo'lmaydi
suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(call())
    } catch (e: HttpException) {
        ApiResult.HttpError(e.code())
    } catch (e: IOException) {
        ApiResult.NetworkError(e.message ?: "no network")
    }
}
