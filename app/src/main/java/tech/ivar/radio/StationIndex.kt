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
import tech.ivar.ra.Queue
import tech.ivar.ra.QueueItem
import tech.ivar.ra.RandomGen
import tech.ivar.ra.Station


private const val CHANNEL_ID = "tech.ivar.radio.dchannel"


class StationIndex {
    lateinit var storageLocations:Map<String, StorageInterface>
    var indexVersionId: Int=0
    init {
        //loadIndex(c)
    }

    var stations: MutableList<StationReference> = mutableListOf()

    init {

    }

    fun updateIndexVersionId() {
        indexVersionId++
    }

    fun loadIndexes(context: Context) {
        storageLocations=listOf(StorageInternal(),StorageExternal()).filter { it.isAvailable() }.associateBy { it.id }
        stations= mutableListOf()
        storageLocations.forEach {
            id, storageLocation ->
            loadIndex(context,storageLocation)
        }

        stations.sortBy { it.position }

        var correct=true
        stations.forEach {
            Log.w("A",it.position.toString())
            if (it.position == null) {
                it.position=0
                correct=false
            }
            Log.w("Q",it.storageLocation.toString())
        }
        if (!correct) {
            //Log.w("W","SAVEING")
            saveIndex(context)
        }
    }

    fun loadIndex(context: Context, storageLocation: StorageInterface) {
        val file:File=storageLocation.getIndexFile(context)
        if (!file.exists()) {
            //saveIndex(context)
            createIndex(file)
        }
        val gson = GsonBuilder().create()
        //val file = context.openFileInput("index.json")
        val stationsString = String(file.readBytes())
        val storageStations:List<StationReference> = gson.fromJson(stationsString, object : TypeToken<List<StationReference>>() {}.type)
        storageStations.forEach({
            it.storageLocation=storageLocation
        })
        stations.addAll(storageStations)


    }

    fun createIndex(file:File) {
        file.writeText("[]")
    }

    fun saveIndex(context: Context) {
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
        reassignPositions()
        val storageLocationsStations:Map<String, MutableList<StationReference>> = storageLocations.map {it.key to mutableListOf<StationReference>()}.toMap()
        Log.w("S","SSSSS")
        Log.w("S",storageLocationsStations.toString())
        stations.forEach {
            Log.w("S",it.storageLocation.toString())
            storageLocationsStations[it.storageLocation!!.id]!!.add(it)
        }
        storageLocations.values.forEach {
            val fileContents = gson.toJson(storageLocationsStations[it.id])

            val file:File=it.getIndexFile(context)
            file.writeText(fileContents)
        }
        updateIndexVersionId()

    }

    fun swapStations(context: Context, from: Int, to: Int) {
        stations[from].position=to
        if (from < to) {
            stations.subList(from+1, to+1).forEach {
                it.position= it.position!! - 1
            }
        } else if (to < from) {
            stations.subList(to, from).forEach {
                it.position= it.position!! + 1
            }
        }
        saveIndex(context)
    }

    fun deleteStationByPosition(context: Context, position: Int) {
        val station=stations[position]
        val stationId:String= station.id
        val baseDir = station.storageLocation!!.getStationsDir(context)
        val dir = File(baseDir, stationId)
        dir.deleteRecursively()

        stations.removeAt(position)
        saveIndex(context)
    }


    private fun reassignPositions() {
        stations.sortBy { it.position }
        //stations.
        stations.forEachIndexed({index: Int, stationReference: StationReference ->
            stationReference.position=index
        })
    }

