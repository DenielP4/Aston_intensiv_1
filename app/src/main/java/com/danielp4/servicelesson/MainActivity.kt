package com.danielp4.servicelesson

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.viewModels
import com.danielp4.servicelesson.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), MusicService.MusicServiceCallback {

    lateinit var binding: ActivityMainBinding

    private var timer: CountDownTimer? = null
    private lateinit var actions: Actions

    private val viewModel: MusicViewModel by viewModels()

    private lateinit var info: Pair<String, Int>

    private var musicService: MusicService? = null
    private var serviceBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            serviceBound = true

            musicService?.setCallback(this@MainActivity)
            Log.d("MyLog", "onServiceConnected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            serviceBound = false
            Log.d("MyLog", "onServiceDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val serviceIntent = Intent(this, MusicService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        binding.apply {
            val intent = Intent(this@MainActivity, MusicService::class.java)
//            Intent(intent).also {
//                it.action = MusicService.Actions.START.toString()
//                startService(it)
//            }
//            actions = Actions.START
            tvName.text = viewModel.name.value
            bStart.setOnClickListener {
                Intent(intent).also {
                    it.action = MusicService.Actions.PLAY.toString()
                    startService(it)
                }
                actions = Actions.PLAY
            }
            bStop.setOnClickListener {
                Intent(intent).also {
                    it.action = MusicService.Actions.STOP.toString()
                    startService(it)
                }
                actions = Actions.STOP
            }
            bPrevious.setOnClickListener {
                Intent(intent).also {
                    it.action = MusicService.Actions.PREVIOUS.toString()
                    startService(it)
                }
                actions = Actions.PREVIOUS
            }
            bNext.setOnClickListener {
                Intent(intent).also {
                    it.action = MusicService.Actions.NEXT.toString()
                    startService(it)
                }
                actions = Actions.NEXT
            }
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

    }

    override fun onStart() {
        super.onStart()
        info = musicService?.sendCurrentSongAndPosition() ?: ("" to 0)
        binding.apply {
            tvName.text = viewModel.name.value
        }
        Log.d("MyLog", "Activity onStart")
        Log.d("MyApp", "onStart ${viewModel.name.value} - ${viewModel.progress.value}")
    }

    override fun onResume() {
        super.onResume()
        info = musicService?.sendCurrentSongAndPosition() ?: ("" to 0)
        binding.apply {
            tvName.text = viewModel.name.value
        }
        Log.d("MyLog", "Activity onResume")
    }


    override fun onStop() {
        super.onStop()
        info = musicService?.sendCurrentSongAndPosition() ?: ("" to 0)
        binding.apply {
            tvName.text = viewModel.name.value
        }
        Log.d("MyLog", "Activity onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
            Log.d("MyLog", "serviceBound")
        }
        timer?.cancel()
        Log.d("MyLog", "Activity onDestroy")
        Log.d("MyApp", "onDestroy ${viewModel.name.value} - ${viewModel.progress.value}")
    }

    override fun onSongChanged(songName: String, time: Int, currentPosition: Int) {
        timer?.cancel()
        binding.apply {
            tvName.text = songName
            progressBar.max = time
        }
        viewModel.name.value = songName
        viewModel.timeSong.value = time
        viewModel.positionSong.value = currentPosition
        val positionSong = currentPosition
        val current = time - currentPosition + 1000L

        startTimer(current, positionSong)
    }


    private fun startTimer(time: Long, positionSong: Int) = with(binding) {
        timer = object : CountDownTimer(time, 1) {
            override fun onTick(restTime: Long) {
                if (actions.name == Actions.STOP.toString()){
                    timer?.cancel()
                } else {
                    tvTime.text = TimeUtils.getTime(restTime)
                    viewModel.progress.value = positionSong + (time - restTime).toInt()
                    progressBar.progress = viewModel.progress.value!!
                }
            }

            override fun onFinish() {
                timer?.cancel()
            }

        }.start()
    }

    enum class Actions {
        START, PLAY, STOP, NEXT, PREVIOUS, EXIT
    }

}