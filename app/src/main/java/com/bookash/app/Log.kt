package com.bookash.app

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Log(
    val id: String = "",
    val userId: String = "",
    val level: String, // info, warn, error
    val tag: String,
    val message: String,
    val createdAt: String? = null
) : Parcelable
