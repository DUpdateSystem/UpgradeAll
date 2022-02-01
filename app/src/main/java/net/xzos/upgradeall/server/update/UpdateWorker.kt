package net.xzos.upgradeall.server.update

import android.app.Notification
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
        val notificationId = UpdateNotification.UPDATE_SERVER_RUNNING_NOTIFICATION_ID
        val notification = updateNotification.startUpdateNotification(notificationId)
        setForeground(createForegroundInfo(notificationId, notification))
        doUpdateWork(updateNotification)
        finishNotify(updateNotification)
        return Result.success()
    }

    companion object {
        private fun createForegroundInfo(
            notificationId: Int, notification: Notification
        ): ForegroundInfo {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    notificationId, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                ForegroundInfo(notificationId, notification)
            }
        }

        suspend fun doUpdateWork(updateNotification: UpdateNotification) {
            AppManager.renewApp(
                updateNotification.renewStatusFun,
                updateNotification.recheckStatusFun
            )
        }

        fun finishNotify(updateNotification: UpdateNotification) {
            updateNotification.updateDone()
            updateNotification.cancelNotification(
                UpdateNotification.UPDATE_SERVER_RUNNING_NOTIFICATION_ID
            )
        }
    }
}