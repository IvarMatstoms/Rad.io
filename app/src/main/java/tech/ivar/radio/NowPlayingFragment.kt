package tech.ivar.radio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_import_web.*
import kotlinx.android.synthetic.main.fragment_now_playing.*
import tech.ivar.ra.Station
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Handler
import android.os.Message
import android.widget.ImageButton
import tech.ivar.ra.UpcomingItem
import java.io.File
import java.util.*
import java.lang.Compiler.command
import java.util.concurrent.*
import javax.xml.datatype.DatatypeConstants.SECONDS

val NOWPLAYING_FRAGMENT_ACTION= "tech.ivar.radio.nowplayingfragment.action"
// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [NowPlayingFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [NowPlayingFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class NowPlayingFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private var currentItem: UpcomingItem?=null
    private var currentStation: Station?=null
    private var currentPlayingState:Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        /*
        val intentFilter = IntentFilter()
        intentFilter.addAction(NOWPLAYING_FRAGMENT_ACTION)
        activity?.registerReceiver(nowPlayingBroadcastReceiver, intentFilter)*/
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_now_playing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val station: Station? = getPlayer().station
        if (station != null) {
            //nowPlayingStation.text = station.name

            //val currentItem = station.queue.currentItem?.item?.getItems()!![0]

            context?.let {
                val imageFile: File = station.getResFile(it, station.imageFileId)
                val myBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath())

                val myImage = nowPlayingImage

                myImage.setImageBitmap(myBitmap)
            }

        }
        updatePlayPause()
        playerPause.setOnClickListener(clickListener)
        val mHandler = Handler()
//Make sure you update Seekbar on UI thread
        val player= getPlayer()
        activity?.runOnUiThread(object : Runnable {
            fun formatSec(sec:Int):String {
                val m=String.format ("%02d", sec/60)
                val s=String.format ("%02d", sec%60)
                return "$m:$s"
            }
            override fun run() {

                if (player.station?.queue?.currentItem != currentItem) {
                    currentItem=player.station?.queue?.currentItem
                    val ci=currentItem?.item?.getItems()!![0]
                    nowPlayingTrack?.text = ci.name
                    nowPlayingAlbum?.text = ci.album.name
                    nowPlayingArtist?.text = ci.artist.name
                    nowPlayingProgress?.max=ci.length
                    nowPlayingLength?.text=formatSec(ci.length)
                }

                if (currentPlayingState != player.playing) {
                    updatePlayPause()
                }

                if (player.playing) {
                    //val progress:Int=mPlayer.
                    if (player.currentProgress!=null) {
                        //val p=(((player.currentProgress!!.toFloat()/1000)/player.station?.queue?.currentItem?.item?.getItems()!![0].length.toFloat())*100).toInt()
                        val p=player.currentProgress!!.toInt()/1000
                        nowPlayingProgress?.progress = p
                        nowPlayingCurrent?.text=formatSec(p)
                        //Log.w("P",p.toString())
                    }
                }

                if (currentStation != player.station && player.station != null && nowPlayingStation != null) {
                    context?.let {
                        nowPlayingStation.text = player.station!!.name
                        currentStation=player.station
                        val imageFile: File = player.station!!.getResFile(it, player.station!!.imageFileId)
                        val myBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath())

                        val myImage = nowPlayingImage

                        myImage.setImageBitmap(myBitmap)
                    }
                }
                /*

                */
                mHandler.postDelayed(this, 1000)
            }
        })

    }

    fun updatePlayPause() {
        val pp = playerPause
        pp?.let {
            if (getPlayer().playing) {
                it.setImageResource(R.drawable.ic_pause_black_24dp);
                currentPlayingState = true
            } else {
                it.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                currentPlayingState = false
            }
        }
    }


    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.playerPause -> {
                val pp = playerPause
                if (getPlayer().playing) {
                    Log.w("W", "Pause")
                    val intent: Intent = Intent(context, BackgroundAudioService::class.java)
                    intent.action = "pause"
                    context?.startService(intent)
                    pp.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                } else {
                    val intent: Intent = Intent(context, BackgroundAudioService::class.java)
                    intent.action = "resume"
                    context?.startService(intent)
                    pp.setImageResource(R.drawable.ic_pause_black_24dp);
                }
            }
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    /*
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }
    */

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NowPlayingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                NowPlayingFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
