package org.hyperskill.musicplayer.viewmodel

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.hyperskill.musicplayer.model.Playlist
import org.hyperskill.musicplayer.model.Song
import org.hyperskill.musicplayer.model.TrackItem

const val PLAY_MUSIC_STATE = 1
const val ADD_PLAYLIST_STATE = 2

const val ALL_SONGS_PLAYLIST = "All Songs"

class PlayerViewModel : ViewModel() {
    var isMainSearchButtonWasClickedBefore = false

    private val _viewState: MutableLiveData<Int> = MutableLiveData(PLAY_MUSIC_STATE)
    val viewState: LiveData<Int> get() = _viewState

    private val allSongsPlaylist: Playlist = Playlist(ALL_SONGS_PLAYLIST, emptyList())

    private val _playlists: MutableList<Playlist> = mutableListOf(allSongsPlaylist)
    val playlists: List<Playlist> get() = _playlists

    private val _currentPlayList: MutableLiveData<Playlist> = MutableLiveData()
    val currentPlayList: LiveData<Playlist> get() = _currentPlayList

    private val _currentTrackItem: MutableLiveData<TrackItem> = MutableLiveData(null)
    val currentTrackItem: LiveData<TrackItem> get() = _currentTrackItem

    var mediaPlayer: MediaPlayer? = null
    var isMediaPlayerCompleted = false

    fun setCurrentPositionToZero(int: Int){
        _currentPosition.value = int
    }

    private val _currentPosition = MutableLiveData<Int>()
    val currentPosition: LiveData<Int> get() = _currentPosition

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 500

    private val updateRunnable : Runnable= object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    _currentPosition.postValue(player.currentPosition)
                    handler.postDelayed(this, updateInterval)
                }
            }
        }
    }

    fun startUpdatingPosition() {
        handler.post(updateRunnable)
    }

    fun stopUpdatingPosition() {
        handler.removeCallbacks(updateRunnable)
    }

    fun setViewState(state: Int){
        _viewState.value = state
    }

    fun setCurrentPlaylist(playlist: Playlist){
        _currentPlayList.value = playlist
    }

    fun setCurrentTrackItem(currentTrack: TrackItem) {
        _currentTrackItem.value = currentTrack
    }

    fun addPlaylist(playlist: Playlist){
        _playlists.add(playlist)
    }

    fun getAllSongsPlaylist(): Playlist{
        return allSongsPlaylist
    }

    fun deletePlaylistByName(name: String){
        val iterator = _playlists.iterator()
        while (iterator.hasNext()) {
            val playlist = iterator.next()
            if (playlist.name == name) {
                iterator.remove()
            }
        }
    }

    fun setUpPlaylist(name: String, songIDs: List<Long>): Playlist{
        val trackItems = mutableListOf<TrackItem>()
        for(songId in songIDs){
            val track = allSongsPlaylist.trackItems.firstOrNull { it.song.id == songId }
            if(track != null){
                trackItems.add(track)
            }
        }
        return Playlist(name, trackItems)
    }

    fun deletePlaylist(playlist: Playlist){
        _playlists.remove(playlist)
    }

    fun getPlaylistByName(playlistName: String): Playlist?{
        val playlist = _playlists.first { it.name == playlistName }
        return playlist
    }

    fun getPlaylistsNames(includeAllSongsName: Boolean): List<String>{
        val playlistNames = mutableListOf<String>()
        for(playlist in _playlists){
            if(playlist.name == ALL_SONGS_PLAYLIST && !includeAllSongsName){
                continue
            } else {
                playlistNames.add(playlist.name)
            }
        }
        return playlistNames
    }

    fun populateAllSongsPlaylist(trackItems: List<TrackItem>){
        getAllSongsPlaylist().addAllTrackItems(trackItems)
    }

    fun populateAllSongsPlaylist() {
        getAllSongsPlaylist().addAllTrackItems(getHardcodedTrackItems())
    }

    private fun getHardcodedTrackItems(): List<TrackItem> {
        val trackItems = mutableListOf<TrackItem>()
        for (song in 1 until 11) {
            val song = Song(song.toLong(), "title${song}", "artist${song}", 215_000)
            val trackItem = TrackItem(song)
            trackItems.add(trackItem)
        }
        return trackItems
    }

}