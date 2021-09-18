package com.androidcafe.meditationtimer.viewmodel

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import com.androidcafe.meditationtimer.R
import com.androidcafe.meditationtimer.view.MainActivity
import java.util.*

class MainFragmentViewModel(private val app: Application): AndroidViewModel(app) {

    companion object {
        val INTENT_START_ALARM_ACTION = "IntentStartAlarmAction"
    }

    private val debug: Boolean = false

    private val _gridViewItemDataList = MutableLiveData<List<GridViewItemData>>()
    val gridViewItemDataList: LiveData<List<GridViewItemData>> = _gridViewItemDataList

    private val _displayTimerText = MutableLiveData<String>()
    val displayTimerText: LiveData<String> = _displayTimerText

    private val _displayButtonText = MutableLiveData<String>()
    val displayButtonText: LiveData<String> = _displayButtonText

    private var _selectedMinutes: Int = 3
    val selectedMinutes : Int get() = _selectedMinutes

    private val selectedColor = app.resources.getColor(R.color.primary)
    private val unSelectedColor = app.resources.getColor(R.color.primary_light)
    private val startStr = app.resources.getString(R.string.button_start_display)
    private val stopStr = app.resources.getString(R.string.button_stop_display)

    private var _countDownWorkInfo : LiveData<WorkInfo>? = null
    val countDownWorkInfo : LiveData<WorkInfo>? get () = _countDownWorkInfo

    /*private val wakeLock: PowerManager.WakeLock by lazy {
        val pm = app.baseContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,  "MeditationTimer::WakelockTag")
    }*/
    private val pm = app.baseContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val alarmManager = app.baseContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val alarmIntent = Intent(INTENT_START_ALARM_ACTION)
    private val pendingAlarmIntent =
        PendingIntent.getBroadcast(
            getApplication<Application>().baseContext,
            0,
            alarmIntent,
            0)

    private val alarmBroadcastReceiver = AlarmBroadcastReceiver(this)

    private val mediaPlayer: MediaPlayer =  MediaPlayer.create(app.baseContext, R.raw.daybreak)
    private val vibrator = app.baseContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var vibrateTimer: Timer? = null

    private var countDownTimer: CountDownTimer? = null

    private val notificationManager =
        app.baseContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private var alarmIsPlaying: Boolean = false

    init {
        getData()
        updateSelectedMinutes(_selectedMinutes)
        updateButtonText(true)

        app.registerReceiver(alarmBroadcastReceiver, IntentFilter(INTENT_START_ALARM_ACTION))
    }

    override fun onCleared() {
        cancelCountDownWork()
        mediaPlayer.release()
        super.onCleared()
    }

    // call this when grid view item is clicked
    fun updateSelectedMinutes(minutes: Int) {
        _selectedMinutes = minutes

        for(data in _gridViewItemDataList.value!!) {
            if(data.meditateMinutes == minutes) {
                data.backgroundColor = selectedColor
            }
            else {
                data.backgroundColor = unSelectedColor
            }
        }

        updateCurrentTimerDisplayText()
    }

    // call this when button is click
    fun toggleButton () {
        if (displayButtonText.value == startStr)
        {
            startCountDownWork()
            updateDisplayOnCountDownStarted()
        }
        else
        {
            cancelCountDownWork()
            updateDisplayOnCountDownStopped()
        }
    }

    fun updateTimerDIsplayText(timeInMillis: Long) {
        val minutes = (timeInMillis / 60000).toInt()
        val seconds = (timeInMillis % 60000 / 1000).toInt()
        updateTimerDisplayText(minutes, seconds)
    }

    fun onCountDownWorkCancelled() {
        updateDisplayOnCountDownStopped()
    }

    fun notifyAlarmFired() {
        acquireWakeLock()
        triggerAlarm()

        //runBlocking {
        //    delay(5000)
        //}
    }

    private fun getData() {

        _gridViewItemDataList.value = mutableListOf(
            GridViewItemData(
                3,
                getViewItemDisplayText(3),
                0,
                true),
            GridViewItemData(
                5,
                getViewItemDisplayText(5),
                0,
                true),
            GridViewItemData(
                15,
                getViewItemDisplayText(15),
                0,
                true),
            GridViewItemData(
                30,
                getViewItemDisplayText(30),
                0,
                true),
        )
    }

    private fun getViewItemDisplayText(meditateMinutes:Int): String {
        return getApplication<Application>().resources.getString(
            R.string.text_item_display, meditateMinutes)
    }

    private fun updateDisplayOnCountDownStarted () {
        updateButtonText(false)
        updateClickable(false)
    }

    private fun updateDisplayOnCountDownStopped() {
        updateButtonText(true)
        updateClickable(true)

        updateCurrentTimerDisplayText()
    }

    private fun updateButtonText(showStartButton: Boolean) {
        if(showStartButton) {
            _displayButtonText.value = startStr
        }
        else{
            _displayButtonText.value = stopStr
        }
    }

    private fun updateCurrentTimerDisplayText() {
        updateTimerDisplayText(_selectedMinutes, 0)
    }

    private fun updateTimerDisplayText(minutes: Int, seconds: Int) {
        _displayTimerText.value = getTimerDisplayText(minutes, seconds)
    }

    private fun getTimerDisplayText(minutes: Int, seconds: Int) : String {

        //TODO: can use DateUtils.formatElapsedTime() API
        //TODO: use transformation map
        if(seconds < 10) {
            return "${minutes}:0${seconds}"
        }
        return "${minutes}:$seconds"
    }

    private fun updateClickable(clickable: Boolean) {

        for(data in _gridViewItemDataList.value!!) {
            data.clickable = clickable
        }
    }

