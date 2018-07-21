package tech.ivar.radio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import com.github.kittinunf.fuel.httpGet
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

private val ID_REGEX="[^A-Za-z0-9\\-_,]".toRegex()
private const val CHANNEL_ID = "tech.ivar.radio.rchannel"


class RepoIndex {
    var repos: MutableList<RepoReference> = mutableListOf()
    var repoById: Map<String, RepoReference> = mapOf()
    var loaded: Boolean = false
    fun loadIndex(context: Context) {
        repos = mutableListOf()
        val file = File(context.filesDir, "repos.json")
        if (!file.exists()) {
            //saveIndex(context)
            file.writeText("[]")
        }
        val gson = GsonBuilder().create()
        //val file = context.openFileInput("index.json")
        val repoString = String(file.readBytes())
        val repoList: List<RepoReference> = gson.fromJson(repoString, object : TypeToken<List<RepoReference>>() {}.type)
        repos.addAll(repoList)
        updateRepoById()
        loaded = true
    }

    fun verifyLoaded(context: Context) {
        if (!loaded) {
            loadIndex(context)
        }
    }

    fun updateRepoById() {
        repoById = repos.associateBy { it.id }

    }

    fun saveIndex(context: Context) {
        val gson = GsonBuilder().create()

        val fileContents = gson.toJson(repos)
        val file = File(context.filesDir, "repos.json")
        file.writeText(fileContents)
    }

    fun getStations(context: Context): List<RepoStationReference> {
        val gson = GsonBuilder().create()

        return repos.map {
            val repo = it
            val fileDir = File(context.filesDir, "repos")
            val file = File(fileDir, "${it.id}.json")
            val repoStationsString = file.readText()
            val repoList: List<RepoStation> = gson.fromJson(repoStationsString, object : TypeToken<List<RepoStation>>() {}.type)
            repoList.map {
                RepoStationReference(repo, it)
            }

        }.flatten()
    }

    fun addRepo(context: Context, url: String, repoFileData: RepoFileData) {
        verifyLoaded(context)
        val uniqueID = UUID.randomUUID().toString()
        val repoReference = RepoReference(uniqueID, repoFileData.name, repoFileData.text, url)
        repos.add(repoReference)
        saveIndex(context)
        updateRepoById()
        refreshRepo(context, repoReference)
    }

    fun refreshRepo(context: Context, repoReference: RepoReference) {

        val intent: Intent = Intent(context, RepoService::class.java)
        //intent.putExtra("firstTrackUri", trackUri.toString())
        intent.putExtra("repo_id", repoReference.id)
        intent.action = "refresh"
        context.startService(intent)
    }

    fun removeRepo(context:Context, repoReference: RepoReference) {
        repos.remove(repoReference)
        updateRepoById()

        val fileDir = File(context.filesDir, "thumbnails")
        val fileRepoDir=File(fileDir,repoReference.id)
        if (fileRepoDir.exists()) {
            fileRepoDir.deleteRecursively()
        }

        val reposDir = File(context.filesDir, "repos")
        val repoFile= File(reposDir, "${repoReference.id}.json")
        if (repoFile.exists()) {
            repoFile.delete()
        }
        saveIndex(context)

    }
}

private var _repoIndex: RepoIndex? = null
fun getRepoIndex(): RepoIndex {
    if (_repoIndex == null) {
        _repoIndex = RepoIndex()
    }
    return _repoIndex!!
}

data class RepoReference(
        val id: String,
        val name: String,
        val text: String,
        val url: String
)

data class RepoFileData(val name: String, val text: String)

data class RepoStation(val name: String, val id: String)

data class RepoStationReference(val repoReference: RepoReference, val repoStation: RepoStation)

class RepoService : Service() {
    val thumbnailDownloadQueue: MutableList<RepoStationReference> = mutableListOf()
    val repoRefreshQueue: MutableList<String> = mutableListOf()
    var thumbnailDownloadThreadRunning = false
    var repoRefreshThreadRunning = false

    var thumbnailDownloadThreadLock: Lock = ReentrantLock()
    var repoRefreshThreadLock: Lock = ReentrantLock()

