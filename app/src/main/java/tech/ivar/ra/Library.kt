package tech.ivar.ra

data class Library (
        val artists: List<Artist>
){

}

data class Artist(
        val name: String,
        val id: String,
        val albums: List<Album>
){
}

data class Album (
        val name: String,
        val id: String,
        val tracks: List<Track>
)

class Track (
        val name:String,
        id: String,
        @com.google.gson.annotations.SerializedName("file_id")
        val fileId: String,
        length: Int

) : QueueItem(
        length,
        id
) {
        override fun toString(): String {
                return "$id:$name"
        }
        override fun getItems(): List<Track> {
                return listOf(this)
        }
}