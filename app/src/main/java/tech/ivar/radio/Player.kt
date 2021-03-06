package tech.ivar.radio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import tech.ivar.ra.Station
import android.media.MediaPlayer
import android.content.Intent
import android.app.Service
import android.os.IBinder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import java.util.*
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
//import sun.invoke.util.VerifyAccess.getPackageName
import android.widget.RemoteViews
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

val PLAYER_SERVICE_ACTION = "tech.ivar.radio.playerservice.action"
private const val CHANNEL_ID = "tech.ivar.radio.pchannel"

class Player {
    var station: Station? = null
    var playing = false

    var currentProgress: Int? = null

    init {

    }

    fun play(context: Context, stationId: String) {
        val intent = Intent(context, BackgroundAudioService::class.java)
        intent.putExtra("stationId", stationId)
        intent.action = "play"
        context.startService(intent)
    }
}

var p: Player? = null

fun getPlayer(): Player {
    if (p == null) {
        p = Player()
    }
    return p!!
}

class BackgroundAudioService : Service() {
    //creating a mediaplayer object
    var mPlayer: MediaPlayer? = null
    var station: Station? = null
    val notification: PlayerNotification = PlayerNotification()

    override fun onBind(intent: Intent): IBinder? {

        Log.w("P", "BIND")
        return null
    }

    init {
        val timer = Timer()
        val player = getPlayer()
        timer.scheduleAtFixedRate(object : TimerTask() {

            override fun run() {
                if (player.playing && mPlayer?.currentPosition != null) {
                    player.currentProgress = mPlayer?.currentPosition
                }
            }

        }, 0, 1000)


    }

    override fun onCreate() {
        super.onCreate()
        notification.create(this)
        val intentFilter = IntentFilter()
        intentFilter.addAction(PLAYER_SERVICE_ACTION)
        this.registerReceiver(playerBroadcastReceiver, intentFilter)

        if ("player_cache.json" !in fileList()) {
            saveCache()
        }
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.w("A", intent.action)
        when {
            intent.action == "play" -> {
                val stationId: String = intent.getStringExtra("stationId")
                selectStation(stationId)
                playTrack()
            }
            intent.action == "pause" -> pause()
            intent.action == "resume" -> resume()

        }
        return START_NOT_STICKY
    }

    fun pause() {
        mPlayer?.pause()
        getPlayer().playing = false
        updateFrontends()
    }

    fun resume() {
        if (station == null) {
            loadCache()
        }

        if (station != null) {
            station?.queue?.fastForward()
            playTrack()
            updateFrontends()
        }
    }

    fun updateFrontends() {
        notification.update(this)
    }

    fun selectStation(stationId: String) {
        //
        //station=loadRaFile(this, stationId)
        station = getStationIndex().loadStation(this, stationId)
        getPlayer().station = station
        station?.queue?.fastForward()
        saveCache()
    }


    private val playerBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getStringExtra("status")
            Log.w("H", status)
            if (status == "toggle") {
                if (getPlayer().playing) {
                    pause()
                } else {
                    resume()
                }
            } else if (status == "resume") {
                resume()
            } else if (status == "destroy_notification") {
                notification.destroy(context)

                pause()
            }
        }
    }

    fun saveCache() {
        val cacheObj = PlayerCache(station?.id)
        val gson = GsonBuilder().create()
        val fileContents = gson.toJson(cacheObj)

        this.openFileOutput("player_cache.json", Context.MODE_PRIVATE).use {
            it.write(fileContents.toByteArray())
        }
    }

    fun loadCache() {
        val gson = GsonBuilder().create()
        val file = openFileInput("player_cache.json")
        val cacheString = String(file.readBytes())
        val cacheObj: PlayerCache = gson.fromJson(cacheString, object : TypeToken<PlayerCache>() {}.type)
        if (cacheObj.stationId != null) {
            selectStation(cacheObj.stationId)
        }
    }


    fun createMediaPlayer(uri: Uri): MediaPlayer {
        val p = MediaPlayer.create(this, uri)
        p.setOnCompletionListener({
            Log.w("P", "DONE!")
            playNext()
        })
        return p
    }

    fun playNext() {
        station?.queue?.nextItem()
        playTrack()
    }

    fun playTrack() {
        Log.w("DAB", station!!.queue.currentItem.toString())
        val nextItem = station!!.queue.currentItem
        val nextTrack = nextItem?.item?.getItems()!![0]
        val fileUri: Uri? = Uri.fromFile(station?.getResFile(this, nextTrack.fileId))
        if (mPlayer == null) {
            mPlayer = createMediaPlayer(fileUri!!)

        } else {
            mPlayer?.reset()
            mPlayer?.setDataSource(this, fileUri)

            mPlayer?.prepare()
        }
        //mPlayer?.seekTo(60)


        val offset = System.currentTimeMillis() - nextItem.startTime * 1000L
        if (offset > 0) {
            mPlayer?.start()
            mPlayer?.seekTo(offset.toInt())

        } else {
            val handler = Handler()
            handler.postDelayed({
                //Do something after 100ms
                mPlayer?.start()
            }, Math.abs(offset))
        }
        getPlayer().playing = true
        notification.update(this)
        //mPlayer?.start();
    }


    override fun onDestroy() {
        Log.w("P", "STOP")
        notification.destroy(this)
        if (mPlayer != null && mPlayer?.isPlaying!!) {
            mPlayer?.stop()
        }
        mPlayer?.release()
        try {
            this.unregisterReceiver(playerBroadcastReceiver)
        } catch (e: IllegalArgumentException) {

        }
    }
}

