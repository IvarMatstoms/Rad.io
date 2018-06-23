package tech.ivar.radio


import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.github.kittinunf.fuel.httpGet
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.*
import kotlin.concurrent.thread
import android.app.NotificationManager
import android.support.v4.app.NotificationCompat
import android.app.NotificationChannel
import android.os.Build
import android.support.v4.app.NotificationManagerCompat


private const val CHANNEL_ID = "tech.ivar.radio.dchannel"


class StationIndex {
    init {
        //loadIndex(c)
    }

    var stations: MutableList<StationReference> = mutableListOf()

    init {

    }

    fun loadIndex(context: Context) {
        if (!("index.json" in context.fileList())) {
            saveIndex(context)
        }
        val gson = GsonBuilder().create()
        val file = context.openFileInput("index.json")
        val stationsString = String(file.readBytes())
        stations = gson.fromJson(stationsString, object : TypeToken<List<StationReference>>() {}.type);

    }

    fun fromUrl(context: Context, url: String) {
        val intent: Intent = Intent(context, DownloaderService::class.java)
        //intent.putExtra("firstTrackUri", trackUri.toString())
        intent.putExtra("url", url)
        intent.action = "download"
        context.startService(intent)
    }

    fun downloadFolder() {

    }


    fun saveIndex(context: Context) {
        val gson = GsonBuilder().create()
        val fileContents = gson.toJson(stations)


        context.openFileOutput("index.json", Context.MODE_PRIVATE).use {
            it.write(fileContents.toByteArray())
        }
    }


}

class DownloaderService() : Service() {
    var mBuilder: NotificationCompat.Builder? = null;
    var notification: DownloadNotification?=null;
    override fun onBind(p0: Intent?): IBinder? {
        Log.w("P", "BIND")
        return null
    }

    fun broadcastUpdate(status:String) {
        Log.w("U","UPDATE SENT")
        val intent = Intent()
        intent.action = MAIN_ACTIVITY_ACTION
        intent.putExtra("status", status)
        sendBroadcast(intent)
    }

    private fun reloadStationsList () {
        Log.w("U","UPDATE SENT")
        val intent = Intent()
        intent.action = STATIONS_FRAGMENT_ACTION
        intent.putExtra("status", "reload")
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.w("A", intent.action)
        if (intent.action == "download") {
            val url: String = intent.getStringExtra("url")
            if (url.endsWith(".ra")) {
                thread {
                    downloadRa(url)
                    broadcastUpdate("download_done")
                    reloadStationsList()
                }
            } else {
                thread {
                    downloadFolder(url)
                    broadcastUpdate("download_done")
                    reloadStationsList()
                }
            }
        }
        return START_NOT_STICKY

    }

    fun downloadRa(url: String) {
        notificationCreate()

        url.httpGet().response { request, response, result ->

            val (bytes, error) = result
            if (bytes != null) {
                //println(bytes)
                addStationRa(bytes)
                notificationDone()
            } else {
                Log.w("W", error.toString())
            }
        }

    }

    fun notificationCreate() {
        notification= DownloadNotification()
        notification!!.create(this)
        notification?.text="Downloading station"
        notification?.update(this)

    }

    fun notificationDone () {
        notification?.showProgress=false
        notification?.text="Download complete"
        notification?.ongoing=false
        notification?.update(this)

    }

    fun notificationProgress (progress:Int){
        notification?.progress=progress
        notification?.update(this)

    }
    fun notificationStatus (status:String){
        notification?.text="Downloading station: $status"
        notification?.update(this)

    }

    fun notificationText (status:String){
        notification?.text=status
        notification?.update(this)

    }




