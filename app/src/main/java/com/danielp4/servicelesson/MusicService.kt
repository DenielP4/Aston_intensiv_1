package com.danielp4.servicelesson

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder


class MusicService : Service() {

    private lateinit var player: MediaPlayer
    private lateinit var musicList: MutableList<Int>
    private var pausedPosition: Int = 0
    private var currentMusic: Int = 0
    private var isPaused: Boolean = false
    private var callback: MusicServiceCallback? = null
    private val binder = MusicBinder()

    interface MusicServiceCallback {
        fun onSongChanged(songName: String)
    }

    fun setCallback(callback: MusicServiceCallback?) {
        this.callback = callback
    }


    override fun onCreate() {
        super.onCreate()
        musicList = mutableListOf(
            R.raw.spasibo__kloun,
            R.raw.discord__call_sound,
            R.raw.devochka__uensdei,
            R.raw.paramore__brick_by_boring_brick,
            R.raw.pharrell_williams__freedom,
            R.raw.pixies__where_is_my_mind,
            R.raw.queen__dont_stop_me_now,
            R.raw.rolling_stones__paint_it_black
        )
        currentMusic = 0
        player = MediaPlayer.create(this, musicList[currentMusic])
        player.setOnCompletionListener {
            next()
            sendCurrentSong()
        }
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            Actions.PLAY.toString() -> play()
            Actions.STOP.toString() -> stop()
            Actions.NEXT.toString() -> next()
            Actions.PREVIOUS.toString() -> previous()
        }
        sendCurrentSong()
        return START_STICKY
    }

    private fun play() {
        if (!player.isPlaying && isPaused) {
            player.seekTo(pausedPosition)
            player.start()
            isPaused = false
        } else {
            player.start()
        }
    }

    private fun stop() {
        if (player.isPlaying) {
            player.pause()
            pausedPosition = player.currentPosition
            isPaused = true
        }
    }

    private fun next() {
        if (currentMusic < musicList.size - 1) {
            currentMusic++
        } else {
            currentMusic = 0
        }
        player.stop()
        turnSound()
        player.start()
    }

    private fun turnSound() {
        player = MediaPlayer.create(this, musicList[currentMusic])
        player.setOnCompletionListener {
            next()
            sendCurrentSong()
        }
    }

    private fun previous() {
        if (currentMusic > 0) {
            currentMusic--
        } else {
            currentMusic = musicList.size - 1
        }
        player.stop()
        turnSound()
        player.start()
    }

    private fun sendCurrentSong(): String {
        val fullNameSong = formatterSong(musicList[currentMusic])
        val songName = fullNameSong.toMusicName()
        callback?.onSongChanged(songName)
        return songName
    }

    private fun formatterSong(song: Int): SongName {
        val fullNameSong: List<String> = getString(song).dropWhile { it!='/' }.drop(1).dropWhile { it!='/' }.drop(1).dropLastWhile { it!='.' }.dropLast(1).split("__")
        var groupList = fullNameSong[0].split("_")
        groupList = groupList.map { it.replaceFirstChar { it.uppercase() } }
        var nameList = fullNameSong[1].split("_")
        nameList = nameList.map { it.replaceFirstChar { it.uppercase() } }
        val songName = SongName(
            group = groupList.joinToString(" "),
            name = nameList.joinToString(" ")
        )
        return songName
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
    }

    enum class Actions {
        PLAY, STOP, NEXT, PREVIOUS
    }
}

