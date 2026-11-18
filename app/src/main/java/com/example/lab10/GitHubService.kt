package com.example.lab10

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubService {

    // per_page = 5, chunki GitHub bir soatda 60 ta so'rovga ruxsat beradi
    @GET("/users")
    suspend fun users(@Query("per_page") perPage: Int): List<User>

    @GET("/users/{login}")
    suspend fun user(@Path("login") login: String): User
}
