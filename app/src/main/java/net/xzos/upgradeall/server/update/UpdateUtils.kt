package net.xzos.upgradeall.server.update

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.server.update.UpdateNotification.Companion.UPDATE_SERVER_RUNNING_NOTIFICATION_ID


fun startUpdateWorker(context: Context) {
    val tag = "update_service"
    val updateWorkRequest = OneTimeWorkRequestBuilder<UpdateWorker>().build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork(tag, ExistingWorkPolicy.KEEP, updateWorkRequest)
}
