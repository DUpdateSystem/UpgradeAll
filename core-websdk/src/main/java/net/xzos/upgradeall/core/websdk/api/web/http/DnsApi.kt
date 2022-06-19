package net.xzos.upgradeall.core.websdk.api.web.http

import java.net.InetAddress
import java.net.URI

class DnsApi {
    companion object {
        fun resolve(host: String): String? {
            val url = getURL(host)
            val address = InetAddress.getByName(url.host)
            return host.replace(url.host, address.hostAddress ?: return null, ignoreCase = true)
        }

        private fun getURL(rawUrl: String): URI {
            val url = if ("://" !in rawUrl)
                "null://$rawUrl"
            else rawUrl
            return URI(url)
        }
    }
}