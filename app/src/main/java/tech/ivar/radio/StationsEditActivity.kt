package tech.ivar.radio

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_stations_edit.*


class StationsEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stations_edit)

        setSupportActionBar(stationsEditActivityToolbar)
        val fragment = StationsEditFragment()
        val fragmentTransaction = this.supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.StationsEditActivityFragmentContainer, fragment)
        fragmentTransaction.commit()


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    //override fun onC


}
