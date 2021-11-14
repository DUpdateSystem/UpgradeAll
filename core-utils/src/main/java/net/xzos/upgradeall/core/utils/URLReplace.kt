package net.xzos.upgradeall.core.utils

data class URLReplace(
    val search: String?,
    val replace: String?,
)

class URLReplaceUtil(private val urlReplace: URLReplace) {
    fun replaceURL(url: String): String {
        val replaced = urlReplace.search?.toRegex()?.let {
            url.replace(it, urlReplace.replace ?: "")
        } ?: urlReplace.replace ?: url
        return handleReplaceString(url, replaced)
    }

    private fun handleReplaceString(url: String, replaceString: String): String {
        return replaceString.replace(DOWNLOAD_URL, url)
    }

    companion object {
        private const val DOWNLOAD_URL = "{DOWNLOAD_URL}"
    }
}