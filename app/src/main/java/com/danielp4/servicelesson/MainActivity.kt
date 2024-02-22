package com.danielp4.servicelesson

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.danielp4.servicelesson.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), MusicService.MusicServiceCallback {

    lateinit var binding: ActivityMainBinding

    private val viewModel: MusicViewModel by viewModels()

    private var musicService: MusicService? = null
    private var serviceBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
            musicService?.setCallback(this@MainActivity)
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            val intent = Intent(this@MainActivity, MusicService::class.java)
            bStart.setOnClickListener {
                Intent(intent).also {
                    it.action = MusicService.Actions.PLAY.toString()
                    startService(it)
                }
            }
            bStop.setOnClickListener {
                Intent(intent).also {
                    it.action = MusicService.Actions.STOP.toString()
                    startService(it)
                }
            }
            bPrevious.setOnClickListener {
                Intent(intent).also {
                    it.action = MusicService.Actions.PREVIOUS.toString()
                    startService(it)
                }
            }
            bNext.setOnClickListener {
                Intent(intent).also {
                    it.action = MusicService.Actions.NEXT.toString()
                    startService(it)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, MusicService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        binding.apply {
            tvName.text = if (viewModel.name.value == null) getString(R.string.main_text) else viewModel.name.value
        }
        val intent = Intent(this@MainActivity, MusicService::class.java)
        Intent(intent).also {
            it.action = MusicService.Actions.START_F.toString()
            startService(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent(this@MainActivity, MusicService::class.java)
        Intent(intent).also {
            it.action = MusicService.Actions.STOP_F.toString()
            stopService(it) // TODO Разобраться, почему при выключении приложения сервис Destroy, а при повороте не Destroy
        }
    }

    override fun onSongChanged(songName: String) {
        binding.apply {
            tvName.text = songName
        }
        viewModel.name.value = songName
    }
}