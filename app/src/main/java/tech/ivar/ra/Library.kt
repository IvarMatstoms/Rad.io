package tech.ivar.ra

data class Library(
        val artists: List<Artist>
) {
    fun updateReferences() {
        for (arist in artists) {
            for (album in arist.albums) {
                album.artist=arist
                for (track in album.tracks) {
                    track.album=album
                    track.artist=arist
                }
            }
        }
    }

}

data class Artist(
        val name: String,
        val id: String,
        val albums: List<Album>
) {
}

data class Album(
        val name: String,
        val id: String,
        val tracks: List<Track>
){
    lateinit var artist:Artist
}

class Track(
        val name: String,
        id: String,
        @com.google.gson.annotations.SerializedName("file_id")
        val fileId: String,
        length: Int

) : QueueItem(
        length,
        id
) {
    lateinit var album:Album
    lateinit var artist:Artist

    override fun toString(): String {
        return "$id:$name"
    }

    override fun getItems(): List<Track> {
        return listOf(this)
    }
}