package net.xzos.upgradeall.server.update

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import net.xzos.upgradeall.core.manager.AppManager

class UpdateWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    private val updateNotification = UpdateNotification()

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo(updateNotification))
        doUpdateWork(updateNotification)
        finishNotify(updateNotification)
        return Result.success()
    }

    companion object {
        private fun createForegroundInfo(updateNotification: UpdateNotification): ForegroundInfo {
            val notificationId = UpdateNotification.UPDATE_SERVER_RUNNING_NOTIFICATION_ID
            val notification = updateNotification.startUpdateNotification(notificationId)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    notificationId, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                ForegroundInfo(notificationId, notification)
            }
        }

        private suspend fun doUpdateWork(updateNotification: UpdateNotification) {
            AppManager.renewApp(
                updateNotification.renewStatusFun,
                updateNotification.recheckStatusFun
            )
        }

        private fun finishNotify(updateNotification: UpdateNotification) {
            updateNotification.updateDone()
            updateNotification.cancelNotification(
                UpdateNotification.UPDATE_SERVER_RUNNING_NOTIFICATION_ID
            )
        }
    }
}