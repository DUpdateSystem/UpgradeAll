package net.xzos.upgradeall.core.websdk.api.client_proxy

import net.xzos.upgradeall.core.utils.constant.VERSION_CODE
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun String.mdToHtml(): String {
    val flavour = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(this)
    return HtmlGenerator(this, parsedTree, flavour).generateHtml()
}

fun String.tryGetTimestamp(): Long {
    // Use SimpleDateFormat for API < 26 compatibility
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return try {
        format.parse(this)?.time?.div(1000) ?: 0L
    } catch (e: Exception) {
        // Try with milliseconds format
        val formatWithMs = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        try {
            formatWithMs.parse(this)?.time?.div(1000) ?: 0L
        } catch (e2: Exception) {
            0L
        }
    }
}

fun net.xzos.upgradeall.websdk.data.json.ReleaseGson.versionCode(value: Number?) = value?.let {
    this.copy(extra = mapOf(VERSION_CODE to it))
} ?: this

fun getFullUrl(host: String, patchPath: String, path: String): String {
    val url = URI(path)
    if (url.host != null) return path
    return URI(host).resolve("$patchPath/$path").toString()
}