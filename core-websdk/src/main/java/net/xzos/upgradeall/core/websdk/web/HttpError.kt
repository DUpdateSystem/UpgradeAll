package net.xzos.upgradeall.core.websdk.web

import okhttp3.Call

data class HttpError(
    val error: Throwable,
    val call: Call? = null,
)