    fun downloadFolder(url_: String) {
        fun getTextFile(url: String): String? {

            val (request, response, result) = url.httpGet().responseString()
            val (data, error) = result
            return if (error == null) {
                data
            } else {
                null
            }
        }

        fun abort(error: String) {
            notificationDone()
            notificationText("Download failed: $error")
            broadcastUpdate("download_failed")
        }

        var uniqueID = UUID.randomUUID().toString()

        var baseDir = this.getDir("stations", Context.MODE_PRIVATE)

        notificationCreate()
        notificationStatus("Downloading manifest")
        val url: String = if (url_.endsWith("/")) {
            url_
        } else {
            url_ + "/"
        }
        val manifestString = getTextFile(url + "manifest.json")
        if (manifestString == null) {
            abort("http error")
            return
        }
        notificationStatus("Downloading file index")
        val fileIndexString = getTextFile(url + "file_index.json")
        if (fileIndexString == null) {
            abort("http error")
            return
        }
        notificationStatus("Parsing")
        val gson = GsonBuilder().create()
        val manifest: StationData = gson.fromJson(manifestString, StationData::class.java)
        Log.w("M", fileIndexString)
        var files: Map<String, FolderFile> = gson.fromJson(fileIndexString, object : TypeToken<Map<String, FolderFile>>() {}.type);

        val dir = File(baseDir, uniqueID)
        dir.mkdir()
        val resDir = File(dir, "res")
        resDir.mkdir()
        val filesSize = files.size
        val progress = 0
        files.values.forEachIndexed { index, archiveFile ->

            notificationStatus("Downloading file (${index+1}/${filesSize})")
            val (request, response, result) = ("${url}files/${archiveFile.key}").httpGet().response()
            val (data, error) = result
            if (error != null) {
                abort("http error")
                return
            }
            val fileId: String = UUID.randomUUID().toString()
            val file: File = File(resDir, fileId)
            file.writeBytes(data!!)
            archiveFile.id = fileId
            //archiveFile.key="ABC"
            val progress = (((index.toFloat()+1) / filesSize.toFloat()) * 100).toInt()
            notificationProgress(progress)

        }
        notificationStatus("Finishing up")
        //Log.w("A",files.toString())
        val getFileId: (String) -> String = { files[it]!!.id!! }

        val fileIndexById: Map<String, FolderFile> = files.values.map { it.id!! to it }.toMap()
        val fileIndexByIdString: String = gson.toJson(fileIndexById)

        val fileIndexByIdFile: File = File(dir, "file_index.json")

        fileIndexByIdFile.writeText(fileIndexByIdString)
        //val manifest: StationData=gson.fromJson(manifestString, StationData::class.java)
        manifest.image = getFileId(manifest.image)
        manifest.id = uniqueID
        manifest.library.artists.forEach({
            it.albums.forEach({
                it.tracks.forEach {
                    it.fileId = getFileId(it.fileId!!)
                }
            })
        })
        val newManifestString: String = gson.toJson(manifest)
        val manifestFile: File = File(dir, "manifest.json")
        manifestFile.writeText(newManifestString)
        //Log.w("M", manifest.toString())

        val station = StationReference(uniqueID, manifest.name)
        getStationIndex().stations.add(station)
        getStationIndex().saveIndex(this)

        notificationDone()
    }

    fun addStationRa(data: ByteArray) {
        fun bytesToInt(byteArray: ByteArray): Int {
            //val = if(b) {3 } else {5}
            return byteArray.mapIndexed { index, byte ->
                (if (byte.toInt() >= 0) {
                    byte.toInt()
                } else {
                    256 + byte.toInt()
                } shl (3 - index) * 8)
            }.sum()
        }

        var uniqueID = UUID.randomUUID().toString()
        val gson = GsonBuilder().create()
        //val directory = context.filesDir.mkdir()
        var offset: Int = 0
        var manifestSize: Int = bytesToInt(data.copyOfRange(offset, offset + 4))
        offset += 4
        var manifestString = String(data.copyOfRange(offset, offset + manifestSize))
        offset += manifestSize
        var fileIndexSize: Int = bytesToInt(data.copyOfRange(offset, offset + 4))
        offset += 4
        var fileIndex = String(data.copyOfRange(offset, offset + fileIndexSize))
        offset += fileIndexSize
        var base_dir = this.getDir("stations", Context.MODE_PRIVATE)
        Log.w("B", fileIndexSize.toString())
        Log.w("B", manifestString)
        Log.w("B", fileIndex)
        val dir = File(base_dir, uniqueID)
        dir.mkdir()
        val resDir = File(dir, "res")
        resDir.mkdir()
        var files: Map<String, ArchiveFile> = gson.fromJson(fileIndex, object : TypeToken<Map<String, ArchiveFile>>() {}.type);
        files.values.forEach {
            Log.w("A", it.toString())
            val fileId: String = UUID.randomUUID().toString()
            val file: File = File(resDir, fileId)
            file.writeBytes(data.copyOfRange(offset + it.startByte, offset + it.startByte + it.size))
            it.id = fileId
        }
        val getFileId: (String) -> String = { files.get(it)!!.id!! }
        val fileIndexById: Map<String, ArchiveFile> = files.values.map { it.id!! to it }.toMap()
        val fileIndexByIdString: String = gson.toJson(fileIndexById)
        val fileIndexByIdFile: File = File(dir, "file_index.json")
        fileIndexByIdFile.writeText(fileIndexByIdString)
        val manifest: StationData = gson.fromJson(manifestString, StationData::class.java)
        manifest.image = getFileId(manifest.image)
        manifest.id = uniqueID
        manifest.library.artists.forEach({
            it.albums.forEach({
                it.tracks.forEach {
                    it.fileId = getFileId(it.id)
                }
            })
        })
        val newManifestString: String = gson.toJson(manifest)
        val manifestFile: File = File(dir, "manifest.json")
        manifestFile.writeText(newManifestString)
        Log.w("S", manifest.toString())

        //data[0].to
        //map multi with xx xx 256 1 vektor
        //var dir=context.getDir("stations", Context.MODE_PRIVATE)
        /*if (!dir.exists()) {
            dir.mkdir()
        }*/
        //var f=File(dir, uniqueID)
        //f.writeBytes(data)

        val station = StationReference(uniqueID, manifest.name)
        getStationIndex().stations.add(station)
        getStationIndex().saveIndex(this)

    }
}

