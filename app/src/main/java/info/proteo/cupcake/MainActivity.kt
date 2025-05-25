package info.proteo.cupcake

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.ActivityMainBinding


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_main),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView?.setupWithNavController(navController)

        binding.navView?.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_storage -> {
                    val intent = Intent(this, StorageActivity::class.java)
                    startActivity(intent)
                    binding.drawerLayout?.closeDrawers()
                    true
                }
                R.id.nav_messages -> {
                    val intent = Intent(this, MessageActivity::class.java)
                    startActivity(intent)
                    binding.drawerLayout?.closeDrawers()
                    true
                }
                else -> {
                    navController.navigate(menuItem.itemId)
                    binding.drawerLayout?.closeDrawers()
                    true
                }
            }
        }

        if (savedInstanceState == null) {
            binding.root.post {
                navController.navigate(R.id.nav_main)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }


}