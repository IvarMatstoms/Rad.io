package tech.ivar.radio

import android.content.Context
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_repo_edit.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

class RepoEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repo_edit)

        var viewManager = LinearLayoutManager(this)

        getRepoIndex().verifyLoaded(this)
        var viewAdapter = EditRepoListAdapter(this, getRepoIndex().repos.toTypedArray())

        editRepoRV.apply {
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


        val clickListener = View.OnClickListener {
            context.alert(context.getString(R.string.cant_be_undone),context.getString(R.string.delete_string,repo.name)) {
                yesButton {
                    getRepoIndex().verifyLoaded(context)
                    getRepoIndex().removeRepo(context,repo)
                    context.toast(context.getString(R.string.item_will_disappear))
                }
                noButton {
                    context.toast(context.getString(R.string.canceled))
                }
            }.show()
        }
        holder.listItem.findViewById<ImageButton>(R.id.editRepoDelete).setOnClickListener (clickListener)


    }

    override fun getItemCount() = repos.size
}