    fun loadStation(context: Context, id: String): Station {
        val stationReference=stations.filter { it.id==id }[0]
        //val base_dir = context.getDir("stations", Context.MODE_PRIVATE)
        val baseDir=stationReference.storageLocation!!.getStationsDir(context)
        val dir = File(baseDir, id)
        val manifestFile: File = File(dir, "manifest.json")
        val manifestString = String(manifestFile.readBytes())
        val gson = GsonBuilder().create()
        val station: Station = gson.fromJson(manifestString, object : TypeToken<Station>() {}.type);
        Log.w("R", station.toString())
        val queueItems: MutableList<QueueItem> = mutableListOf()
        station.library.updateReferences()
        station.library.artists.forEach {
            it.albums.forEach {
                queueItems.addAll(it.tracks)
            }
        }
        Log.w("S","START TIME ${station.startTime}")
        station.queue= Queue(RandomGen(station.seed), queueItems, station.startTime)
        station.setStorageLocation(stationReference.storageLocation!!)
        return station
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
            val method: String = intent.getStringExtra("method")
            val storageLocationId = intent.getStringExtra("storage_location")
            val storageLocation = getStationIndex().storageLocations[storageLocationId]!!
            //Log.w("S",storageLocation?.name)
            if (method=="ra") {
                thread {
                    downloadRa(url, storageLocation)
                    broadcastUpdate("download_done")
                    reloadStationsList()
                }
            } else {
                thread {
                    downloadFolder(url, storageLocation)
                    broadcastUpdate("download_done")
                    reloadStationsList()
                }
            }
        }
        return START_NOT_STICKY

    }

    fun downloadRa(url: String, storageLocation: StorageInterface) {
        notificationCreate()

        url.httpGet().response { request, response, result ->

            val (bytes, error) = result
            if (bytes != null) {
                //println(bytes)
                addStationRa(bytes, storageLocation)
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




    fun downloadFolder(url_: String, storageLocation: StorageInterface) {
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

        val uniqueID = UUID.randomUUID().toString()
        val baseDir = storageLocation!!.getStationsDir(this)

        //val baseDir = this.getDir("stations", Context.MODE_PRIVATE)

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
        val files: Map<String, FolderFile> = gson.fromJson(fileIndexString, object : TypeToken<Map<String, FolderFile>>() {}.type);

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
        /*
        notificationStatus("Fetching thumbnail")
        Log.w("E","${url}image_thumbnail")
        val (request, response, result) = ("${url}image_thumbnail").httpGet().response()
        val thumbnailFile = File(baseDir, "thumbnail")
        val (data, error) = result
        if (error != null) {
            abort("Thumbnail http error")
            return
        }
        thumbnailFile.writeBytes(data!!)
        */


        notificationStatus("Finishing up")
        //Log.w("A",files.toString())
        val getFileId: (String) -> String = { files[it]!!.id!! }

        val fileIndexById: Map<String, FolderFile> = files.values.map { it.id!! to it }.toMap()
        val fileIndexByIdString: String = gson.toJson(fileIndexById)

        val fileIndexByIdFile: File = File(dir, "file_index.json")

        fileIndexByIdFile.writeText(fileIndexByIdString)
        //val manifest: StationData=gson.fromJson(manifestString, StationData::class.java)
        manifest.image = getFileId(manifest.image)
        manifest.imageThumbnail = getFileId(manifest.imageThumbnail)
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

        val station = StationReference(uniqueID, manifest.name, 0, manifest.imageThumbnail)
        station.storageLocation=storageLocation
        getStationIndex().stations.add(station)
        getStationIndex().saveIndex(this)

        notificationDone()
    }

    fun addStationRa(data: ByteArray, storageLocation: StorageInterface) {
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
        val baseDir = storageLocation!!.getStationsDir(this)

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
        //var base_dir = this.getDir("stations", Context.MODE_PRIVATE)
        Log.w("B", fileIndexSize.toString())
        Log.w("B", manifestString)
        Log.w("B", fileIndex)
        val dir = File(baseDir, uniqueID)
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
        manifest.imageThumbnail = getFileId(manifest.imageThumbnail)

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

        val station = StationReference(uniqueID, manifest.name, 0, manifest.imageThumbnail)
        station.storageLocation=storageLocation
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
        @com.google.gson.annotations.SerializedName("image_thumbnail")
        var imageThumbnail: String,
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

data class StationReference(
        @com.google.gson.annotations.Expose
        var id: String,
        @com.google.gson.annotations.Expose
        var name: String,
        @com.google.gson.annotations.Expose
        var position: Int?,
        @com.google.gson.annotations.Expose
        var imageThumbnailId: String
        ) {
    var storageLocation:StorageInterface?=null
    fun getThumbnailFile(context: Context):File {
        return File(File(File(storageLocation!!.getStationsDir(context),id),"res"),imageThumbnailId)
    }
}


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