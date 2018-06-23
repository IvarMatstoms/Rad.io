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
import java.util.*


class Player {
    var station:Station?=null
    var playing=false;

    var currentProgress:Int?=null;
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
    var mPlayer: MediaPlayer? = null;
    var station: Station? = null;


    override fun onBind(intent: Intent): IBinder? {

        Log.w("P","BIND")
        return null
    }

    init {
        val timer = Timer()
        val player=getPlayer()
        timer.scheduleAtFixedRate(object : TimerTask() {

            override fun run() {
                if (player.playing && mPlayer?.currentPosition != null) {
                    player.currentProgress=mPlayer?.currentPosition
                    /*
                    val intent = Intent()
                    intent.action = NOWPLAYING_FRAGMENT_ACTION
                    intent.putExtra("status", "update")
                    //val currentItem=
                    intent.putExtra("track")
                    sendBroadcast(intent)
                    //
                            //FUCK
                            */
                }
            }

        }, 0, 1000)
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
            //mPlayer = MediaPlayer.create(this);
            //setting loop play to true
            //this will make the ringtone continuously playing
            //mPlayer.setLooping(true);

            //staring the mPlayer

        } else if (intent.action=="pause") {
            mPlayer?.pause()
            getPlayer().playing=false
        } else if (intent.action=="resume") {
            station?.queue?.fastForward()
            playTrack()

        }

        //we have some options for service
        //start sticky means service will be explicity started and stopped
        //return START_REDELIVER_INTENT;
        return START_NOT_STICKY
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
        if (mPlayer == null) {
            mPlayer= createMediaPlayer(fileUri!!)

        } else {
            mPlayer?.reset()
            mPlayer?.setDataSource(this, fileUri)

            mPlayer?.prepare()
        }
        //mPlayer?.seekTo(60)


        val offset=System.currentTimeMillis()-nextItem?.startTime*1000L
        if (offset > 0) {
            mPlayer?.start();
            mPlayer?.seekTo(offset.toInt())

        } else {
            val handler = Handler()
            handler.postDelayed(Runnable {
                //Do something after 100ms
                mPlayer?.start();
            }, Math.abs(offset))
        }
        getPlayer().playing=true
        //mPlayer?.start();
    }


    override fun onDestroy() {
        Log.w("P","STOP")
        if (mPlayer != null && mPlayer?.isPlaying!!) {
            mPlayer?.stop()
        }
        mPlayer?.release()
    }
}
