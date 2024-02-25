package com.danielp4.servicelesson

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat


class MusicService : Service() {

    private lateinit var player: MediaPlayer
    private lateinit var musicList: MutableList<Int>
    private var pausedPosition: Int = 0
    private var currentMusic: Int = 0
    private var isPaused: Boolean = false
    private var callback: MusicServiceCallback? = null
    private val binder = MusicBinder()
    private lateinit var notificationManager: NotificationManager


    interface MusicServiceCallback {
        fun onSongChanged(songName: String)
    }

    fun setCallback(callback: MusicServiceCallback?) {
        this.callback = callback
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "channel",
                "Music Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
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
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when(intent?.action) {
            Actions.STOP_F.toString() -> stopF()
            Actions.PLAY.toString() -> play()
            Actions.STOP.toString() -> stop()
            Actions.NEXT.toString() -> next()
            Actions.PREVIOUS.toString() -> previous()
        }
        sendCurrentSong()
        return START_NOT_STICKY
    }

    private fun stopF() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE)

        val previousIntent = Intent(this, MusicService::class.java)
        previousIntent.action = Actions.PREVIOUS.name
        val previousPendingIntent = PendingIntent.getService(this, 0, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val playIntent = Intent(this, MusicService::class.java)
        playIntent.action = Actions.PLAY.name
        val playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val stopIntent = Intent(this, MusicService::class.java)
        stopIntent.action = Actions.STOP.name
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val nextIntent = Intent(this, MusicService::class.java)
        nextIntent.action = Actions.NEXT.name
        val nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, "channel")
            .setContentTitle("Радиоволна Country Rock")
            .setContentText("Сейчас в Ваших ушках: ${currentSound()}")
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .setSound(null)
            .addAction(R.drawable.ic_skip_previous, "Previous", previousPendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .addAction(R.drawable.ic_skip_next, "Next", nextPendingIntent)
            .build()

    }

    private fun play() {

        if (!player.isPlaying && isPaused) {
            player.seekTo(pausedPosition)
            player.start()
            isPaused = false
        } else {
            player.start()
            val notification = createNotification()
            startForeground(1, notification)
            notificationManager.notify(1, notification)
        }
    }

    private fun stop() {
        if (player.isPlaying) {
            player.pause()
            pausedPosition = player.currentPosition
            isPaused = true
        }
        val notification = createNotification()
        notificationManager.notify(1, notification)
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
        val notification = createNotification()
        notificationManager.notify(1, notification)
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

    fun currentSound(): String {
        return formatterSong(musicList[currentMusic]).toMusicName()
    }

    private fun sendCurrentSong(): String {
        callback?.onSongChanged(currentSound())
        return currentSound()
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
        PLAY, STOP, NEXT, PREVIOUS, START_F, STOP_F
    }
}

