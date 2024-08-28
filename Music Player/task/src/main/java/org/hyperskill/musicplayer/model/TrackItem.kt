package org.hyperskill.musicplayer.model

data class TrackItem(
    val song: Song,
    var isSelected: Boolean = false,
    var isCurrentTrack: Boolean = false,
) {
    var state: TrackState = TrackState.STOPPED
        private set

    enum class TrackState {
        PLAYING, PAUSED, STOPPED
    }

    fun getSongDurationMinutesAndSeconds(): Pair<Int, Int>{
        val minutes = ((song.durationInMilliseconds / 1000) / 60).toInt()
        val seconds = ((song.durationInMilliseconds / 1000) % 60).toInt()
        return Pair(minutes, seconds)
    }

    fun pauseTrack(){
        state = TrackState.PAUSED
    }

    fun playTrack(){
        state = TrackState.PLAYING
    }

    fun stopTrack(){
        state = TrackState.STOPPED
    }

}