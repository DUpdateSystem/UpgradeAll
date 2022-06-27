package net.xzos.upgradeall.core.websdk.api.client_proxy

import net.xzos.upgradeall.core.websdk.json.Assets
import org.dom4j.DocumentException
import org.dom4j.io.SAXReader
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.net.URI

fun String.mdToHtml(): String {
    val flavour = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(this)
    val a = HtmlGenerator(this, parsedTree, flavour).generateHtml()
    return a
}

fun String.getAssets(host: String, path: String): List<Assets> {
    val root = try {
        SAXReader().read(this.byteInputStream()).rootElement
    } catch (e: DocumentException) {
        return emptyList()
    }
    return root.selectNodes("//a").map {
        Assets(
            fileName = it.text,
            fileType = null,
            downloadUrl = getFullUrl(host, path, it.valueOf("./@href"))
        )
    }
}

fun getFullUrl(host: String, patchPath: String, path: String): String {
    val url = URI(path)
    if (url.host != null) return path
    return URI(host).resolve("$patchPath/$path").toString()
}