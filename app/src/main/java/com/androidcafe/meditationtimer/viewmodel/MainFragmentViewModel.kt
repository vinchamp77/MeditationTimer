package com.androidcafe.meditationtimer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import com.androidcafe.meditationtimer.R

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

    private var alarmIsPlaying: Boolean = false

    init {
        getData()
        updateSelectedMinutes(_selectedMinutes)
        updateButtonText(true)
    }

    override fun onCleared() {
        cancelCountDownWork()
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

    private fun cancelCountDownWork () {

        WorkManager.getInstance(app.baseContext).cancelUniqueWork(CountDownWorker.WORK_NAME)
        _countDownWorkInfo = null
    }
}