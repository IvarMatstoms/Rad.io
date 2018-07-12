package tech.ivar.radio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_import_web.*


val IMPORTWEB_STATUSBAR_ACTION = "tech.ivar.radio.importweb.statusbar"
// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ImportWebFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ImportWebFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ImportWebFragment : Fragment() {
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
        //val intentFilter = IntentFilter()
        //intentFilter.addAction(IMPORTWEB_STATUSBAR_ACTION)
        //activity?.registerReceiver(ImportWebBroadcastReceiver(), intentFilter)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_web, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        importWebCheckButton.setOnClickListener(clickListener)
    }

    fun startImportOptionsActivity(url:String) {
        val intent = Intent(context, ImportOptionsActivity::class.java)
        val b = Bundle()
        b.putString("import_type", "web") //Your id
        b.putString("import_web_url", url) //Your id

        intent.putExtras(b) //Put your id to your next Intent
        startActivity(intent)
    }

    fun showStatusBar() {
        importWebProgress.visibility=View.VISIBLE
    }

    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.importWebCheckButton -> {
                val url:String=importWebUrl.text.toString()
                Log.w("W", "Check press")
                Log.w("W",url)
                //getStationIndex().fromUrl(activity as Context,url)
                startImportOptionsActivity(url)
                /*
                val intent = Intent(activity, MainActivity::class.java)
                        .apply {

                        }
                startActivity(intent)
                */

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
         * @return A new instance of fragment ImportWebFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                ImportWebFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
/*
class ImportWebBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.w("Q","HI!!!")
        val status=intent.getStringExtra("status");
        if (status=="show") {
            ((context as ImportActivity).currentFragment as ImportWebFragment).showStatusBar()

        }
    }

}
        */