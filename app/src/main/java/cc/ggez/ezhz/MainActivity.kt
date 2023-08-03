package cc.ggez.ezhz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import cc.ggez.ezhz.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val menu = intent?.getStringExtra("menu")
        Log.d(TAG, "onCreate: ${menu}")
        if (menu.equals("proxy")) {
            navController.navigate(R.id.navigation_frida)
        } else if (menu.equals("shared_pref")) {
            navController.navigate(R.id.navigation_shared_pref)
        } else if (menu.equals("frida")) {
            navController.navigate(R.id.navigation_proxy)
        }

        val navView: BottomNavigationView = binding.navView

        navController = binding.navHostFragmentActivityMain.getFragment<NavHostFragment>().navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_proxy, R.id.navigation_frida, R.id.navigation_shared_pref
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


    }

    override fun onNewIntent(intent: Intent?) {
        val menu = intent?.getStringExtra("menu")
        Log.d(TAG, "onActivityReenter: ${intent.toString()}")
        if (menu.equals("proxy")) {
            navController.navigate(R.id.navigation_frida)
        } else if (menu.equals("shared_pref")) {
            navController.navigate(R.id.navigation_shared_pref)
        } else if (menu.equals("frida")) {
            navController.navigate(R.id.navigation_proxy)
        }
        super.onNewIntent(intent)
    }
}