    private fun startCountDownWork() {
        /*
        acquireWakeLock()

        val elapsedTimeInMS = (if(debug) 0.1 * 60000 else _selectedMinutes * 60000).toLong()

        countDownTimer = createCountDownTimer(elapsedTimeInMS)
        countDownTimer?.start()
        setAlarm(elapsedTimeInMS)
        createNotification(elapsedTimeInMS)

        return*/

        //val countDownWorkRequest = OneTimeWorkRequest.from(CountDownWorker::class.java)
        val workRequest = OneTimeWorkRequestBuilder<CountDownWorker>()
            .setInputData(workDataOf(
                CountDownWorker.SELECTED_MINUTES_INPUT_DATA_KEY to _selectedMinutes
            ))
            .build()

        WorkManager.getInstance(app.baseContext).enqueueUniqueWork(
            CountDownWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        _countDownWorkInfo = WorkManager.getInstance(app.baseContext)
            .getWorkInfoByIdLiveData(workRequest.id)
    }

    private fun acquireWakeLock() {
        cancelWakeLock()
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,  "MeditationTimer::WakelockTag")
        wakeLock?.acquire()
    }

    private fun cancelWakeLock() {
        wakeLock?.release()
        wakeLock = null
    }

    private fun cancelCountDownWork () {
        /*countDownTimer?.cancel()
        cancelAlarm()
        cancelNotification()
        cancelWakeLock()
        return*/

        WorkManager.getInstance(app.baseContext).cancelUniqueWork(CountDownWorker.WORK_NAME)
        _countDownWorkInfo = null

        cancelWakeLock()
    }

    private fun createCountDownTimer(timeInMillis: Long) : CountDownTimer {

        return object: CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerDIsplayText(millisUntilFinished)
                updateNotification(millisUntilFinished)
            }

            override fun onFinish() {
                //triggerAlarm()
            }
        }
    }

    private fun triggerAlarm() {
        updateTimerDisplayText(0,0)
        startAlarm()
    }

    private fun setAlarm(timeInMillis: Long) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Note: this does not work well
            /*alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                android.os.SystemClock.elapsedRealtime() + timeInMillis,
                pendingAlarmIntent)*/

            val info = AlarmManager.AlarmClockInfo(System.currentTimeMillis() + timeInMillis, pendingAlarmIntent)
            alarmManager.setAlarmClock(info, pendingAlarmIntent)
        } else {
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + timeInMillis,
                pendingAlarmIntent)
        }
    }

    private fun cancelAlarm() {
        alarmManager.cancel(pendingAlarmIntent)
        stopAlarm()
    }

    private fun startAlarm() {
        if(!mediaPlayer.isPlaying) mediaPlayer.start()

        if(vibrator.hasVibrator()) {

            vibrateTimer = Timer()

            val vibrateTimerTask = object: TimerTask() {
                override fun run() {
                    vibrate()
                }
            }

            vibrateTimer?.scheduleAtFixedRate(vibrateTimerTask, 0, 2000)
        }

        alarmIsPlaying = true;
    }

    private fun stopAlarm() {
        if(mediaPlayer.isPlaying) mediaPlayer.pause()

        vibrateTimer?.cancel()
        vibrateTimer = null

        alarmIsPlaying = false;
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

    private fun createNotification(elapsedTimeInMS: Long) {

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val mainActivityIntent = Intent(app.baseContext, MainActivity::class.java)
        val mainActivityPendingIntent = PendingIntent.getActivity(
            app.baseContext,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        //val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        notificationBuilder = NotificationCompat.Builder(app.baseContext,
            CountDownWorker.NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle("Remaining Minutes")
            .setContentText(getNotificationContextText(elapsedTimeInMS))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(mainActivityPendingIntent)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            //.addAction(android.R.drawable.ic_delete, "Stop", cancelIntent)

        notificationManager.notify(0, notificationBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CountDownWorker.NOTIFICATION_CHANNEL_ID, "Primary", importance)
        // Register the channel with the system
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification(elapsedTimeInMS: Long) {
        notificationBuilder.setContentText(getNotificationContextText(elapsedTimeInMS))

        //setForeground(ForegroundInfo(CountDownWorker.NOTIFICATION_ID, notificationBuilder.build()))
        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun cancelNotification () {
        notificationManager.cancel(CountDownWorker.NOTIFICATION_ID)
    }

    private fun getNotificationContextText(elapsedTimeInMS: Long) : String {
        val minutes = (elapsedTimeInMS / 60000).toInt()
        val seconds = (elapsedTimeInMS % 60000 / 1000).toInt()

        return getTimerDisplayText(minutes, seconds)
    }

    /* Future code reference


    private val playAudioServiceIntent : Intent by lazy {
        Intent(getApplication(), PlayAudioService::class.java)
    }

    private lateinit var playAudioService: PlayAudioService

    private val playAudioServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayAudioService.LocalServiceBinder
            playAudioService = binder.getService()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
        }
    }

    init {

        mediaPlayer.isLooping = true
        mediaPlayer.setWakeMode(app.baseContext, PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP)
        mediaPlayer.setScreenOnWhilePlaying(true)
        app.registerReceiver(alarmBroadcastReceiver, IntentFilter(INTENT_START_ALARM_ACTION))
    }

    private fun startPlayAudioService() {
        getApplication<Application>().startService(playAudioServiceIntent)
    }

    private fun bindPlayAudioService() {
        getApplication<Application>().bindService(
            playAudioServiceIntent,
            playAudioServiceConnection,
            BIND_AUTO_CREATE
        )
    }

    private fun unbindPlayAudioService() {
        getApplication<Application>().unbindService(
            playAudioServiceConnection)
    }

    */
}