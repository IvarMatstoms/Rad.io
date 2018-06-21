package tech.ivar.radio

import android.app.Activity
import android.app.PendingIntent.getActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_stations.*
import tech.ivar.ra.loadRaFile
import android.support.design.widget.BottomNavigationView
import tech.ivar.radio.R.id.navigation

val STATIONS_FRAGMENT_ACTION= "tech.ivar.radio.stationsfragment.action"
// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [StationsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [StationsFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class StationsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        //Log.d("V",toString(R.id.fabImport))
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(STATIONS_FRAGMENT_ACTION)
        activity?.registerReceiver(StationsFragmentBroadcastReceiver(), intentFilter)



    }


    val clickListener = OnClickListener {view ->
        when (view.id) {
            R.id.fabImport -> {
                Log.w("W","Fab press")
                val intent = Intent(activity, ImportActivity::class.java)
                        .apply {

                        }
                startActivity(intent)
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fabImport.setOnClickListener(clickListener)
        var viewManager = LinearLayoutManager(activity)
        Log.w("S",getStationIndex().stations.map { it.name }.toString())
        var viewAdapter = StationsListAdapter(activity as Context,getStationIndex().stations.toTypedArray())

        var recyclerView = stationsList.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_stations, container, false)
        //val fab = v.findViewById<FloatingActionButton>(R.id.fabImport)
        //fab.setOnClickListener(clickListener)

        return v
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        Log.d("V", "B press")
        listener?.onFragmentInteraction(uri)
    }

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
         * @return A new instance of fragment StationsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                StationsFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}

class StationsListAdapter(val context: Context,private val stations: Array<StationReference>) :
        RecyclerView.Adapter<StationsListAdapter.ViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class ViewHolder(val listItem: ConstraintLayout) : RecyclerView.ViewHolder(listItem)


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): StationsListAdapter.ViewHolder {
        // create a new view
        val listItem = LayoutInflater.from(parent.context)
                .inflate(R.layout.station_list_item, parent, false) as ConstraintLayout

        return ViewHolder(listItem)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.listItem.findViewById<TextView>(R.id.stationListName).text = stations[position].name
        val id:String=stations[position].id
        val slp=holder.listItem.findViewById<ImageButton>(R.id.stationListPlay)
        val player= getPlayer()
        var currentPlaying=false
        if (player.playing) {
            if (stations[position].id == player.station?.id) {
                slp.setImageResource(R.drawable.ic_pause_black_24dp);
                currentPlaying=true
            }
        }
        val clickListener = OnClickListener {view ->
            when (view.id) {
                R.id.stationListPlay -> {
                    val p= getPlayer()
                    if (!currentPlaying || !p.playing) {
                        getPlayer().play(context, id)
                        val fragment = NowPlayingFragment()
                        val fragmentTransaction = (context as MainActivity).getSupportFragmentManager().beginTransaction()
                        fragmentTransaction.replace(R.id.fragmentContainer, fragment)
                        fragmentTransaction.addToBackStack(null)
                        fragmentTransaction.commit()
                        val bottomNavigationView: BottomNavigationView = (context as MainActivity).findViewById(navigation) as BottomNavigationView
                        bottomNavigationView.selectedItemId = R.id.navigation_dashboard
                    } else {
                        val intent: Intent = Intent(context, BackgroundAudioService::class.java)
                        intent.action = "pause"
                        context?.startService(intent)
                        slp.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                    }
                }
            }

        }
        holder.listItem.findViewById<ImageButton>(R.id.stationListPlay).setOnClickListener (clickListener)

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = stations.size
}

class StationsFragmentBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.w("Q","HI!!!")
        val status=intent.getStringExtra("status");
        if (status=="update") {
            Log.w("U","UPDATE!!")
            //((context as ImportActivity).currentFragment as ImportWebFragment).showStatusBar()

        }
    }

}
