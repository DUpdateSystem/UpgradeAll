package net.xzos.upgradeall.server.update

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking

class UpdateWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    override fun doWork(): Result {
        runBlocking { doUpdateWork() }
        return Result.success()
    }
}