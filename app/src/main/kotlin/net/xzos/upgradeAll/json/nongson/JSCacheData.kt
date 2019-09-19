package net.xzos.upgradeAll.json.nongson

import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.ui.viewmodels.componnent.EditIntPreference
import org.jsoup.nodes.Document
import java.util.*

data class JSCacheData(
        val httpResponseDict: MutableMap<String, Pair<Calendar, String>> = mutableMapOf(),
        val jsoupDomDict: MutableMap<String, Pair<Calendar, Document>> = mutableMapOf()
) {
    companion object {
        fun isFreshness(time: Calendar?): Boolean {
            return if (time == null)
                false
            else {
                val defaultDataExpirationTime = MyApplication.context.resources.getInteger(R.integer.default_data_expiration_time)  // 默认自动刷新时间 10min
                val autoRefreshMinute = EditIntPreference.getInt("sync_time", defaultDataExpirationTime)
                time.add(Calendar.MINUTE, autoRefreshMinute)
                Calendar.getInstance().before(time)
            }
        }
    }
}
