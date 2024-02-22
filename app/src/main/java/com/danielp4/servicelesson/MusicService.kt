package com.danielp4.servicelesson

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.danielp4.servicelesson.Constants.ACTION_CURRENT_SONG_AND_POSITION
import com.danielp4.servicelesson.Constants.EXTRA_CURRENT_POSITION
import com.danielp4.servicelesson.Constants.EXTRA_CURRENT_SONG

class MusicService : Service() {

    private lateinit var player: MediaPlayer
    private lateinit var musicList: MutableList<Int>
    private var pausedPosition: Int = 0
    private var currentMusic: Int = 0
    private var isPaused: Boolean = false
    private var callback: MusicServiceCallback? = null
    private val binder = MusicBinder()

    interface MusicServiceCallback {
        fun onSongChanged(songName: String, time: Int, currentPosition: Int)
//        fun onProgressChanged(currentPosition: Int)
    }

    fun setCallback(callback: MusicServiceCallback?) {
        this.callback = callback
    }

//    private fun setupProgressListener() {
//        Log.d("MyLog", "setupProgressListener")
//        callback?.onProgressChanged(player.currentPosition)
//    }

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
            sendCurrentSongAndPosition()
        }
        Log.d("MyLog", "Service onCreate")
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        when(intent?.action) {
            Actions.START.toString() -> start()
            Actions.PLAY.toString() -> play()
            Actions.STOP.toString() -> stop()
            Actions.NEXT.toString() -> next()
            Actions.PREVIOUS.toString() -> previous()
        }
        Log.d("MyLog", "Service onStartCommand")
        sendCurrentSongAndPosition()
        return START_STICKY
    }

    private fun start() {
        val notification = NotificationCompat
            .Builder(this, "running_channel")
            .setSmallIcon(R.drawable.ic_account)
            .setContentTitle("Музыкальный плеер запущен")
            .setStyle(
                NotificationCompat
                    .BigTextStyle()
                    .bigText(getCurrentSong())
            )
            .build()
        startForeground(1, notification)
    }

    fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "running_channel",
                "Music",
                NotificationManager.IMPORTANCE_HIGH
            )

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
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
            sendCurrentSongAndPosition()
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

    fun getCurrentSong(): String {
        val fullNameSong = formatterSong(musicList[currentMusic])
        val songName = fullNameSong.toMusicName()
        return songName
    }

    fun sendCurrentSongAndPosition(): Pair<String, Int> {
//        val fullNameSong = formatterSong(musicList[currentMusic])
//        val intent = Intent(ACTION_CURRENT_SONG_AND_POSITION)
//        intent.putExtra(EXTRA_CURRENT_SONG, fullNameSong.toMusicName())
//        intent.putExtra(EXTRA_CURRENT_POSITION, player.currentPosition)
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        val fullNameSong = formatterSong(musicList[currentMusic])
        val songName = fullNameSong.toMusicName()
        val time = player.duration
        val cuurentPosition = player.currentPosition
        callback?.onSongChanged(songName, time, cuurentPosition)
//        callback?.onProgressChanged(currentPosition)
        return songName to time
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
        Log.d("MyLog", "Service onDestroy")
    }

    enum class Actions {
        START, PLAY, STOP, NEXT, PREVIOUS
    }
}

