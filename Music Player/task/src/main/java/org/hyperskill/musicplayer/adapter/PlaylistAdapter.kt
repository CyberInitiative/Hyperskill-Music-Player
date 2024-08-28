package org.hyperskill.musicplayer.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.hyperskill.musicplayer.R
import org.hyperskill.musicplayer.model.Playlist
import org.hyperskill.musicplayer.model.TrackItem
import org.hyperskill.musicplayer.viewmodel.PLAY_MUSIC_STATE

class PlaylistAdapter(
    var playlist: Playlist,
    private val listener: OnListItemSongViewHolderActionListener,
    var listState: Int = PLAY_MUSIC_STATE
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_PLAY_MUSIC = 0
        const val TYPE_SELECT_SONG = 1
    }

    class ListItemSongViewHolder(
        itemView: View, private val listener: OnListItemSongViewHolderActionListener
    ) : RecyclerView.ViewHolder(itemView) {
        val songItemImgBtnPlayPause: ImageButton =
            itemView.findViewById(R.id.songItemImgBtnPlayPause)
        val songItemTvArtist: TextView = itemView.findViewById(R.id.songItemTvArtist)
        val songItemTvTitle: TextView = itemView.findViewById(R.id.songItemTvTitle)
        val songItemTvDuration: TextView = itemView.findViewById(R.id.songItemTvDuration)

        init {
            songItemImgBtnPlayPause.setOnClickListener {
                listener.onClick(adapterPosition)
            }

            itemView.setOnLongClickListener {
                listener.onLongItemClick(adapterPosition)
                true
            }
        }
    }

    class ListItemSongSelectorViewHolder(
        itemView: View,
        private val listener: OnListItemSongViewHolderActionListener
    ) : RecyclerView.ViewHolder(itemView) {
        val songSelectorItemCheckBox: CheckBox =
            itemView.findViewById(R.id.songSelectorItemCheckBox)
        val songSelectorItemTvArtist: TextView =
            itemView.findViewById(R.id.songSelectorItemTvArtist)
        val songSelectorItemTvTitle: TextView = itemView.findViewById(R.id.songSelectorItemTvTitle)
        val songSelectorItemTvDuration: TextView =
            itemView.findViewById(R.id.songSelectorItemTvDuration)

        init {
            itemView.setOnClickListener {
                listener.onClick(adapterPosition)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (listState) {
            PLAY_MUSIC_STATE -> TYPE_PLAY_MUSIC
            else -> TYPE_SELECT_SONG
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View

        return when (viewType) {
            TYPE_PLAY_MUSIC -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_song, parent, false)
                ListItemSongViewHolder(view, listener)
            }

            TYPE_SELECT_SONG -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_song_selector, parent, false)
                ListItemSongSelectorViewHolder(view, listener)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val trackItem = playlist.trackItems[position]
        when (holder) {
            is ListItemSongViewHolder -> {
                onListItemSongViewHolder(holder, trackItem)
            }

            is ListItemSongSelectorViewHolder -> {
                onListItemSongSelectorViewHolder(holder, trackItem)
            }

            else -> {
                throw ClassCastException("Expected ListItemSongViewHolder or ListItemSongSelectorViewHolder, but received ${holder::class.simpleName}")
            }
        }
    }

    private fun getDuration(trackItem: TrackItem, textView: TextView){
        val minutesAndSeconds = trackItem.getSongDurationMinutesAndSeconds()
        val minutes = minutesAndSeconds.first
        val seconds = minutesAndSeconds.second
        textView.text = textView.resources.getString(R.string.song_duration_time, minutes, seconds)
    }

    private fun onListItemSongViewHolder(holder: ListItemSongViewHolder, trackItem: TrackItem) {
        val trackItemSong = trackItem.song
        holder.songItemTvArtist.text = trackItemSong.artist
        holder.songItemTvTitle.text = trackItemSong.title

        getDuration(trackItem, holder.songItemTvDuration)

        if (trackItem.isCurrentTrack) {
            if (trackItem.state == TrackItem.TrackState.PLAYING) {
                holder.songItemImgBtnPlayPause.setImageResource(R.drawable.ic_pause)
            } else if (trackItem.state == TrackItem.TrackState.PAUSED) {
                holder.songItemImgBtnPlayPause.setImageResource(R.drawable.ic_play)
            }
        } else {
            holder.songItemImgBtnPlayPause.setImageResource(R.drawable.ic_play)
        }
    }

    private fun onListItemSongSelectorViewHolder(
        holder: ListItemSongSelectorViewHolder,
        trackItem: TrackItem
    ) {
        val trackItemSong = trackItem.song
        holder.songSelectorItemTvArtist.text = trackItemSong.artist
        holder.songSelectorItemTvTitle.text = trackItemSong.title

        getDuration(trackItem, holder.songSelectorItemTvDuration)

        if (trackItem.isSelected) {
            holder.songSelectorItemCheckBox.isChecked = true
            holder.itemView.setBackgroundColor(Color.LTGRAY)
        } else {
            holder.songSelectorItemCheckBox.isChecked = false
            holder.itemView.setBackgroundColor(Color.WHITE)
        }
    }

    override fun getItemCount(): Int {
        return playlist.trackItems.size
    }

    interface OnListItemSongViewHolderActionListener {
        fun onClick(position: Int)
        fun onLongItemClick(position: Int)
    }

}