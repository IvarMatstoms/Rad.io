package tech.ivar.radio

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_repo_edit.*
import kotlinx.android.synthetic.main.fragment_find.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton
import java.io.File

class RepoEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repo_edit)

        var viewManager = LinearLayoutManager(this)

        getRepoIndex().verifyLoaded(this)
        var viewAdapter = EditRepoListAdapter(this, getRepoIndex().repos.toTypedArray())

        var recyclerView = editRepoRV.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

    }

}

class EditRepoListAdapter(val context: Context, private val repos: Array<RepoReference>) :
        RecyclerView.Adapter<EditRepoListAdapter.ViewHolder>() {

    class ViewHolder(val listItem: ConstraintLayout) : RecyclerView.ViewHolder(listItem)


    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): EditRepoListAdapter.ViewHolder {
        // create a new view
        val listItem = LayoutInflater.from(parent.context)
                .inflate(R.layout.edit_repo_list_item, parent, false) as ConstraintLayout

        return ViewHolder(listItem)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val repo=repos[position]
        holder.listItem.findViewById<TextView>(R.id.editRepoName).text = repo.name


        //findListDownloadButton
        val clickListener = View.OnClickListener { view ->
            context.alert("This can not be undone","Delete \"${repo.name}\"") {
                yesButton {
                    getRepoIndex().verifyLoaded(context)
                    getRepoIndex().removeRepo(context,repo)
                    context.toast("remove will disappear on exit")
                }
                noButton {}
            }.show()
            /*
            val intent = Intent(context, ImportOptionsActivity::class.java)
            val b = Bundle()

            val url="${station.repoReference.url}/stations/${station.repoStation.id}/"
            b.putString("import_type", "web") //Your id
            b.putString("import_web_url", url) //Your id

            intent.putExtras(b) //Put your id to your next Intent
            context.startActivity(intent)
            */
        }
        holder.listItem.findViewById<ImageButton>(R.id.editRepoDelete).setOnClickListener (clickListener)

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
    override fun getItemCount() = repos.size
}

