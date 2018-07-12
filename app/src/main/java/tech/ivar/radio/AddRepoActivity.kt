package tech.ivar.radio

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.github.kittinunf.fuel.httpGet
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_add_repo.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

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
                val url:String=AddRepoUrl.text.toString()
                addRepo(url)
            }
        }

    }

    fun addRepo(url_:String) {
        buttonState(false)
        val url:String=if(url_.endsWith("/")) {url_} else {
            "$url_/"
        }
        (url+"repo.json").httpGet().responseString { _, _, result ->

            val (text, error) = result
            if (error != null) {
                toast("Http error")
                buttonState(true)
                return@responseString
            }
            val gson = GsonBuilder().create()
            val repoFileData: RepoFileData = gson.fromJson(text, object : TypeToken<RepoFileData>() {}.type)
            alert(repoFileData.text,"Add \"${repoFileData.name}\"") {
                yesButton {
                    getRepoIndex().addRepo(this@AddRepoActivity, url, repoFileData)
                    val intent = Intent(this@AddRepoActivity, MainActivity::class.java)
                            .apply {

                            }
                    startActivity(intent)
                }
                noButton {buttonState(true)}
            }.show()
        }
    }

    fun buttonState (state:Boolean) {
        addRepoAdd.isEnabled=state
    }
}
