package com.example.aigeneratedandroid.microlearning.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Downloads the newest episode and its audio in the background, so it's ready (and plays
 * offline) by the time you're making coffee.
 */
class PrefetchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = EpisodeRepository(applicationContext)
        val episode = repo.refreshLatest() ?: return Result.retry()
        return runCatching {
            repo.downloadAudio(episode)
            repo.pruneAudio()
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PrefetchWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("prefetch-episode", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
