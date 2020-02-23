package net.xzos.upgradeall.data_manager.utils

import net.xzos.upgradeall.data.config.AppConfig


class GitUrlTranslation(private val gitUrl: String) {

    fun testUrl(): Boolean {
        return getGitRawUrl("").isNotBlank()
    }

    fun getGitRawUrl(path: String): String {
        val urlTemplate = when {
            gitUrl.contains("cdn.jsdelivr.net") -> AppConfig.cdn_github_url
            gitUrl.contains("github.com") -> AppConfig.github_url
            gitUrl.contains("coding.net") -> AppConfig.coding_url
            else -> return ""
        }
        val rawUrlTemplate = when {
            gitUrl.contains("cdn.jsdelivr.net") -> AppConfig.cdn_github_raw_url
            gitUrl.contains("github.com") -> AppConfig.github_raw_url
            gitUrl.contains("coding.net") -> AppConfig.coding_raw_url
            else -> return ""
        }
        val args = listOf(AutoTemplate.Arg("%path",
                if (path.indexOf("/") == 0)
                    path.substring(1)
                else path))
        return AutoTemplate(gitUrl, urlTemplate, rawUrlTemplate).toString(args)
    }
}
