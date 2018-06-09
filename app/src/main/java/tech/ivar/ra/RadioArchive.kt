package tech.ivar.ra

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

class RadioArchive {

}

fun loadRaFile(context: Context, id: String): Station {
    val base_dir = context.getDir("stations", Context.MODE_PRIVATE)
    val dir = File(base_dir, id)
    val manifestFile: File = File(dir, "manifest.json")
    val manifestString = String(manifestFile.readBytes())
    val gson = GsonBuilder().create()
    val station: Station = gson.fromJson(manifestString, object : TypeToken<Station>() {}.type);
    Log.w("R", station.toString())
    val queueItems: MutableList<QueueItem> = mutableListOf()

    station.library.artists.forEach {
        it.albums.forEach {
            queueItems.addAll(it.tracks)
        }
    }
    Log.w("S","START TIME ${station.startTime}")
    station.queue= Queue(RandomGen(station.seed), queueItems, station.startTime)
    return station
}