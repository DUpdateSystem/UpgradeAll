package net.xzos.upgradeall.core.utils

import java.net.URL

data class URLReplaceData(
    val search: String?,
    val replace: String?,
)

class URLReplace(private val urlReplaceData: URLReplaceData) {
    fun replaceURL(url: String): String {
        val replaceString = urlReplaceData.replace?.let { handleReplaceString(it, url) }
        val replaceURL = replaceString?.let { URL(it) }
        val search = urlReplaceData.search
        return when {
            search == null && replaceString == null -> return url
            search != null -> url.replace(search.toRegex(), replaceString ?: "")
            replaceURL != null && replaceURL.path?.isEmpty() == true -> changeURLHost(
                url, replaceURL.host
            )
            else -> replaceString ?: url
        }
    }

    private fun changeURLHost(url: String, host: String) =
        url.replace("://\\S*?/", "://$host/")

    private fun handleReplaceString(replaceString: String, url: String): String {
        return replaceString.replace(DOWNLOAD_URL, url)
    }

    companion object {
        private const val DOWNLOAD_URL = "{DOWNLOAD_URL}"
    }
}