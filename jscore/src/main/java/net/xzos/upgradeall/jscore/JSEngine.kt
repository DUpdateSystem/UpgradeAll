package net.xzos.upgradeall.jscore

import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.jscore.js.engine.JavaScriptEngine


class JSEngine(
        private val objectTag: ObjectTag,
        private val URL: String?,
        private val jsCode: String?,
        private val debugMode: Boolean = false
) {

    val javaScriptEngine
        get() = JavaScriptEngine(
                objectTag, URL, jsCode, debugMode
        )
}