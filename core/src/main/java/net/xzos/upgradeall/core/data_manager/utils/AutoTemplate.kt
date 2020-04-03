package net.xzos.upgradeall.core.data_manager.utils

import net.xzos.upgradeall.core.data.config.AppConfig


class AutoTemplate(
    private val string: String?,
    private val template: String
) {

    private val regex = AppConfig.git_url_arg_regex

    val args: List<Arg>
        get() = if (string != null)
            matchArgs(string, template)
        else listOf()

    fun synthesis(template: String, extraArgs: List<Arg> = listOf()): String {
        return fillArgs(template, args + extraArgs)
    }

    private fun matchArgs(s: String, template: String): MutableList<Arg> {
        var cropString = s
        val valueList = mutableListOf<String>()
        val keywords = getArgsKeywords(template)
        val intervalStringList = getIntervalString(keywords, template)
        for (intervalString in intervalStringList) {
            val splitList = cropString.split(intervalString, limit = 2)
            if (splitList.isNotEmpty()) {
                if (splitList[0].isNotEmpty())
                    valueList.add(splitList[0])
                cropString = if (splitList.size == 2) {
                    splitList[1]
                } else ""
            }
        }
        valueList.add(cropString)
        val args = mutableListOf<Arg>()
        var i = 0
        for (key in keywords) {
            if (i < valueList.size) {
                args.add(
                    Arg(
                        key.value,
                        valueList[i]
                    )
                )
            } else break
            i++
        }
        return args
    }

    private fun getIntervalString(matchResults: Sequence<MatchResult>, template: String): List<String> {
        val list = mutableListOf<String>()
        var lastIndex = 0
        for (matchResult in matchResults) {
            val currentIndex = matchResult.range.first
            if (lastIndex != currentIndex)
                list.add(template.substring(lastIndex, currentIndex))
            lastIndex = matchResult.range.last + 1
        }
        if (lastIndex != template.length)
            list.add(template.substring(lastIndex))
        return list
    }

    private fun getArgsKeywords(template: String) =
        regex.findAll(template)

    companion object {
        fun fillArgs(template: String, args: List<Arg>): String {
            var returnString = template
            for (arg in args) {
                returnString = returnString.replace(arg.key, arg.value)
            }
            return returnString
        }
    }

    data class Arg(
        val key: String,
        val value: String
    )
}
