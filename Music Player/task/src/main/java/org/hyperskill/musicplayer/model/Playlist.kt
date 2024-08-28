package org.hyperskill.musicplayer.model

class Playlist(val name: String, trackItems: List<TrackItem>) {
    private val _trackItems: MutableList<TrackItem> = trackItems.toMutableList()
    val trackItems: List<TrackItem> get() = _trackItems

    fun addTrackItem(trackItem: TrackItem){
        _trackItems.add(trackItem)
    }

    fun addAllTrackItems(trackItems: List<TrackItem>){
        _trackItems.addAll(trackItems)
    }

    fun removeTrackItem(trackItem: TrackItem){
        _trackItems.remove(trackItem)
    }

    fun checkIfThereIsCurrentTrack(): Boolean{
        for(track in _trackItems){
            if(track.isCurrentTrack){
                return true
            }
        }
        return false
    }

    fun getCurrentTrackInPlaylist(): TrackItem?{
        for(track in _trackItems){
            if(track.isCurrentTrack){
                return track
            }
        }
        return null
    }

    fun getIndexOfCurrentTrack(): Int{
        return _trackItems.indexOfFirst { it.isCurrentTrack }
    }

    fun setCurrentTrackInPlaylist(trackItem: TrackItem){
        val oldCurrentTrackItem: TrackItem? = getCurrentTrackInPlaylist()
        if(oldCurrentTrackItem != null){
            oldCurrentTrackItem.isCurrentTrack = false
            oldCurrentTrackItem.stopTrack()
        }
        trackItem.isCurrentTrack = true
    }

    fun setCurrentTrackInPlaylist(trackItemIndex: Int){
        val oldCurrentTrackItem: TrackItem? = getCurrentTrackInPlaylist()
        if(oldCurrentTrackItem != null){
            oldCurrentTrackItem.isCurrentTrack = false
            oldCurrentTrackItem.stopTrack()
        }
        for(index in trackItems.indices){
            if(index == trackItemIndex){
                trackItems[index].isCurrentTrack = true
            }
        }
    }

    fun getSelectedTracks(): List<TrackItem> {
        val selectedTracks = mutableListOf<TrackItem>()
        for(track in _trackItems){
            if(track.isSelected){
                selectedTracks.add(track)
            }
        }
        return selectedTracks
    }

    fun getSelectedTracksSongIDs(): List<Long>{
        val tracks = getSelectedTracks()
        return tracks.map { it.song.id }
    }

    fun unselectAllSelectedTracks(){
        for(track in _trackItems){
            if(track.isSelected){
                track.isSelected = false
            }
        }
    }

    fun isAnyItemSelected(): Boolean{
        for(track in _trackItems){
            if(track.isSelected){
                return true
            }
        }
        return false
    }

    fun isEmpty():Boolean{
        return trackItems.isEmpty()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Playlist

        if (name != other.name) return false
        if (_trackItems != other._trackItems) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + _trackItems.hashCode()
        return result
    }

    override fun toString(): String {
        return "Playlist(name='$name', _trackItems=$_trackItems)"
    }

}