package tech.ivar.radio


import android.app.DownloadManager
import android.content.Context
import android.support.annotation.IntegerRes
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.*
import com.github.kittinunf.fuel.core.response
import com.github.kittinunf.fuel.httpGet
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import tech.ivar.ra.Station
import java.io.File
import java.security.AccessController.getContext
import java.util.*

class StationIndex {
    init {
        //loadIndex(c)
    }
    var stations: MutableList<StationReference> = mutableListOf()
    init {

    }

    fun loadIndex(context:Context) {
        if (!("index.json" in context.fileList())){
            saveIndex(context)
        }
        val gson = GsonBuilder().create()
        val file=context.openFileInput("index.json")
        val stationsString=String(file.readBytes())
        stations=gson.fromJson(stationsString, object : TypeToken<List<StationReference>>() {}.type);
    }

    fun downloadFile(context:Context, url:String){

        url.httpGet().response{request, response, result   ->

            val (bytes, error) = result
            if (bytes != null) {
                //println(bytes)
                addStation(context,bytes)
            }
        }
    }
    fun addStation(context: Context, data: ByteArray) {
        Log.w("B","DONNNE")
        var uniqueID = UUID.randomUUID().toString()
        val gson = GsonBuilder().create()
        //val directory = context.filesDir.mkdir()
        var offset:Int=0
        var manifestSize:Int=bytesToInt(data.copyOfRange(offset,offset+4))
        offset+=4
        Log.w("B",manifestSize.toString())
        var manifestString=String(data.copyOfRange(offset,offset+manifestSize))
        offset+=manifestSize
        var fileIndexSize:Int=bytesToInt(data.copyOfRange(offset,offset+4))
        Log.w("B", data.copyOfRange(offset-1,offset+4-1)[0].toString())
        Log.w("B",offset.toString())
        offset+=4
        var fileIndex=String(data.copyOfRange(offset,offset+fileIndexSize))
        offset+=fileIndexSize
        var base_dir=context.getDir("stations", Context.MODE_PRIVATE)
        Log.w("B",fileIndexSize.toString())
        Log.w("B",manifestString)
        Log.w("B",fileIndex)
        val dir=File(base_dir,uniqueID)
        dir.mkdir()
        val resDir=File(dir,"res")
        resDir.mkdir()
        var files: Map<String, ArchiveFile> = gson.fromJson(fileIndex, object : TypeToken<Map<String,ArchiveFile>>() {}.type);
        files.values.forEach {
            Log.w("A",it.toString())
            val fileId:String=UUID.randomUUID().toString()
            val file:File=File(resDir, fileId)
            file.writeBytes(data.copyOfRange(offset+it.startByte,offset+it.startByte+it.size))
            it.id=fileId
        }
        val getFileId: (String) -> String = {files.get(it)!!.id!!}
        val fileIndexById: Map<String,ArchiveFile> =files.values.map { it.id!! to it }.toMap()
        val fileIndexByIdString:String =gson.toJson(fileIndexById)
        val fileIndexByIdFile:File=File(dir, "file_index.json")
        fileIndexByIdFile.writeText(fileIndexByIdString)
        val manifest: StationData=gson.fromJson(manifestString, StationData::class.java)
        manifest.image=getFileId(manifest.image)
        manifest.id=uniqueID
        manifest.library.artists.forEach({it.albums.forEach({it.tracks.forEach{
            it.fileId=getFileId(it.id)
        }})})
        val newManifestString:String =gson.toJson(manifest)
        val manifestFile:File=File(dir, "manifest.json")
        manifestFile.writeText(newManifestString)
        Log.w("S",manifest.toString())

        //data[0].to
        //map multi with xx xx 256 1 vektor
        //var dir=context.getDir("stations", Context.MODE_PRIVATE)
        /*if (!dir.exists()) {
            dir.mkdir()
        }*/
        //var f=File(dir, uniqueID)
        //f.writeBytes(data)

        val station=StationReference(uniqueID, manifest.name)
        stations.add(station)
        saveIndex(context)

    }

    fun bytesToInt(byteArray: ByteArray):Int {
        //val = if(b) {3 } else {5}
        return byteArray.mapIndexed { index, byte ->  (if(byte.toInt()>=0){byte.toInt()}else{256+byte.toInt()} shl (3-index)*8)}.sum()
    }

    fun saveIndex(context:Context) {
        val gson = GsonBuilder().create()
        val fileContents =  gson.toJson(stations)


        context.openFileOutput("index.json", Context.MODE_PRIVATE).use {
            it.write(fileContents.toByteArray())
        }
    }


}

data class StationData(
        @com.google.gson.annotations.SerializedName("start_time")
        var StartTime:Int,
        var seed:Long,
        var name:String,
        var image:String,
        var library:Library,
        var id:String?


)
data class Library(var artists:List<Artist>)

data class Artist(val name:String, val id:String, val albums:List<Album>)

data class Album(val name:String, val id:String, val tracks:List<Track>)

data class Track(
        val name:String,
        val id:String,
        val length:Int,
        @com.google.gson.annotations.SerializedName("file_id")
        var fileId:String?) {
    //var fileId:String?=null
}

data class StationReference(var id:String, var name:String) {}

data class ArchiveFile(var key:String,
                  @com.google.gson.annotations.SerializedName("start_byte")
                  var startByte:Int,
                  var size:Int,
                  @com.google.gson.annotations.SerializedName("file_type")
                  var fileType:String)
{
    /*override fun toString(): String {
        return "$key.$fileType $startByte:$size"
    }*/
    var id: String?=null

}

private var stationIndex: StationIndex? = null;
fun getStationIndex():StationIndex {
    if(stationIndex == null) {
        stationIndex = StationIndex()
    }
    return stationIndex!!
}