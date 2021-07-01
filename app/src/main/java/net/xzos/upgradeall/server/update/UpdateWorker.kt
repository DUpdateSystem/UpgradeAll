package net.xzos.upgradeall.server.update

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.manager.AppManager

class UpdateWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

    private val updateNotification = UpdateNotification()

    override fun doWork(): Result {
        updateNotification.startUpdateNotification(
            UpdateNotification.UPDATE_SERVER_RUNNING_NOTIFICATION_ID
        )
        runBlocking {
            doUpdateWork(updateNotification)
        }
        finishNotify(updateNotification)
        return Result.success()
    }

    override fun onStopped() {
        finishNotify(updateNotification)
        super.onStopped()
    }

    companion object {
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