package net.xzos.upgradeall.core.websdk.api.client_proxy

import net.xzos.upgradeall.core.utils.data_cache.cache_object.Encoder
import org.dom4j.Document
import org.dom4j.DocumentException
import org.dom4j.io.SAXReader

object XmlEncoder : Encoder<Document> {
    override fun encode(value: Document?): ByteArray =
        value?.asXML()?.toByteArray() ?: byteArrayOf()

    override fun decode(bytes: ByteArray): Document? = try {
        SAXReader().read(bytes.inputStream())
    } catch (e: DocumentException) {
        null
    }
}