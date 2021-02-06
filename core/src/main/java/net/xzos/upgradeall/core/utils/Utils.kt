package net.xzos.upgradeall.core.utils

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.log.ObjectTag.Companion.core
import java.io.StringReader
import java.security.MessageDigest
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    md.update(this.toByteArray())
    return md.digest().toString(Charsets.UTF_8)
}

fun parsePropertiesString(s: String): Properties {
    return Properties().apply {
        this.load(StringReader(s))
    }
}

fun Mutex.unlockAfterComplete(action: () -> Unit) {
    action()
    if (this.isLocked) {
        this.unlock()
    }
}

suspend fun Mutex.wait() {
    if (this.isLocked) {
        withLock { }
    }
}

fun <T> Mutex.runWithLock(context: CoroutineContext = EmptyCoroutineContext, action: () -> T): T {
    return runBlocking(context) {
        this@runWithLock.withLock {
            action()
        }
    }
}

fun requestPermission(
        activity: Activity,
        permission: String,
        PERMISSIONS_REQUEST_CONTACTS: Int,
        tipResId: Int
): Boolean {
    val tag = "RequestPermission"
    val logObjectTag = ObjectTag(core, tag)
    var havePermission = false
    if (ContextCompat.checkSelfPermission(
                    activity,
                    permission
            ) != PackageManager.PERMISSION_GRANTED
    ) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            Log.i(logObjectTag, tag, tipResId.toString())
        }
        ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                PERMISSIONS_REQUEST_CONTACTS
        )
    } else
        havePermission = true
    return havePermission
}