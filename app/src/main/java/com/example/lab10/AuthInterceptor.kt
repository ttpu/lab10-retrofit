package com.example.lab10

import okhttp3.Interceptor
import okhttp3.Response

// 9-punktning qo'shimcha qismi: har bir so'rovga Authorization header qo'shadi.
// ApiInstance da uni yoqsak, Logcat da (Level.HEADERS bilan) header ko'rinadi.
class AuthInterceptor(private val token: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer " + token)
            .build()
        return chain.proceed(request)
    }
}
