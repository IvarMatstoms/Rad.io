package tech.ivar.radio

import android.content.Context
import android.util.Log
import tech.ivar.ra.Station
import android.media.MediaPlayer
import android.content.Intent
import android.app.Service
import android.os.IBinder
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Handler
import tech.ivar.ra.loadRaFile


class Player {
    var station:Station?=null
    var playing=false;
    init {

    }

    fun createPlayer(context:Context) {

    }

    fun play(context: Context, stationId: String) {
        //val upcomingItem=station.queue.nextItem()
        //val trackUri:Uri=Uri.fromFile(station.getResFile(context, upcomingItem.item.getItems()[0].fileId))

        val intent:Intent=Intent(context, BackgroundAudioService::class.java)
        //intent.putExtra("firstTrackUri", trackUri.toString())
        intent.putExtra("stationId", stationId)
        intent.action="play"
        context.startService(intent)
    }
}

var p:Player? = null;

fun getPlayer():Player {
    if (p == null) {
        p=Player()
    }
    return p!!
}
class BackgroundAudioService() : Service() {
    //creating a mediaplayer object
    var player: MediaPlayer? = null;
    var station: Station? = null;


    override fun onBind(intent: Intent): IBinder? {

        Log.w("P","BIND")
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //getting systems default ringtone
        Log.w("A",intent.action)
        if (intent.action == "play") {
            val stationId:String = intent.getStringExtra("stationId")
            station= loadRaFile(this, stationId)
            getPlayer().station=station
            station?.queue?.fastForward()
            playTrack()
            //player = MediaPlayer.create(this);
            //setting loop play to true
            //this will make the ringtone continuously playing
            //player.setLooping(true);

            //staring the player

        } else if (intent.action=="pause") {
            player?.pause()
        }

        //we have some options for service
        //start sticky means service will be explicity started and stopped
        return START_STICKY;
    }

    fun createMediaPlayer(uri: Uri):MediaPlayer {
        val p = MediaPlayer.create(this, uri);
        p.setOnCompletionListener(OnCompletionListener {
            Log.w("P","DONE!")
            playNext()
        })
        return p
    }

    fun playNext() {
        station?.queue?.nextItem()
        playTrack()
    }

    fun playTrack() {
        Log.w("DAB",station!!.queue.currentItem.toString())
        val nextItem=station!!.queue.currentItem
        val nextTrack=nextItem?.item?.getItems()!![0]
        val fileUri:Uri?= Uri.fromFile(station?.getResFile(this, nextTrack.fileId))
        if (player == null) {
            player= createMediaPlayer(fileUri!!)

        } else {
            player?.reset()
            player?.setDataSource(this, fileUri)

            player?.prepare()
        }
        //player?.seekTo(60)


        val offset=System.currentTimeMillis()-nextItem?.startTime*1000L
        if (offset > 0) {
            player?.start();
            player?.seekTo(offset.toInt())

        } else {
            val handler = Handler()
            handler.postDelayed(Runnable {
                //Do something after 100ms
                player?.start();
            }, Math.abs(offset))
        }
        getPlayer().playing=true
        //player?.start();
    }


    override fun onDestroy() {
        Log.w("P","STOP")
        if (player != null && player?.isPlaying!!) {
            player?.stop()
        }
        player?.release()
    }
}
