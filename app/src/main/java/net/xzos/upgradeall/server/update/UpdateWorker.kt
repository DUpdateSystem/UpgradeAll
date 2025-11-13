package net.xzos.upgradeall.server.update

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.manager.AppManager

class UpdateWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    private val updateNotification = UpdateNotification()

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val notificationId = UpdateNotification.UPDATE_SERVER_RUNNING_NOTIFICATION_ID
            val notification = updateNotification.startUpdateNotification(notificationId)

            // Try to set foreground service, but continue if it fails due to Android 12+ restrictions
            try {
                setForeground(createForegroundInfo(notificationId, notification))
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException is a subclass of IllegalStateException
                // This can happen on Android 12+ when app is in background
                // Log and continue with background execution
                android.util.Log.w("UpdateWorker", "Cannot start foreground service, continuing in background", e)
            }

            doUpdateWork(updateNotification)
            finishNotify(updateNotification)
            Result.success()
        }
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