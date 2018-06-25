package tech.ivar.radio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*




val MAIN_ACTIVITY_ACTION= "tech.ivar.radio.mainactivity.action"
class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                //toolbar.title = "Songs"
                val songsFragment = StationsFragment.newInstance("","")
                openFragment(songsFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                val songsFragment = NowPlayingFragment.newInstance("","")
                openFragment(songsFragment)
                return@OnNavigationItemSelectedListener true
                //message.setText(R.string.title_dashboard)

            }
            R.id.navigation_notifications -> {
                val songsFragment = SettingsFragment.newInstance("","")
                openFragment(songsFragment)
                return@OnNavigationItemSelectedListener true
                //message.setText(R.string.title_notifications)

            }
        }
        false
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status=intent.getStringExtra("status")
            Log.w("H","MAIN A CALL")
            if (status=="download_done") {
                val text = "Download complete"
                val duration = Toast.LENGTH_LONG

                val toast = Toast.makeText(applicationContext, text, duration)
                toast.show()
            } else if (status=="download_failed") {
                val text = "Download failed"
                val duration = Toast.LENGTH_LONG

                val toast = Toast.makeText(applicationContext, text, duration)
                toast.show()
            }
            // now you can call all your fragments method here
        }
    }

    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }



    override fun onCreate(savedInstanceState: Bundle?) {

        getStationIndex().loadIndex(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        val songsFragment = StationsFragment.newInstance("","")
        openFragment(songsFragment)

        val intentFilter = IntentFilter()
        intentFilter.addAction(MAIN_ACTIVITY_ACTION)
        registerReceiver(broadcastReceiver, intentFilter)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putInt("SelectedItemId", navigation.getSelectedItemId())
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val selectedItemId = savedInstanceState.getInt("SelectedItemId")
        navigation.setSelectedItemId(selectedItemId)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: IllegalArgumentException) {}

        super.onDestroy()
    }

    override fun onPause() {
        unregisterReceiver(
                broadcastReceiver)
        super.onPause()
    }
    override fun onResume() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(MAIN_ACTIVITY_ACTION)
        registerReceiver(broadcastReceiver, intentFilter)
        super.onResume()
    }

}
