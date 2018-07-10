package tech.ivar.radio

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_add_repo.*

class AddRepoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_repo)
        addRepoAdd.setOnClickListener(clickListener)

    }
    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.addRepoAdd -> {
                Log.w("C","CLICK")
                var url=AddRepoUrl.text

            }
        }

    }
}
