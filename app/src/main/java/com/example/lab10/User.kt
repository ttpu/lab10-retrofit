package com.example.lab10

import com.google.gson.annotations.SerializedName

// JSON dagi avatar_url nomi boshqacha, shuning uchun SerializedName yozdim
data class User(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url")
    val avatarUrl: String?
)
