package net.xzos.upgradeall.server.update

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun startUpdateWorker(context: Context) {
    val updateWorkRequest = OneTimeWorkRequestBuilder<UpdateWorker>().build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork("update_service", ExistingWorkPolicy.REPLACE, updateWorkRequest)
}

fun startUpdate(lifecycleScope: CoroutineScope) {
    val updateNotification = UpdateNotification()
    val notificationId = UpdateNotification.UPDATE_SERVER_RUNNING_NOTIFICATION_ID
    updateNotification.startUpdateNotification(notificationId)
    lifecycleScope.launch(Dispatchers.IO) {
        UpdateWorker.doUpdateWork(updateNotification)
        UpdateWorker.finishNotify(updateNotification)
    }
}
