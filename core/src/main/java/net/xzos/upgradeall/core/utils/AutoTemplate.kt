package net.xzos.upgradeall.core.utils

import net.xzos.upgradeall.core.data.URL_ARG_REGEX


class AutoTemplate(private val string: String?, private val template: String) {

    val args: Map<String, String>
        get() = if (string != null)
            matchArgs(string, template)
        else mapOf()

    fun synthesis(template: String, extraArgs: Map<String, String> = mapOf()): String {
        return fillArgs(template, args + extraArgs)
    }

    private fun matchArgs(s: String, template: String): Map<String, String> {
        var cropString = s
        val keywordMatchResultList = getArgsKeywords(template)
        val keywords = keywordMatchResultList.map { it.value }
        val stringIndexList = getStringIndexList(keywordMatchResultList, template)
        val args = mutableMapOf<String, String>()
        val intervalStringMap = mutableMapOf<Int, String>()
        for (i in stringIndexList.indices) {
            val item = stringIndexList[i]
            if (!keywords.contains(item)) {
                intervalStringMap[i] = item
            }
        }
        for ((index, intervalString) in intervalStringMap.entries) {
            val splitList = cropString.split(intervalString, limit = 2)
            if (splitList.isNotEmpty()) {
                var keyA: String? = null
                var keyB: String? = null
                if (index - 1 > 0 && index - 1 < stringIndexList.size)
                    keyA = stringIndexList[index - 1]
                if (index + 1 < stringIndexList.size)
                    keyB = stringIndexList[index + 1]
                if (keyA != null) {
                    args[keyA] = splitList[0]
                }
                if (keyB != null && splitList.size == 2) {
                    args[keyB] = splitList[1]
                }
                cropString = if (splitList.size == 2) {
                    splitList[1]
                } else ""
            }
        }
        return args
    }

    private fun getStringIndexList(
            matchResults: Sequence<MatchResult>,
            template: String
    ): List<String> {
        val list = mutableListOf<String>()
        var lastIndex = 0
        for (matchResult in matchResults) {
            val currentIndex = matchResult.range.first
            if (lastIndex != currentIndex)
                list.add(template.substring(lastIndex, currentIndex))
            list.add(matchResult.value)
            lastIndex = matchResult.range.last + 1
        }
        if (lastIndex != template.length)
            list.add(template.substring(lastIndex))
        return list
    }

    companion object {
        private val regex = URL_ARG_REGEX

        fun getArgsKeywords(template: String) = regex.findAll(template)

        fun fillArgs(template: String, args: Map<String, String?>): String {
            var returnString = template
            for (arg in args) {
                returnString = returnString.replace(arg.key, arg.value ?: "NULL")
            }
            return returnString
        }

        fun urlToAppId(url: String, templateList: List<String>): Map<String, String>? {
            if (url.isBlank() || templateList.isEmpty())
                return null
            for (template in templateList) {
                val keyList = getArgsKeywords(template).map { it.value }.toList()
                val autoTemplate = AutoTemplate(url, template)
                val args = autoTemplate.args
                if (args.keys == keyList) {
                    return args.mapKeys { it.key.replaceFirst("%", "") }
                }
            }
            return null
        }
    }
}