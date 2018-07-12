package tech.ivar.ra

import android.content.Context
import tech.ivar.radio.StorageInterface
import java.io.File

data class Station(
        @com.google.gson.annotations.SerializedName("start_time")
        val startTime: Int,
        val id: String,
        @com.google.gson.annotations.SerializedName("image")
        val imageFileId: String,
        @com.google.gson.annotations.SerializedName("image_thumbnail")
        val imageThumbnailFileId: String,
        val library: Library,
        val seed: Long,
        val name: String) {
    var randomGen: RandomGen
    lateinit var queue:Queue
    private var storageLocation:StorageInterface? = null
    init {
        randomGen = RandomGen(seed)

    }

    fun setStorageLocation(storageLocation_: StorageInterface) {
        storageLocation=storageLocation_
    }

    fun getResFile(context: Context, fileId:String):File {
        //val stationsDir=context.getDir("stations", Context.MODE_PRIVATE)
        val stationsDir=storageLocation!!.getStationsDir(context)
        val stationDir= File(stationsDir,id)
        return File(File(stationDir, "res"),fileId)


    }

}