    var repoRefreshNotification: RepoNotification?=null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == "refresh") {
            val repoId: String = intent.getStringExtra("repo_id")
            refresh(repoId)
        } else if (intent.action == "refresh-all") {
            getRepoIndex().verifyLoaded(this)
            refreshRepos(getRepoIndex().repos.map { it.id })
        }
        return START_NOT_STICKY
    }

    fun refresh(id: String) {
        val repo = getRepoIndex().repoById[id]
        if (repo == null) {
            Log.w("A", "NULL")
            return
        }
        val url_ = repo.url
        val url: String = if (url_.endsWith("/")) {
            url_
        } else {
            "$url_/"
        }

        (url + "manifest.json").httpGet().responseString { _, _, result ->

            val (text, error) = result
            if (text != null) {
                addRepoIndexData(repo, text)
            } else {
                Log.w("W", error.toString())
            }
        }
    }


    fun addRepoIndexData(repo: RepoReference, data: String) {

        val gson = GsonBuilder().create()
        //getRepoIndex().
        val repoStations: List<RepoStation> = (gson.fromJson(data, object : TypeToken<List<RepoStation>>() {}.type) as List<RepoStation>).filter {
            !ID_REGEX.containsMatchIn(it.id)
        }
        val fileDir = File(filesDir, "repos")
        fileDir.mkdirs()
        val file = File(fileDir, "${repo.id}.json")
        file.writeText(gson.toJson(repoStations))
        downloadThumbnails(repoStations.map { RepoStationReference(repo,it) })
    }


    fun downloadThumbnails(thumbnails: Collection<RepoStationReference>) {
        if (thumbnailDownloadThreadRunning) {
            thumbnailDownloadThreadLock.withLock {
                thumbnailDownloadQueue.addAll(thumbnails)
            }
        } else {
            thumbnailDownloadQueue.addAll(thumbnails)
            startDownloadThread()
        }
    }

    fun refreshRepos(repos: Collection<String>) {
        if (repoRefreshThreadRunning) {
            repoRefreshThreadLock.withLock {
                repoRefreshQueue.addAll(repos)
            }
        } else {
            repoRefreshQueue.addAll(repos)
            startRepoRefreshThread()
        }
    }


    fun startDownloadThread() {
        thread {
            thumbnailDownloadThreadRunning = true
            val fileDir = File(this.filesDir, "thumbnails")
            fileDir.mkdirs()

            while(true) {
                thumbnailDownloadThreadLock.lock()
                if (thumbnailDownloadQueue.size==0) {
                    thumbnailDownloadThreadLock.unlock()
                    break
                }
                val currentTarget=thumbnailDownloadQueue[0]
                thumbnailDownloadQueue.remove(currentTarget)
                thumbnailDownloadThreadLock.unlock()
                val fileRepoDir=File(fileDir,currentTarget.repoReference.id)

                fileRepoDir.mkdirs()

                val file=File(fileRepoDir, currentTarget.repoStation.id)
                if (file.exists()) {
                    continue
                }
                val (_, _, result) = ("${currentTarget.repoReference.url}/stations/${currentTarget.repoStation.id}/image_thumbnail").httpGet().response()
                val (data, error) = result
                if (error != null) {
                    continue
                }

                file.writeBytes(data!!)
            }
            thumbnailDownloadThreadRunning = false
        }
    }

    fun startRepoRefreshThread() {
        thread {
            repoRefreshThreadRunning = true
            repoRefreshNotification= RepoNotification()
            repoRefreshNotification?.create(this)
            repoRefreshNotification?.text=getString(R.string.updateing_repos)
            repoRefreshNotification?.progress=-1
            repoRefreshNotification?.update(this)

            while(true) {
                repoRefreshThreadLock.lock()
                if (repoRefreshQueue.size==0) {
                    repoRefreshThreadLock.unlock()
                    break
                }
                val currentTarget=repoRefreshQueue[0]
                repoRefreshQueue.remove(currentTarget)

                refresh(currentTarget)

                repoRefreshThreadLock.unlock()

            }
            repoRefreshNotification?.text=getString(R.string.updateing_repos_done)
            repoRefreshNotification?.showProgress=false
            repoRefreshNotification?.ongoing=false
            repoRefreshNotification?.update(this)


            repoRefreshThreadRunning = false

        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        Log.w("P", "BIND")
        return null
    }

}

class RepoNotification {
    private var mBuilder: NotificationCompat.Builder?=null

    var text:String=""
    var ongoing:Boolean=true
    private var _progress:Int=0
    var showProgress:Boolean=false

    init{


    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.rchannel_name)
            val description = context.getString(R.string.rchannel_description)
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
        text=context.getString(R.string.downloading_station)
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
            if (progress != -1) {
                mBuilder?.setProgress(100, progress, false)
            } else {
                mBuilder?.setProgress(100, 0, true)

            }
        }
        val notificationManager = NotificationManagerCompat.from(context)

        notificationManager.notify(1, mBuilder!!.build())
    }
}
