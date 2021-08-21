package net.xzos.upgradeall.core.utils

import java.io.StringReader
import java.util.*

fun String.parseProperties(): Properties {
    return Properties().apply {
        this.load(StringReader(this@parseProperties))
    }
}