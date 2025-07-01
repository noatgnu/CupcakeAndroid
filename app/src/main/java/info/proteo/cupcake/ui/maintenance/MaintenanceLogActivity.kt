package info.proteo.cupcake.ui.maintenance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.ActivityMaintenanceLogBinding

@AndroidEntryPoint
class MaintenanceLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaintenanceLogBinding
    private lateinit var navController: NavController

    companion object {
        private const val EXTRA_INSTRUMENT_ID = "extra_instrument_id"
        private const val EXTRA_INSTRUMENT_NAME = "extra_instrument_name"

        fun createIntent(context: Context, instrumentId: Long, instrumentName: String?): Intent {
            return Intent(context, MaintenanceLogActivity::class.java).apply {
                putExtra(EXTRA_INSTRUMENT_ID, instrumentId)
                putExtra(EXTRA_INSTRUMENT_NAME, instrumentName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMaintenanceLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupToolbar()
        navigateToMaintenanceLog()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        
        // Don't set up action bar with nav controller initially - do it after navigation is set up
        val instrumentName = intent.getStringExtra(EXTRA_INSTRUMENT_NAME)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = if (instrumentName != null) {
                "Maintenance - $instrumentName"
            } else {
                "Maintenance Logs"
            }
        }

        // Set toolbar colors
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
    }

    private fun navigateToMaintenanceLog() {
        val instrumentId = intent.getLongExtra(EXTRA_INSTRUMENT_ID, -1L)
        if (instrumentId == -1L) {
            finish()
            return
        }

        // Set the nav graph with the instrument ID as start destination arguments
        navController.setGraph(R.navigation.maintenance_nav_graph, Bundle().apply {
            putLong("instrumentId", instrumentId)
        })
        
        // Set up action bar with nav controller after graph is set
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}