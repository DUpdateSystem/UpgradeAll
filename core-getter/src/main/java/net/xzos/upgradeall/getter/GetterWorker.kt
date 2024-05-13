package net.xzos.upgradeall.getter

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

lateinit var GETTER_PORT: GetterPort
private val mutex = Mutex()

suspend fun runGetterWorker(context: Context, getterPort: GetterPort) {
    GETTER_PORT = getterPort
    val workRequest: WorkRequest =
        OneTimeWorkRequestBuilder<GetterWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    WorkManager
        .getInstance(context)
        .enqueue(workRequest)
    while (!GETTER_PORT.waitService()) {
        delay(1000L)
    }
}

class GetterWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        runBlocking {
            mutex.withLock {
                Log.d("GetterWorker", "doWork(${id}): start")
                GETTER_PORT.runService()
                Log.d("GetterWorker", "doWork(${id}): stopped")
            }
        }
        return Result.success()
    }
}