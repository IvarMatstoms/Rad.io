package tech.ivar.radio

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_import.*
import kotlinx.android.synthetic.main.activity_stations_edit.*


class StationsEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stations_edit)

        setSupportActionBar(stationsEditActivityToolbar)
        //Log.w("A","HII")
        val fragment = StationsEditFragment()
        val fragmentTransaction = this.supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.StationsEditActivityFragmentContainer, fragment)
        //fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    //override fun onC


}
