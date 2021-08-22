package net.xzos.upgradeall.core.utils

import java.io.StringReader
import java.security.MessageDigest
import java.util.*

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    md.update(this.toByteArray())
    return md.digest().toString(Charsets.UTF_8)
}

fun String.parseProperties(): Properties {
    return Properties().apply {
        this.load(StringReader(this@parseProperties))
    }
}