class PlayerNotification {
    private var mBuilder: NotificationCompat.Builder? = null
    var trackName: String = ""
    var stationName: String = ""
    var ongoing: Boolean = true
    var destroyed: Boolean = false
    private var stationImage: Bitmap? = null
    private var currentStaitonId: String? = null

    init {

    }

    fun create(context: Context) {
        createNotificationChannel(context)
        //update(context)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Log.w("C",context.getString(R.string.finishing_up))
            val name = context.getString(R.string.pchannel_name)
            val description = context.getString(R.string.pchannel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }


    fun update(context: Context) {
        if (!getPlayer().playing && destroyed) {
            return
        } else {
            destroyed=false
        }
        val player = getPlayer()
        val station = player.station
        if (station != null && currentStaitonId != station.id) {
            val imageFile: File = player.station!!.getResFile(context, player.station!!.imageFileId)
            stationImage = BitmapFactory.decodeFile(imageFile.absolutePath)
            currentStaitonId = station.id
        }
        val item = player.station?.queue?.currentItem?.item?.getItems()?.get(0)
        trackName = item?.name ?: ""
        stationName = station?.name ?: ""

        //val intent: Intent = Intent(context, BackgroundAudioService::class.java)
        //intent.action = "pause"
        val intent = Intent()
        intent.action = PLAYER_SERVICE_ACTION
        intent.putExtra("status", "toggle")

        val pendingPauseIntent = PendingIntent.getBroadcast(context, 101, intent, 0)

        val destroyIntent = Intent()
        destroyIntent.action = PLAYER_SERVICE_ACTION
        destroyIntent.putExtra("status", "destroy_notification")

        val pendingDestroyIntent = PendingIntent.getBroadcast(context, 102, destroyIntent, 0)

        val notificationLayout = RemoteViews(context.packageName, R.layout.notification_player)
        notificationLayout.setTextViewText(R.id.playerNotificationTrack, trackName)
        notificationLayout.setTextViewText(R.id.playerNotificationStation, stationName)
        notificationLayout.setOnClickPendingIntent(R.id.playerNotificationPlayPause, pendingPauseIntent)
        notificationLayout.setOnClickPendingIntent(R.id.playerNotificationDestroy, pendingDestroyIntent)

        //notificationLayout.setOnClickPendingIntent()
        stationImage?.let {
            notificationLayout.setImageViewBitmap(R.id.playerNotificationImage, it)
        }

        val tapIntent = Intent(context, MainActivity::class.java)
        tapIntent.putExtra("status", "now_playing")
        tapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        //tapIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val tapPendingIntent = PendingIntent.getActivity(context, 0, tapIntent, 0)

        if (player.playing) {
            notificationLayout.setImageViewResource(R.id.playerNotificationPlayPause, R.drawable.ic_pause_black_24dp)
        } else {
            notificationLayout.setImageViewResource(R.id.playerNotificationPlayPause, R.drawable.ic_play_arrow_black_24dp)
        }
                notificationLayout.setImageViewResource(R.id.playerNotificationDestroy, R.drawable.ic_close_black_24dp)


        mBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_radio_black_24dp)
                .setOngoing(ongoing)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setContentIntent(tapPendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)

        notificationManager.notify(3, mBuilder!!.build())
    }

    fun destroy(context: Context) {
        destroyed=true
        NotificationManagerCompat.from(context).cancel(3)
    }
}

private data class PlayerCache(
        @com.google.gson.annotations.SerializedName("station_id")
        val stationId: String?)