package tech.ivar.radio

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

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

    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        val songsFragment = StationsFragment.newInstance("","")
        openFragment(songsFragment)
        getStationIndex().loadIndex(this)
    }
}
