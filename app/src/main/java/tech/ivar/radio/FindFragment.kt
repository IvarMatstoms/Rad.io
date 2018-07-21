package tech.ivar.radio

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Filter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_find.*
import java.io.File


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
    private lateinit var viewAdapter: FindListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        setHasOptionsMenu(true)


    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        //val inflater = activity?.getMenuInflater()
        inflater.inflate(R.menu.find_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.findMenuEdit -> {
                val intent = Intent(activity, RepoEditActivity::class.java)
                        .apply {

                        }
                startActivity(intent)
                true
            }
            R.id.findMenuRefresh -> {
                val intent: Intent = Intent(context, RepoService::class.java)
                //intent.putExtra("firstTrackUri", trackUri.toString())
                intent.action = "refresh-all"
                context!!.startService(intent)
                true
            }
                else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        findListAdd.setOnClickListener(clickListener)
        findSearchButton.setOnClickListener(clickListener)

        val viewManager = LinearLayoutManager(activity)

        getRepoIndex().verifyLoaded(context!!)
        viewAdapter = FindListAdapter(activity as Context, getRepoIndex().getStations(context!!).sortedBy { it.repoStation.name }.toTypedArray())
        findListRV.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        findSearchText.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val text=findSearchText.text.toString()
                viewAdapter.getFilter().filter(text)
                return@OnEditorActionListener true
            }
            false
        })

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find, container, false)
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
            R.id.findSearchButton -> {
                val text=findSearchText.text.toString()
                viewAdapter.getFilter().filter(text)
            }
        }

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
    interface OnFragmentInteractionListener

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
class FindListAdapter(val context: Context, private val stations: Array<RepoStationReference>) :
        RecyclerView.Adapter<FindListAdapter.ViewHolder>() {

    private var stationsFiltered: List<RepoStationReference> = stations.toList()

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
        val station=stationsFiltered[position]
        holder.listItem.findViewById<TextView>(R.id.findListName).text = station.repoStation.name
        holder.listItem.findViewById<TextView>(R.id.findListRepoName).text = station.repoReference.name


        val imageFile = File(File(File(context.filesDir, "thumbnails"),station.repoReference.id),station.repoStation.id)
        if (imageFile.exists()) {
            val myBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

            holder.listItem.findViewById<ImageView>(R.id.findListImage).setImageBitmap(myBitmap)
        }
        //findListDownloadButton
        val clickListener = View.OnClickListener {
            val intent = Intent(context, ImportOptionsActivity::class.java)
            val b = Bundle()

            val url="${station.repoReference.url}/stations/${station.repoStation.id}/"
            b.putString("import_type", "web") //Your id
            b.putString("import_web_url", url) //Your id

            intent.putExtras(b) //Put your id to your next Intent
            context.startActivity(intent)
        }
        holder.listItem.findViewById<ImageButton>(R.id.findListDownloadButton).setOnClickListener (clickListener)

    }

    fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                stationsFiltered = if (charString.isEmpty()) {
                    stations.sortedBy { it.repoStation.name }.toList()
                } else {
                    stations.filter {
                        it.repoStation.name.toLowerCase().contains(charSequence.toString().toLowerCase())
                    }.sortedBy { it.repoStation.name }.toList()
                }

                val filterResults = FilterResults()
                filterResults.values = stationsFiltered
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                stationsFiltered = filterResults.values as List<RepoStationReference>
                notifyDataSetChanged()
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = stationsFiltered.size
}


