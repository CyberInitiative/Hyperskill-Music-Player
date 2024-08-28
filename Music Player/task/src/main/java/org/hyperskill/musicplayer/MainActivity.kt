package org.hyperskill.musicplayer

import android.Manifest
import android.app.AlertDialog
import android.content.ContentUris
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.hyperskill.musicplayer.adapter.PlaylistAdapter
import org.hyperskill.musicplayer.database.DatabaseHelper
import org.hyperskill.musicplayer.model.Playlist
import org.hyperskill.musicplayer.model.Song
import org.hyperskill.musicplayer.model.TrackItem
import org.hyperskill.musicplayer.viewmodel.ADD_PLAYLIST_STATE
import org.hyperskill.musicplayer.viewmodel.ALL_SONGS_PLAYLIST
import org.hyperskill.musicplayer.viewmodel.PLAY_MUSIC_STATE
import org.hyperskill.musicplayer.viewmodel.PlayerViewModel

const val PERMISSION_REQUEST_CODE = 1

class MainActivity : AppCompatActivity(), PlaylistAdapter.OnListItemSongViewHolderActionListener,
    MainPlayerControllerFragment.OnMainPlayerControllerFragmentButtonsClicksListener,
    MainAddPlaylistFragment.OnMainAddPlaylistFragmentButtonsClicksListener {

    private lateinit var mainFragmentContainer: FragmentContainerView
    private lateinit var mainButtonSearch: Button

    private lateinit var mainSongList: RecyclerView
    private lateinit var playlistAdapter: PlaylistAdapter

    private lateinit var viewModel: PlayerViewModel
    private lateinit var mediaPlayer: MediaPlayer

    private var dbHelper: DatabaseHelper = DatabaseHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this).get(PlayerViewModel::class.java)

        initializeViews()
        setViewStateObserver()
        setCurrentPlaylistObserver()
        setMainSearchButtonOnClickListener()

        setMediaPlayerOnCompletionListener()
    }

    private fun initializeViews() {
        mainFragmentContainer = findViewById(R.id.mainFragmentContainer)
        mainButtonSearch = findViewById(R.id.mainButtonSearch)

        playlistAdapter = PlaylistAdapter(viewModel.getAllSongsPlaylist(), this)
        mainSongList = findViewById<RecyclerView?>(R.id.mainSongList).apply {
            adapter = playlistAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun loadSongs(): List<TrackItem> {
        val allTracksItems = mutableListOf<TrackItem>()

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION
        )

        val query = contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val duration = cursor.getLong(durationColumn)

                val song = Song(id, title, artist, duration)
                allTracksItems.add(TrackItem(song))

            }
        }
        return allTracksItems
    }

    private fun setLoadedSongs() {
        val trackItems = loadSongs()
        if (loadSongs().isNotEmpty()) {
            viewModel.populateAllSongsPlaylist(trackItems)
        }
    }

    private fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            PermissionChecker.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PermissionChecker.PERMISSION_GRANTED
        }
    }

    private fun requestForPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE,
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setLoadedSongs()
                viewModel.setCurrentPlaylist(viewModel.getAllSongsPlaylist())
            } else {
                Toast.makeText(this, "Songs cannot be loaded without permission", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun setViewStateObserver() {
        viewModel.viewState.observe(this, Observer {
            it.let {
                playlistAdapter.listState = it
                when (it) {
                    PLAY_MUSIC_STATE -> {
                        playlistAdapter.playlist.unselectAllSelectedTracks()

                        supportFragmentManager.beginTransaction()
                            .replace(R.id.mainFragmentContainer, MainPlayerControllerFragment())
                            .commit()
                    }

                    ADD_PLAYLIST_STATE -> {
                        playlistAdapter.playlist = viewModel.getAllSongsPlaylist()
                        playlistAdapter.notifyDataSetChanged()

                        supportFragmentManager.beginTransaction()
                            .replace(R.id.mainFragmentContainer, MainAddPlaylistFragment())
                            .commit()
                    }
                }
            }
        })
    }

    private fun setCurrentPlaylistObserver() {
        viewModel.currentPlayList.observe(this, Observer {
            if (it != null) {
                playlistAdapter.playlist = it
                if (it.getCurrentTrackInPlaylist() == null) {
                    if (it.trackItems.isNotEmpty()) {
                        val currentTrack = it.trackItems.first()
                        it.setCurrentTrackInPlaylist(currentTrack)
                        viewModel.setCurrentTrackItem(currentTrack)
                        currentTrack.pauseTrack()
                        val songUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            currentTrack.song.id
                        )
                        if (this::mediaPlayer.isInitialized) {
                            mediaPlayer.seekTo(0)
                            viewModel.setCurrentPositionToZero(0)
                            mediaPlayer.release()
                        }
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(this@MainActivity, songUri)
                            prepare()
                        }
                        viewModel.mediaPlayer = mediaPlayer
                        setMediaPlayerOnCompletionListener()
                    }
                }
                playlistAdapter.notifyDataSetChanged()
            }

        })
    }

    private fun setMediaPlayerOnCompletionListener() {
        viewModel.mediaPlayer?.let {
            it.setOnCompletionListener {

                viewModel.stopUpdatingPosition()
                playlistAdapter.playlist.getCurrentTrackInPlaylist()?.let {
                    it.stopTrack()
                    println(playlistAdapter.playlist.getIndexOfCurrentTrack())
                    playlistAdapter.notifyItemChanged(playlistAdapter.playlist.getIndexOfCurrentTrack())
                }
                viewModel.setCurrentPositionToZero(0)
                mediaPlayer.seekTo(0)

                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.reset()
                viewModel.isMediaPlayerCompleted = true
                val songUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    playlistAdapter.playlist.getCurrentTrackInPlaylist()!!.song.id
                )

                mediaPlayer.setDataSource(this@MainActivity, songUri)
                mediaPlayer.prepare()
            }
        }
    }

    private fun startMediaPlayer() {
        mediaPlayer.start()
        viewModel.startUpdatingPosition()
    }

    private fun pauseMediaPlayer() {
        mediaPlayer.pause()
        viewModel.stopUpdatingPosition()
    }

    private fun resetMediaPlayer(songId: Long) {
        viewModel.stopUpdatingPosition()
        mediaPlayer.seekTo(0)
        viewModel.setCurrentPositionToZero(0)

        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.reset()

        val songUri =
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

        mediaPlayer.setDataSource(this@MainActivity, songUri)
        mediaPlayer.prepare()

        setMediaPlayerOnCompletionListener()
    }

    private fun setMainSearchButtonOnClickListener() {
        mainButtonSearch.setOnClickListener {
            if (isPermissionGranted()) {
                when (viewModel.viewState.value) {
                    PLAY_MUSIC_STATE -> {
                        setLoadedSongs()
                        viewModel.setCurrentPlaylist(viewModel.getAllSongsPlaylist())
                    }

                    ADD_PLAYLIST_STATE -> {
                        playlistAdapter.playlist = viewModel.getAllSongsPlaylist()
                        playlistAdapter.notifyDataSetChanged()
                    }
                }
            } else {
                requestForPermission()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.playlist_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mainMenuAddPlaylist -> {
                onMainMenuAddPlaylistOptionSelected()
                true
            }

            R.id.mainMenuLoadPlaylist -> {
                onMainMenuLoadPlaylistOptionSelected()
                true
            }

            R.id.mainMenuDeletePlaylist -> {
                onMainMenuDeletePlaylistOptionSelected()
                true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun onMainMenuAddPlaylistOptionSelected() {
        if (viewModel.viewState.value != ADD_PLAYLIST_STATE) {
            val allSongsPlaylist = viewModel.getAllSongsPlaylist()
            if (allSongsPlaylist.trackItems.isEmpty()) {
                Toast.makeText(
                    this,
                    "no songs loaded, click search to load songs",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                viewModel.setViewState(ADD_PLAYLIST_STATE)
            }
        }
    }

    private fun onMainMenuLoadPlaylistOptionSelected() {
        val preItems = mutableListOf(ALL_SONGS_PLAYLIST)
        preItems.addAll(dbHelper.getAllPlaylistNames())
        val items = preItems.toList().toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("choose playlist to load")
            .setItems(items) { _, i ->
                if (!viewModel.isMainSearchButtonWasClickedBefore) {
                    setLoadedSongs()
                }
                val oldPlaylist = playlistAdapter.playlist
                val playlist: Playlist = if (items[i] != ALL_SONGS_PLAYLIST) {
                    viewModel.setUpPlaylist(items[i], dbHelper.getPlaylist(items[i]))
                } else {
                    viewModel.getAllSongsPlaylist()
                }

                when (viewModel.viewState.value) {
                    PLAY_MUSIC_STATE -> {
                        viewModel.setCurrentPlaylist(playlist)
                    }

                    ADD_PLAYLIST_STATE -> {
                        playlistAdapter.playlist = playlist
                        playlistAdapter.notifyDataSetChanged()
                    }
                }
                for (trackItem in oldPlaylist.trackItems) {
                    if (!playlist.trackItems.contains(trackItem) && trackItem.isSelected) {
                        trackItem.isSelected = false
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onMainMenuDeletePlaylistOptionSelected() {
        val items = dbHelper.getAllPlaylistNames().toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("choose playlist to delete")
            .setItems(items) { _, i ->
                if (viewModel.currentPlayList.value?.name == items[i]) {
                    viewModel.setCurrentPlaylist(viewModel.getAllSongsPlaylist())
                }
                dbHelper.deletePlaylist(items[i])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onClick(position: Int) {
        when (viewModel.viewState.value) {
            PLAY_MUSIC_STATE -> onPlayMusicStateItemClick(position)
            ADD_PLAYLIST_STATE -> onAddPlaylistStateItemClick(position)
        }
    }

    override fun onLongItemClick(position: Int) {
        viewModel.setViewState(ADD_PLAYLIST_STATE)
        val currentSong = playlistAdapter.playlist.trackItems[position]
        currentSong.isSelected = true
        playlistAdapter.notifyItemChanged(position)
    }

    private fun onAddPlaylistStateItemClick(position: Int) {
        val currentSong = playlistAdapter.playlist.trackItems[position]

        currentSong.isSelected = !currentSong.isSelected
        playlistAdapter.notifyItemChanged(position)
    }

    private fun onPlayMusicStateItemClick(position: Int) {
        val currentTrack = playlistAdapter.playlist.getCurrentTrackInPlaylist()
        val indexOfCurrentTrack: Int = playlistAdapter.playlist.trackItems.indexOf(currentTrack)

        if (currentTrack != null) {
            if (indexOfCurrentTrack == position) {
                viewModel.setCurrentTrackItem(currentTrack)
                when (currentTrack.state) {
                    TrackItem.TrackState.PLAYING -> {
                        currentTrack.pauseTrack()
                        pauseMediaPlayer()
                        playlistAdapter.notifyItemChanged(indexOfCurrentTrack)
                    }

                    TrackItem.TrackState.PAUSED -> {
                        currentTrack.playTrack()
                        startMediaPlayer()
                        if (viewModel.isMediaPlayerCompleted) {
                            viewModel.isMediaPlayerCompleted = false
                        }
                        playlistAdapter.notifyItemChanged(indexOfCurrentTrack)
                    }

                    TrackItem.TrackState.STOPPED -> {
                        currentTrack.playTrack()
                        startMediaPlayer()
                        if (viewModel.isMediaPlayerCompleted) {
                            viewModel.isMediaPlayerCompleted = false
                        }
                        playlistAdapter.notifyItemChanged(indexOfCurrentTrack)
                    }

                    else -> {
                        throw IllegalStateException("Track has an inappropriate state")
                    }
                }
            } else {
                playlistAdapter.playlist.setCurrentTrackInPlaylist(position)
                viewModel.setCurrentTrackItem(playlistAdapter.playlist.getCurrentTrackInPlaylist()!!)
                playlistAdapter.playlist.trackItems[position].playTrack()
                resetMediaPlayer(playlistAdapter.playlist.getCurrentTrackInPlaylist()!!.song.id)
                startMediaPlayer()
                if (viewModel.isMediaPlayerCompleted) {
                    viewModel.isMediaPlayerCompleted = false
                }
                playlistAdapter.notifyItemChanged(position)
                playlistAdapter.notifyItemChanged(indexOfCurrentTrack)
            }
        }

    }

    override fun onControllerBtnPlayPauseButtonClick() {
        val currentTrack = playlistAdapter.playlist.getCurrentTrackInPlaylist()
        if (currentTrack != null) {
            val indexOfCurrentTrack = playlistAdapter.playlist.trackItems.indexOf(currentTrack)
            if (currentTrack.state == TrackItem.TrackState.PLAYING) {
                currentTrack.pauseTrack()
                pauseMediaPlayer()
            } else {
                currentTrack.playTrack()
                startMediaPlayer()
                if (viewModel.isMediaPlayerCompleted) {
                    viewModel.isMediaPlayerCompleted = false
                }
            }
            playlistAdapter.notifyItemChanged(indexOfCurrentTrack)
        } else {
            if (!playlistAdapter.playlist.isEmpty()) {
                playlistAdapter.playlist.setCurrentTrackInPlaylist(1)
                playlistAdapter.playlist.getCurrentTrackInPlaylist()!!.playTrack()
                startMediaPlayer()
                if (viewModel.isMediaPlayerCompleted) {
                    viewModel.isMediaPlayerCompleted = false
                }
            }
        }
    }

    override fun onControllerBtnStopButtonClick() {
        val currentTrack = playlistAdapter.playlist.getCurrentTrackInPlaylist()
        if (currentTrack != null) {
            val indexOfCurrentTrack = playlistAdapter.playlist.getIndexOfCurrentTrack()
            currentTrack.stopTrack()
            resetMediaPlayer(currentTrack.song.id)
            playlistAdapter.notifyItemChanged(indexOfCurrentTrack)
        }
    }

    override fun onAddPlaylistBtnCancelButtonClick() {
        viewModel.setViewState(PLAY_MUSIC_STATE)
        if (viewModel.currentPlayList.value != null) {
            playlistAdapter.playlist = viewModel.currentPlayList.value!!
        }
    }

    override fun onAddPlaylistBtnOkButtonClick(playlistName: String) {
        if (!playlistAdapter.playlist.isAnyItemSelected()) {
            Toast.makeText(this, "Add at least one song to your playlist", Toast.LENGTH_LONG).show()
        } else if (playlistAdapter.playlist.isAnyItemSelected() && playlistName.isEmpty()) {
            Toast.makeText(this, "Add a name to your playlist", Toast.LENGTH_LONG).show()
        } else if (playlistAdapter.playlist.isAnyItemSelected() && playlistName.isNotEmpty()) {
            if (playlistName == ALL_SONGS_PLAYLIST) {
                Toast.makeText(
                    this,
                    "All Songs is a reserved name choose another playlist name",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val songsIds = playlistAdapter.playlist.getSelectedTracksSongIDs()
                dbHelper.insertOrReplacePlaylist(playlistName, songsIds)
                playlistAdapter.playlist.unselectAllSelectedTracks()
                viewModel.setViewState(PLAY_MUSIC_STATE)
            }
        }
    }
}