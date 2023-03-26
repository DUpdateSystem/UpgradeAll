package net.xzos.upgradeall.core.websdk.api.client_proxy

import net.xzos.upgradeall.core.utils.data_cache.cache_object.BytesEncoder
import org.dom4j.Document
import org.dom4j.DocumentException
import org.dom4j.io.SAXReader

object XmlEncoder : BytesEncoder<Document> {
    override fun encode(obj: Document?): ByteArray =
        obj?.asXML()?.toByteArray() ?: byteArrayOf()

    override fun decode(data: ByteArray): Document? = try {
        SAXReader().read(data.inputStream())
    } catch (e: DocumentException) {
        null
    }
}

object StringEncoder : BytesEncoder<String> {
    override fun encode(obj: String?) = obj?.toByteArray()!!
    override fun decode(data: ByteArray) = data.toString()
}