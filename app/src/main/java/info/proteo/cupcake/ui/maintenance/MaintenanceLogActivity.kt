package info.proteo.cupcake.ui.maintenance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
        
        // Set up edge-to-edge with transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        binding = ActivityMaintenanceLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up window insets and status bar background
        setupStatusBarBackground()

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

        // Set the nav graph first without arguments
        navController.setGraph(R.navigation.maintenance_nav_graph)

        // Navigate to the fragment with the proper arguments
        val bundle = Bundle().apply {
            putLong("instrumentId", instrumentId)
        }
        navController.navigate(R.id.maintenanceLogFragment, bundle)

        // Set up action bar with nav controller after graph is set
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        // Check if we're on the start destination (maintenance log list)
        if (navController.currentDestination?.id == R.id.maintenanceLogFragment) {
            // If we're on the main maintenance log screen, go back to instrument detail
            finish()
            return true
        }
        // Otherwise, use normal navigation (for going back from detail to list)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    
    /**
     * Set up the toolbar to extend into the status bar area
     */
    private fun setupStatusBarBackground() {
        // Get the actual resolved color from the theme
        val typedArray = theme.obtainStyledAttributes(intArrayOf(
            com.google.android.material.R.attr.colorPrimary
        ))
        val resolvedColor = typedArray.getColor(0, ContextCompat.getColor(this, R.color.primary))
        typedArray.recycle()
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Extend the toolbar to cover the status bar area
            binding.toolbar.let { toolbar ->
                // Set the toolbar background color
                toolbar.setBackgroundColor(resolvedColor)
                toolbar.elevation = 0f
                toolbar.alpha = 1.0f
                
                // Extend toolbar height to include status bar
                val toolbarParams = toolbar.layoutParams
                val actionBarHeight = resources.getDimensionPixelSize(androidx.appcompat.R.dimen.abc_action_bar_default_height_material)
                toolbarParams.height = actionBarHeight + systemBars.top
                toolbar.layoutParams = toolbarParams
                
                // Add top padding to toolbar content so it appears below status bar
                toolbar.setPadding(
                    toolbar.paddingLeft,
                    systemBars.top,
                    toolbar.paddingRight,
                    toolbar.paddingBottom
                )
            }
            
            windowInsets
        }
        
        // Set appropriate status bar appearance for both themes
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false
    }
}