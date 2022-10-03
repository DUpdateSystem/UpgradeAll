package net.xzos.upgradeall.core.utils

import android.util.Base64
import java.math.BigInteger
import java.security.MessageDigest

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(this.toByteArray())).toString(16).padStart(32, '0')
}

fun String.base64(): String = this.toByteArray().base64()
fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_PADDING).replace("\n", "")
