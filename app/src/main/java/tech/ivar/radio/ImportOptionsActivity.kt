package tech.ivar.radio

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.kittinunf.fuel.httpGet

class ImportOptionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_import_options)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val loadingFragment = ImportOptionsLoadingFragment.newInstance("","")
        openFragment(loadingFragment)

        val extras = intent.extras!!
        val url:String=extras.getString("import_web_url")
        if(url.endsWith(".ra")) {
            val optionsFragment = ImportOptionsFormFragment.newInstance("RA",url)
            openFragment(optionsFragment)
        }else {
            downloadSwsManifest(url)
        } /*
        var value = -1 // or other values
        if (b != null)
            value = b.getInt("key")
            */
    }

    fun downloadSwsManifest(url_: String) {
        val url:String=if(url_.endsWith("/")) {url_} else {
            "$url_/"
        }

        (url+"sws.json").httpGet().responseString { _, _, result ->

            val (text, error) = result
            if (text != null) {
                val optionsFragment = ImportOptionsFormFragment.newInstance(text,url)
                openFragment(optionsFragment)
            } else {
                Log.w("W", error.toString())
            }
        }
    }
    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.ImportOptionsFragment, fragment)
        //transaction.addToBackStack(null)
        transaction.commit()
    }
}
