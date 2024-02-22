package com.danielp4.servicelesson

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MusicViewModel : ViewModel() {

    var name = MutableLiveData<String>()
    var timeSong = MutableLiveData<Int>()
    var positionSong = MutableLiveData<Int>()
    var progress = MutableLiveData<Int>()

}