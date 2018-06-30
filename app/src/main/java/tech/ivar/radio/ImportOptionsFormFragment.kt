package tech.ivar.radio

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.jetbrains.anko.toast
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.content.Intent
import android.widget.RadioButton
import kotlinx.android.synthetic.main.fragment_import_options_form.*

//importOptionsFormOkButton
// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ImportOptionsFormFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ImportOptionsFormFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ImportOptionsFormFragment : Fragment(), OnItemSelectedListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private var swsManifest: SwsManifest? = null
    private var downloadOptionsList: List<DownloadOption>? = null
    private var selectedDownloadOption: DownloadOption? = null
    private var url: String?=null

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
        return inflater.inflate(R.layout.fragment_import_options_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        param1?.let {
            parseSwsManifest(it)
            createForm()
        } ?: run {
            context?.toast("error?")
        }
        url=param2
        importOptionsFormOkButton.setOnClickListener(clickListener)
    }

    fun createForm() {


        val spinner = activity?.findViewById(R.id.importOptionsFormMethodSpinner) as Spinner
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item)
        downloadOptionsList = swsManifest!!.DownloadOptions.values.toList().sortedBy { if(it.methodId=="folder") 0 else 1 }
        adapter.addAll(downloadOptionsList!!.mapIndexed { index:Int, downloadOption ->
            val extraString=if(index==0){"(Recommended)"} else {""}
            "${downloadOption.name}(${downloadOption.methodObject!!.downloadMethodName})$extraString"
        }.toMutableList())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.onItemSelectedListener = this;

// Apply the adapter to the spinner
        spinner.adapter = adapter
        getStationIndex().storageLocations.forEach({id, storageLocation ->
            val radioButton = (activity?.findViewById<View>(getResources().getIdentifier(storageLocation.radioButtonId, "id", context!!.getPackageName())) as RadioButton)
            radioButton.text=storageLocation.name
            radioButton.isEnabled=storageLocation.isAvailable()
            radioButton.isChecked= storageLocation.isDefault

        })
    }

    fun parseSwsManifest(swsManifestString: String) {
        val gson = GsonBuilder().create()
        swsManifest = gson.fromJson(swsManifestString, object : TypeToken<SwsManifest>() {}.type);
        swsManifest!!.DownloadOptions.forEach({
            key, value ->
            Log.w("a", value.name)
            value.methodObject= downloadMethodsLookup.get(value.methodId)

        })
        Log.w("M", swsManifest.toString())
    }


    override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
        Log.w("P", "Spinner selected $pos")
        selectedDownloadOption = downloadOptionsList!![pos]
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }


    val clickListener = View.OnClickListener { view ->

        when (view.id) {
            R.id.importOptionsFormOkButton -> {
                selectedDownloadOption?.let {
                    val baseUrl=url+it.fileName
                    val selectedId = importOptionsFormStorageGroup.getCheckedRadioButtonId()
                    val storageLocation= getStationIndex().storageLocations.filterValues { getResources().getIdentifier(it.radioButtonId, "id", context!!.getPackageName())==selectedId }.toList()[0]
                    it.methodObject!!.downloadFromUrl(context!!, baseUrl)
                    val intent = Intent(activity, MainActivity::class.java)
                            .apply {

                            }
                    startActivity(intent)
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
         * @return A new instance of fragment ImportOptionsFormFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(swsManifestString: String, url: String) =
                ImportOptionsFormFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, swsManifestString)
                        putString(ARG_PARAM2, url)
                    }
                }
    }
}

interface DownloadMethod {
    val downloadMethodName: String
    fun downloadFromUrl(context: Context, url: String)
}

class DownloadMethodFolder : DownloadMethod {
    override fun downloadFromUrl(context: Context, url: String) {
        val intent: Intent = Intent(context, DownloaderService::class.java)
        //intent.putExtra("firstTrackUri", trackUri.toString())
        intent.putExtra("url", url)
        intent.putExtra("method", "folder")
        intent.action = "download"
        context.startService(intent)
    }

    override val downloadMethodName: String
        get() = "Folder"

}

class DownloadMethodRaArchive : DownloadMethod {
    override fun downloadFromUrl(context: Context, url: String) {
        //Log.w("T", "TODO DOWNLOAD FUN")
        val intent: Intent = Intent(context, DownloaderService::class.java)
        //intent.putExtra("firstTrackUri", trackUri.toString())
        intent.putExtra("url", url)
        intent.putExtra("method", "ra")
        intent.action = "download"
        context.startService(intent)
    }

    override val downloadMethodName: String
        get() = "Radio Archive"
}

val downloadMethodsLookup = mapOf<String, DownloadMethod>("ra" to DownloadMethodRaArchive(), "folder" to DownloadMethodFolder())

data class SwsManifest(
        val name: String,
        @com.google.gson.annotations.SerializedName("download_options")
        val DownloadOptions: Map<String, DownloadOption>

)

data class DownloadOption(
        val name: String,
        @com.google.gson.annotations.SerializedName("method")
        val methodId: String,
        @com.google.gson.annotations.SerializedName("file_name")
        val fileName: String
) {
    var methodObject:DownloadMethod?=null
    var valid:Boolean=true
/*    init {
        Log.w("I","DDDAANNKK")

        if (methodId in downloadMethodsLookup.keys) {
            methodObject = downloadMethodsLookup.get(methodId)!!
            Log.w("I","Valid")

        } else {
            Log.w("I","Invalid")
            valid=false
        }
    }*/
}

enum class DownloadMethods(val method: String) {
    RA("ra"),
    RA_GZ("ra.gz"),
    FOLDER("folder")
}