package com.androidcafe.meditationtimer.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.androidcafe.meditationtimer.R
import com.androidcafe.meditationtimer.view.MainActivity
import kotlinx.coroutines.delay
import java.util.*

class CountDownWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val SELECTED_MINUTES_INPUT_DATA_KEY = "SelectedMinutes"
        const val WORK_NAME = "CountDownWork"
        const val PROGRESS = "ElapsedTimeInMS"
        const val NOTIFICATION_CHANNEL_ID = "MeditationTimerNotificationChannelId"
        const val NOTIFICATION_ID = 0
    }

    private val debug: Boolean = false

    private val mediaPlayer = MediaPlayer.create(applicationContext, R.raw.daybreak)
    private val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val vibrateTimer = Timer()
    private val vibrateTimerTask = object: TimerTask() {
        override fun run() {
            vibrate()
        }
    }

    private val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override suspend fun doWork(): Result {
        return try {

            val minutes = inputData.getInt(SELECTED_MINUTES_INPUT_DATA_KEY, 0)
            var elapsedTimeInMS = (if(debug) 0.1 * 60000 else minutes * 60000).toLong()

            setForeground(createForegroundInfo(elapsedTimeInMS))

            while (elapsedTimeInMS > 0L) {
                setProgress(workDataOf(PROGRESS to elapsedTimeInMS))
                updateNotification(elapsedTimeInMS)
                delay(1000)
                elapsedTimeInMS -= 1000L
            }

            setProgress(workDataOf(PROGRESS to 0L))
            updateNotification(0L)

            startAlarm()

            while(true) {
                delay(1L * 60000L) // 1 minute
            }

            Result.success()

        } catch (e: Exception) {
            Result.failure()
        } finally {
            stopAlarm()
            cancelNotification()
        }
    }

    private fun startAlarm() {
        mediaPlayer.start()

        if(vibrator.hasVibrator()) {
            vibrateTimer.scheduleAtFixedRate(vibrateTimerTask, 0, 2000)
        }
    }

    private fun stopAlarm() {
        mediaPlayer.stop()
        mediaPlayer.release()

        vibrateTimer.cancel()
    }

    private fun vibrate() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    1000,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )

        } else {
            vibrator.vibrate(1000)
        }
    }

    private fun createForegroundInfo(elapsedTimeInMS: Long): ForegroundInfo {

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val mainActivityIntent = Intent(applicationContext, MainActivity::class.java)
        val mainActivityPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        notificationBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Remaining Minutes")
            .setContentText(getNotificationContextText(elapsedTimeInMS))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(mainActivityPendingIntent)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, "Stop", cancelIntent)

        return ForegroundInfo(NOTIFICATION_ID, notificationBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Primary", importance)
        // Register the channel with the system
        notificationManager.createNotificationChannel(channel)
    }

    private suspend fun updateNotification(elapsedTimeInMS: Long) {
        notificationBuilder.setContentText(getNotificationContextText(elapsedTimeInMS))

        setForeground(ForegroundInfo(NOTIFICATION_ID, notificationBuilder.build()))
        //notificationManager.notify(0, notificationBuilder.build())
    }

    private fun cancelNotification () {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun getNotificationContextText(elapsedTimeInMS: Long) : String {
        val minutes = (elapsedTimeInMS / 60000).toInt()
        val seconds = (elapsedTimeInMS % 60000 / 1000).toInt()

        return getTimerDisplayText(minutes, seconds)
    }

    private fun getTimerDisplayText(minutes: Int, seconds: Int) : String {

        //TODO: can use DateUtils.formatElapsedTime() API
        //TODO: use transformation map
        if(seconds < 10) {
            return "${minutes}:0${seconds}"
        }
        return "${minutes}:$seconds"
    }
}
