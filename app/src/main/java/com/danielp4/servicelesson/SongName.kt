package com.danielp4.servicelesson

data class SongName(
    var group: String,
    val name: String
) {
    fun toMusicName(): String{
        return "$group - $name"
    }
}