class DownloadNotification {
    var mBuilder: NotificationCompat.Builder?=null

    var text:String=""
    var ongoing:Boolean=true
    private var _progress:Int=0
    var showProgress:Boolean=false

    init{


    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.dchannel_name)
            val description = context.getString(R.string.dchannel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    var progress: Int
    get() =_progress
    set(value) {
        _progress=value
        showProgress=true
    }

    fun create(context: Context) {
        text="Downloading station"
        ongoing=true
        _progress=0
        showProgress=false
        createNotificationChannel(context)
    }


    fun update (context: Context) {
        mBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play_arrow_black_24dp)
                .setContentTitle(text)
                .setOngoing(ongoing)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (showProgress) {
            //Log.w("P",progress.toString())
            mBuilder?.setProgress(100, progress, false)
        }
        val notificationManager = NotificationManagerCompat.from(context)

        notificationManager.notify(1, mBuilder!!.build())
    }
}

data class StationData(
        @com.google.gson.annotations.SerializedName("start_time")
        var StartTime: Int,
        var seed: Long,
        var name: String,
        var image: String,
        var library: Library,
        var id: String?


)

data class Library(var artists: List<Artist>)

data class Artist(val name: String, val id: String, val albums: List<Album>)

data class Album(val name: String, val id: String, val tracks: List<Track>)

data class Track(
        val name: String,
        val id: String,
        val length: Int,
        @com.google.gson.annotations.SerializedName("file_id")
        var fileId: String?) {
    //var fileId:String?=null
}

data class StationReference(var id: String, var name: String) {}


data class FolderFile(var key: String,
                      @com.google.gson.annotations.SerializedName("file_type")
                      var fileType: String,
                      var size: Int) {
    var id: String? = null

}

data class ArchiveFile(var key: String,
                       @com.google.gson.annotations.SerializedName("start_byte")
                       var startByte: Int,
                       var size: Int,
                       @com.google.gson.annotations.SerializedName("file_type")
                       var fileType: String) {
    /*override fun toString(): String {
        return "$key.$fileType $startByte:$size"
    }*/
    var id: String? = null

}

private var stationIndex: StationIndex? = null;
fun getStationIndex(): StationIndex {
    if (stationIndex == null) {
        stationIndex = StationIndex()
    }
    return stationIndex!!
}


/*
   fun notificationDone() {
        if (mBuilder != null) {
            val notificationManager = NotificationManagerCompat.from(this)
            mBuilder!!.setOngoing(false)
                    .setProgress(0, 100, false)
                    .setContentTitle("Download done")
            notificationManager.notify(1, mBuilder!!.build());
        }
    }

    fun notificationStatus(status: String) {
        if (mBuilder != null) {
            val notificationManager = NotificationManagerCompat.from(this)
            mBuilder!!.setOngoing(false)
                    .setContentTitle("Downloading station:$status")
            notificationManager.notify(1, mBuilder!!.build());
        }
    }

    fun notificationProgress(progress: Int) {
        if (mBuilder != null) {
            Log.w("P",progress.toString())
            val notificationManager = NotificationManagerCompat.from(this)
            mBuilder!!.setProgress(0, progress, false)
            notificationManager.notify(1, mBuilder!!.build());
        }
    }

    fun notificationCreate() {
        createNotificationChannel()
        mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play_arrow_black_24dp)
                .setContentTitle("Downloading station")
                .setProgress(33, 100, false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationManager = NotificationManagerCompat.from(this)

        notificationManager.notify(1, mBuilder!!.build())

    }
 */