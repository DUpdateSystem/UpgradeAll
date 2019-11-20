package net.xzos.upgradeAll.data.json.gson

import net.xzos.upgradeAll.application.MyApplication
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class UIConfig {

    var appList = parseAppList()
        set(value) {
            field = value
            save()
        }

    data class AppGroupBean(
            var name: String,
            var member: List<Any>
    ) {
        fun toJSONObject(): JSONObject {
            val jsonArray = JSONArray()
            for (i in member) {
                if (i is Long) {
                    jsonArray.put(i)
                } else if (i is AppGroupBean) {
                    jsonArray.put(i.toJSONObject())
                }
            }
            val json = JSONObject()
            json.put("name", name)
            json.put("member", jsonArray)
            return json
        }
    }

    fun save() {
        val appJsonArray = JSONArray()
        for (i in appList) {
            if (i is Long) {
                appJsonArray.put(i)
            } else if (i is AppGroupBean) {
                appJsonArray.put(i.toJSONObject())
            }
        }
        val parent = CONFIG_FILE.parentFile
        if (parent != null && !parent.exists())
            parent.mkdirs()
        json.put("app_list", appJsonArray)
        CONFIG_FILE.writeText(json.toString())
    }

    fun getAppIdUnderNode(indexList: List<Int>): List<Long> {
        if (indexList.isNotEmpty()) {
            val obj: Any = appList[indexList[0]]
            for (i in indexList) {
                if (obj is Long)
                    return listOf(obj)
                else if (obj is AppGroupBean) {
                    return parseAppIdFromMemberList(obj.member)
                }
            }
        }
        // 若索引队列为空则返回所有 UI_CONFIG 已记录的 ID
        return parseAppIdFromMemberList(appList)
    }

    private fun parseAppIdFromMemberList(member: List<Any>): List<Long> {
        val idList = mutableListOf<Long>()
        for (i in member) {
            if (i is Long)
                idList.add(i)
            else if (i is AppGroupBean) {
                val childMember = i.member
                val childNodeMemberList = parseAppIdFromMemberList(childMember)
                idList += childNodeMemberList
            }
        }
        return idList
    }

    private fun parseAppList(): MutableList<Any> {
        val returnList = mutableListOf<Any>()
        val appList = json.getJSONArray("app_list")
        val length = appList.length() - 1
        for (i in 0..length) {
            var obj = appList.get(i)
            obj = parsingMemberBean(obj)
            returnList.add(obj)
        }
        return returnList
    }

    private fun parsingMemberBean(obj: Any): Any? {
        return when (obj) {
            is Int -> obj.toLong()
            is JSONObject -> {
                val memberList = mutableListOf<Any>()
                val name = obj.getString("name")
                val member = obj.getJSONArray("member")
                val length = member.length() - 1
                for (i in 0..length) {
                    var memberObj = member.get(i)
                    memberObj = parsingMemberBean(memberObj)
                    if (memberObj != null)
                        memberList.add(obj)
                }
                AppGroupBean(name, memberList)
            }
            else -> obj
        }
    }

    companion object {
        private const val CONFIG_FILE_PATH = "ui.json"
        private val CONFIG_FILE = File(MyApplication.context.filesDir, CONFIG_FILE_PATH)
        private val json = try {
            JSONObject(CONFIG_FILE.readText())
        } catch (e: Throwable) {
            val json = JSONObject()
            json.put("app_list", JSONArray())
            json
        }
    }
}