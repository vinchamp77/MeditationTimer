package com.androidcafe.meditationtimer.viewmodel

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import com.androidcafe.meditationtimer.R

class PlayAudioService : Service() {

    inner class LocalServiceBinder : Binder() {
        fun getService() : PlayAudioService {
            return this@PlayAudioService
        }
    }

    private val mediaPlayer: MediaPlayer by lazy {
        MediaPlayer.create(this, R.raw.daybreak)
    }

    private val binder = LocalServiceBinder()


    override fun onCreate() {
        mediaPlayer.isLooping = true
    }
    /*
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mediaPlayer.start()
        return Service.START_NOT_STICKY;
    }*/

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onDestroy() {
        mediaPlayer.release()
    }

    fun start() {
        if(!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    fun pause() {
        if(mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }
}