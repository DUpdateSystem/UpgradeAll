package net.xzos.upgradeAll.server.app.engine.js.utils

import android.annotation.SuppressLint
import com.arialyy.annotations.Download
import com.arialyy.annotations.Upload
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadEntity
import com.arialyy.aria.core.download.DownloadTarget
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.json.nongson.JSCacheData
import net.xzos.upgradeAll.server.ServerContainer
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.seimicrawler.xpath.JXDocument
import java.io.File
import java.util.*


/**
 * 爬虫相关库的打包集合
 * For JavaScript
 */
class JSUtils(private val logObjectTag: Array<String>) {

    private var jsoupApi = JsoupApi(logObjectTag, jsCacheData)

    fun getJSONObject(): JSONObject {
        return JSONObject()
    }

    fun getJSONArray(): JSONArray {
        return JSONArray()
    }

    @Throws(JSONException::class)
    fun getJSONObject(jsonString: String? = null): JSONObject {
        return if (jsonString != null) JSONObject(jsonString)
        else JSONObject()
    }

    @Throws(JSONException::class)
    fun getJSONArray(jsonString: String? = null): JSONArray {
        return if (jsonString != null) JSONArray(jsonString)
        else JSONArray()
    }

    fun getHttpResponse(URL: String): String? {
        val httpResponseMap = jsCacheData.httpResponseDict
        val time = httpResponseMap[URL]?.first
        var response = httpResponseMap[URL]?.second
        if (response == null || !JSCacheData.isFreshness(time)) {
            response = OkHttpApi(logObjectTag, jsCacheData = jsCacheData).getHttpResponse(URL).first
            if (response != null) {
                httpResponseMap[URL] = Pair(Calendar.getInstance(), response)
                Log.d(logObjectTag, TAG, "OkHttp: $URL 已刷新")
            }
        } else {
            Log.d(logObjectTag, TAG, "OkHttp: $URL 已缓存")
        }
        return response
    }

    fun selNByJsoupXpath(userAgent: String?, URL: String, xpath: String): ArrayList<*> {
        val jsoupDomDict = jsCacheData.jsoupDomDict
        val time = jsoupDomDict[URL]?.first
        var doc = jsoupDomDict[URL]?.second
        if (doc == null || !JSCacheData.isFreshness(time)) {
            doc = jsoupApi.getDoc(URL, userAgent = userAgent)
            if (doc != null) {
                jsoupDomDict[URL] = Pair(Calendar.getInstance(), doc)
                Log.d(logObjectTag, TAG, "Jsoup: $URL 已刷新")
            } else {
                Log.e(logObjectTag, TAG, "selNByJsoupXpathJavaList: Jsoup 对象初始化失败")
                return ArrayList<Any>()
            }
        } else
            Log.d(logObjectTag, TAG, "Jsoup: $URL 已缓存")
        val jxDocument = JXDocument.create(doc)
        val nodeStringArrayList = ArrayList<String>()
        for (node in jxDocument.selN(xpath)) {
            nodeStringArrayList.add(node.toString())
        }
        Log.d(logObjectTag, TAG, "selNByJsoupXpath: node_list number: " + nodeStringArrayList.size)
        return nodeStringArrayList
    }

    @SuppressLint("CheckResult")
    fun downloadFile(fileName: String, URL: String): String {
        @Download.onTaskFail
        @Upload.onTaskComplete
        fun taskFinish(download: DownloadTarget, downloadFile: File) {
            download.removeRecord()
            downloadFile.deleteOnExit()
            // TODO: 可控的下载文件管理
        }
        Aria.download(this).load(URL).cancel(true)  // 取消已有任务，避免冲突
        val cookies = jsCacheData.cookieManager.getCookies(URL)
        var cookieString = ""
        for (key in cookies.keys) {
            cookieString += "$key=${cookies[key]}; "
        }
        cookieString = cookieString.substringBeforeLast(";")
        val file = File(MyApplication.context.externalCacheDir, fileName)
        val download = Aria.download(this)
                .load(URL)
                .useServerFileName(true)
                .setFilePath(file.path)
        if (cookieString.isNotBlank()) {
            download.addHeader("Cookie", cookieString)
        }
        val taskList = Aria.download(this).totalTaskList
        for (task in taskList) {
            task as DownloadEntity
            if (task.filePath == file.path)
                task.deleteData()
        }
        taskFinish(download, file)
        download.start()
        return file.path
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "JSUtils"

        private val jsCacheData = JSCacheData()
    }
}