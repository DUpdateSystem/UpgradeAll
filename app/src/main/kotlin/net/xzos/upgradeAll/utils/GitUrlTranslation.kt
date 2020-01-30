package net.xzos.upgradeAll.utils

import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import java.util.*

internal class GitUrlTranslation(private val gitUrl: String) {

    fun testUrl(): Boolean {
        return getGitRawUrl("") != null
    }

    fun getGitRawUrl(path: String): String? {
        val urlTemplate = when {
            gitUrl.contains("github.com") -> MyApplication.context.getString(R.string.github_url)
            gitUrl.contains("coding.net") -> MyApplication.context.getString(R.string.coding_url)
            else -> return null
        }
        val argsMap = matchArgs(gitUrl, urlTemplate)?.apply {
            this["%path"] = if (path.indexOf("/") == 0)
                path.substring(1)
            else path
        } ?: return null
        val rawUrlTemplate = when {
            gitUrl.contains("github.com") -> MyApplication.context.getString(R.string.github_raw_url)
            gitUrl.contains("coding.net") -> MyApplication.context.getString(R.string.coding_raw_url)
            else -> return null
        }
        return fillArgs(rawUrlTemplate, argsMap)
    }

    private fun matchArgs(gitUrl: String, urlTemplate: String): HashMap<String, String>? {
        val gitUrlPre = preUrl(gitUrl) ?: return null
        val urlTemplatePre = preUrl(urlTemplate) ?: return null
        // 预处理
        val argsMap = hashMapOf<String, String>()
        for (i in urlTemplatePre.second.indices) {
            val urlTemplateFragment = urlTemplatePre.second[i]
            val keyword = getArgsKeyword(urlTemplateFragment)
            if (keyword != null) {
                val value = if (i < gitUrlPre.second.size) {
                    deleteSameString(gitUrlPre.second[i], urlTemplateFragment.split(keyword))
                } else ""
                if (value.isBlank()) {
                    if (keyword == "%branch")
                        argsMap[keyword] = "master"
                    else return null
                } else
                    argsMap[keyword] = value
            }
        }
        return argsMap
    }

    private fun fillArgs(s: String, argsMap: HashMap<String, String>): String {
        var returnString = s
        for (key in argsMap.keys) {
            argsMap[key]?.run {
                returnString = returnString.replace(key, this)
            }
        }
        return returnString
    }

    private fun getArgsKeyword(s: String): String? {
        if (s.contains("%")) {
            val regexString = MyApplication.context.getString(R.string.git_url_arg_regex)
            val regex = regexString.toRegex()
            val matchString = regex.find(s)?.value
            if (!matchString.isNullOrBlank()) {
                return matchString
            }
        }
        return null
    }

    private fun deleteSameString(string: String, stringList: List<String>): String {
        var returnString = string
        for (s in stringList) {
            returnString = returnString.replace(s, "")
        }
        return returnString
    }

    private fun preUrl(url: String): Pair<String?, List<String>>? {
        val gitUrlSplitList = url.split("://")
        if (gitUrlSplitList.isEmpty() || gitUrlSplitList.size > 2) return null
        var scheme: String? = null
        var others: String? = null
        for (s in gitUrlSplitList) {
            if (s.contains("/"))
                others = s
            else scheme = s
        }
        if (others == null) return null
        val addressList = others.split("/")
        return Pair(scheme, addressList)
    }
}