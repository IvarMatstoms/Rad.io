package tech.ivar.radio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_find.*
import kotlinx.android.synthetic.main.fragment_stations.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [FindFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [FindFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class FindFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        findListAdd.setOnClickListener(clickListener)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find, container, false)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.findListAdd -> {
                Log.w("W", "Fab press")
                val intent = Intent(activity, AddRepoActivity::class.java)
                        .apply {

                        }
                startActivity(intent)
            }
        }

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
         * @return A new instance of fragment FindFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                FindFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
//TODO
class FindListAdapter(val context: Context,private val stations: Array<StationReference>) :
        RecyclerView.Adapter<FindListAdapter.ViewHolder>() {

    class ViewHolder(val listItem: ConstraintLayout) : RecyclerView.ViewHolder(listItem)


    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): FindListAdapter.ViewHolder {
        // create a new view
        val listItem = LayoutInflater.from(parent.context)
                .inflate(R.layout.find_list_item, parent, false) as ConstraintLayout

        return ViewHolder(listItem)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            holder.listItem.findViewById<TextView>(R.id.findListName).text = stations[position].name
        /*
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
            */

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = stations.size
}
