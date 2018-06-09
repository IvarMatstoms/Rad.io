package tech.ivar.ra

import android.content.Context
import java.io.File

data class Station(
        @com.google.gson.annotations.SerializedName("start_time")
        val startTime: Int,
        val id: String,
        @com.google.gson.annotations.SerializedName("image")
        val imageFileId: String,
        val library: Library,
        val seed: Long,
        val name: String) {
    var randomGen: RandomGen
    lateinit var queue:Queue;
    init {
        randomGen = RandomGen(seed)

    }

    fun fastForward() {

    }

    fun getResFile(context: Context, fileId:String):File {
        val stationsDir=context.getDir("stations", Context.MODE_PRIVATE)
        val stationDir= File(stationsDir,id)
        return File(File(stationDir, "res"),fileId)


    }

}
