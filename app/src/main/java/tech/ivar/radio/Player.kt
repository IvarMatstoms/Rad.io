package tech.ivar.radio

import android.content.Context
import android.os.Handler
import android.util.Log
import tech.ivar.ra.Station
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelection
import android.media.MediaPlayer
import android.content.Intent
import android.R.raw
import android.app.IntentService
import android.app.Service
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.IBinder
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Bundle
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.FileDataSource.FileDataSourceException
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util


class Player {
    init {

    }

    fun createPlayer(context:Context) {

    }

    fun play(context: Context, station: Station) {
        //(0 .. 20).forEach {
        //    Log.w("P",station.queue.nextItem().toString())
        //}
        val upcomingItem=station.queue.nextItem()
        val trackUri:Uri=Uri.fromFile(station.getResFile(context, upcomingItem.item.getItems()[0].fileId))
        // 1. Create a default TrackSelector
        //val mediaPlayer:MediaPlayer = MediaPlayer.create(context,trackUri);
        //mediaPlayer.start();
        //MediaPlayer mediaPlayer = new MediaPlayer();
        val intent:Intent=Intent(context, BackgroundAudioService::class.java)
        intent.putExtra("firstTrackUri", trackUri.toString())
        context.startService(intent)
        //val mediaPlayer = MediaPlayer.create(context, trackUri)
        //mediaPlayer.setAudioAttributes(AudioAttributes.CONTENT_TYPE_MUSIC);
//mediaPlayer.setDataSource(context, trackUri);
//mediaPlayer.prepare();
//mediaPlayer.start();

        //val exoPlayer = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector(null), DefaultLoadControl())
        /*
        val playbackServiceIntent = Intent(context, BackgroundAudioService::class.java)

        //val bundle = Bundle()
        //bundle.putString("firstTrack", uri.toString())
        playbackServiceIntent.putExtra("firstTrackUri", uri.toString())
        Log.w("U",uri.toString())
        context.startService(playbackServiceIntent);
        */
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
    lateinit var player: MediaPlayer;

    override fun onBind(intent: Intent): IBinder? {

        Log.w("P","BIND")
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //getting systems default ringtone
        val url: Uri=Uri.parse(intent.getStringExtra("firstTrackUri"))
        player = MediaPlayer.create(this,
                url);
        //setting loop play to true
        //this will make the ringtone continuously playing
        //player.setLooping(true);

        //staring the player
        player.start();

        //we have some options for service
        //start sticky means service will be explicity started and stopped
        return START_STICKY;
    }


    override fun onDestroy() {
        Log.w("P","STOP")
        if (player.isPlaying) {
            player.stop()
        }
        player.release()
    }
}
/*
class BackgroundAudioService() : Service(), OnCompletionListener {


    lateinit var mediaPlayer: MediaPlayer
    lateinit var firstTrackUri: Uri

    init {
        Log.w("P","INIT")
    }

    override fun onBind(intent: Intent): IBinder? {

        Log.w("P","BIND")
        return null
    }

    override fun onCreate() {

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.w("P","CREATE")
        firstTrackUri = Uri.parse(intent.getStringExtra("firstTrackUri"))
        mediaPlayer = MediaPlayer.create(this, firstTrackUri)// raw/s.mp3
        mediaPlayer.prepareAsync();

        mediaPlayer.setOnCompletionListener(this)

        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
        Log.w("P","START")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.w("P","STOP")
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
    }

    override fun onCompletion(_mediaPlayer: MediaPlayer) {
        Log.w("P","STOP")
        stopSelf()
    }

}
*/