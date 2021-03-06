package tech.ivar.radio

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [StationsEditFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [StationsEditFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class StationsEditFragment : Fragment(), OnStartDragListener{
    private var mItemTouchHelper: ItemTouchHelper? = null
    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mItemTouchHelper?.startDrag(viewHolder)
    }

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stations_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = RecyclerListAdapter(this)

        val recyclerView = view.findViewById(R.id.stationsEditRview) as RecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)

        //val adapter = RecyclerListAdapter()


        val callback = SimpleItemTouchHelperCallback(adapter)
        //val touchHelper = ItemTouchHelper(callback)
        //touchHelper.attachToRecyclerView(recyclerView)
        //val callback = SimpleItemTouchHelperCallback(adapter)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper?.attachToRecyclerView(recyclerView)
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
    interface OnFragmentInteractionListener

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment StationsEditFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                StationsEditFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}

class RecyclerListAdapter(private val mDragStartListener: OnStartDragListener) : RecyclerView.Adapter<RecyclerListAdapter.ItemViewHolder>(), ItemTouchHelperAdapter {

    private val mItems:MutableList<StationReference> = mutableListOf()
    var context: Context?=null
    init {
        //mItems.addAll(Arrays.asList(context.resources.getStringArray(R.array.dummy_items)))
        mItems.addAll(getStationIndex().stations)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.stations_edit_list_item, parent, false)
        context=parent.context
        return ItemViewHolder(view)

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        //holder.textView.setText(mItems.get(position))
        holder.listItem.findViewById<TextView>(R.id.stationsEditListName).text = mItems[position].name
        holder.station=mItems[position]

        // Start a drag whenever the handle view it touched
        holder.handleView.setOnTouchListener({ _, event ->
            //event.getAc
            if (event.action == MotionEvent.ACTION_DOWN) {
                mDragStartListener.onStartDrag(holder)
            }
            false
        })
    }

    override fun onItemDismiss(position: Int) {
        Log.w("R","REMOVE AT $position")
        val station:StationReference= getStationIndex().stations[position]
        if (context != null ) {
            context?.alert(context!!.getString(R.string.cant_be_undone), context!!.getString(R.string.delete_string,station.name)) {
                yesButton {
                    getStationIndex().deleteStationByPosition(context!!, position)
                    context?.toast(context!!.getString(R.string.station_deleted))
                }
                noButton {
                    context?.toast(context!!.getString(R.string.canceled))
                    /*
                    val fragment = StationsEditFragment()
                    val fragmentTransaction = (context as StationsEditActivity).supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.fragmentContainer, fragment)
                    fragmentTransaction.addToBackStack(null)
                    fragmentTransaction.commit()
                    */
                }
            }?.show()
        }
        mItems.removeAt(position)
        notifyItemRemoved(position)

    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {

        Collections.swap(mItems, fromPosition, toPosition)
        val stationIndex: StationIndex = getStationIndex()
        notifyItemMoved(fromPosition, toPosition)
        stationIndex.swapStations(context!!, fromPosition, toPosition)
        Log.w("M","$fromPosition -> $toPosition")
        return true
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ItemTouchHelperViewHolder {

        val listItem: ConstraintLayout = itemView as ConstraintLayout

        val handleView: ImageView = itemView.findViewById(R.id.stationsEditListHandle) as ImageView

        var station: StationReference?=null

        val context: Context?=itemView.context
        init {

        }

        override fun onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY)
        }

        override fun onItemClear() {
            itemView.setBackgroundColor(0)
        }
    }

    override fun getItemCount(): Int {
        return mItems.size
    }
}
interface ItemTouchHelperAdapter {

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean

    fun onItemDismiss(position: Int)
}


class SimpleItemTouchHelperCallback(private val mAdapter: ItemTouchHelperAdapter) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return true
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        recyclerView.context
        //Log.w("M",viewHolder.getAdapterPosition().toString()+"|"+target.getAdapterPosition().toString())
        mAdapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        mAdapter.onItemDismiss(viewHolder.adapterPosition)
    }

}

interface ItemTouchHelperViewHolder {

    fun onItemSelected()

    fun onItemClear()
}

interface OnStartDragListener {

    /**
     * Called when a view is requesting a start of a drag.
     *
     * @param viewHolder The holder of the view to drag.
     */
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)

}