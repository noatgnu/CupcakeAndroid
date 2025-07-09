package info.proteo.cupcake

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.ActivityInstrumentBinding
import info.proteo.cupcake.ui.barcode.BarcodeScannerFragment
import info.proteo.cupcake.ui.instrument.InstrumentViewModel
import info.proteo.cupcake.ui.main.MainFragment
import info.proteo.cupcake.ui.instrument.InstrumentFragment
import info.proteo.cupcake.ui.instrument.InstrumentDetailFragment

@AndroidEntryPoint
class InstrumentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInstrumentBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var isDualPane: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge with transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        binding = ActivityInstrumentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up window insets and status bar background
        setupStatusBarBackground()

        setSupportActionBar(binding.toolbar)
        
        // Check if we have dual-pane layout
        isDualPane = findViewById<View>(R.id.instrument_detail_fragment) != null
        
        if (isDualPane) {
            setupDualPaneLayout()
        } else {
            setupSinglePaneLayout()
        }

        // Make all toolbar elements white
        binding.toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(this, R.color.white)
        )
        binding.toolbar.setTitleTextColor(
            ContextCompat.getColor(this, R.color.white)
        )
        binding.toolbar.overflowIcon?.setTint(
            ContextCompat.getColor(this, R.color.white)
        )

    }
    
    private fun setupDualPaneLayout() {
        // Set up toolbar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Instruments"
        }
        
        // Set up instrument list fragment communication
        val listFragment = supportFragmentManager.findFragmentById(R.id.instrument_list_fragment) as? InstrumentFragment
        listFragment?.setOnInstrumentSelectedListener(object : InstrumentFragment.OnInstrumentSelectedListener {
            override fun onInstrumentSelected(instrumentId: Int) {
                showInstrumentDetail(instrumentId)
            }
        })
    }
    
    private fun setupSinglePaneLayout() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.instrument_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(),
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }
    
    private fun showInstrumentDetail(instrumentId: Int) {
        if (isDualPane) {
            // Hide empty state and show detail fragment
            binding.emptyInstrumentState?.visibility = View.GONE
            binding.instrumentDetailFragment?.visibility = View.VISIBLE
            
            // Create and show instrument detail fragment
            val detailFragment = InstrumentDetailFragment.newInstance(instrumentId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.instrument_detail_fragment, detailFragment)
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_instrument, menu)

        // Configure the search view
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = "Search by name or serial number"

        // Update to use custom search icon
        try {
            val searchIcon = searchView.findViewById<androidx.appcompat.widget.AppCompatImageView>(
                androidx.appcompat.R.id.search_mag_icon
            )
            searchIcon?.setImageResource(R.drawable.outline_feature_search_24)
            searchIcon?.setColorFilter(
                ContextCompat.getColor(this, R.color.white),
                android.graphics.PorterDuff.Mode.SRC_IN
            )

            // For the search button (when collapsed)
            val searchButton = searchView.findViewById<androidx.appcompat.widget.AppCompatImageView>(
                androidx.appcompat.R.id.search_button
            )
            searchButton?.setImageResource(R.drawable.outline_feature_search_24)
            searchButton?.setColorFilter(
                ContextCompat.getColor(this, R.color.white),
                android.graphics.PorterDuff.Mode.SRC_IN
            )

            // Make the clear (X) button white
            val closeIcon = searchView.findViewById<androidx.appcompat.widget.AppCompatImageView>(
                androidx.appcompat.R.id.search_close_btn
            )
            closeIcon?.setColorFilter(
                ContextCompat.getColor(this, R.color.white),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        } catch (e: Exception) {
            Log.e("InstrumentActivity", "Error customizing search view: ${e.message}")
        }

        // Setup search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Broadcast the search query to the currently displayed fragment
                broadcastSearchQuery(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    broadcastSearchQuery(null)
                }
                return true
            }
        })

        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (isDualPane) {
            finish()
            true
        } else {
            navController.navigateUp() || super.onSupportNavigateUp()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_scan_barcode -> {
                openBarcodeScanner()
                true
            }
            android.R.id.home -> {
                if (isDualPane) {
                    finish()
                } else {
                    if (!navController.popBackStack()) {
                        finish()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openBarcodeScanner() {
        // Setup fragment result listener for barcode scanner
        supportFragmentManager.setFragmentResultListener(
            "barcode_result",
            this
        ) { _, bundle ->
            val barcode = bundle.getString("barcode")
            Log.d("InstrumentActivity", "Barcode scanned: $barcode")

            barcode?.let {
                // Broadcast the barcode scan result
                broadcastSearchQuery(it, true)
            }
        }

        // Launch barcode scanner
        val scannerFragment = BarcodeScannerFragment()
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, scannerFragment, "barcode_scanner")
            .addToBackStack("barcode_scanner")
            .commit()
    }

    // Method to broadcast search queries to the currently active fragment
    private fun broadcastSearchQuery(query: String?, isBarcode: Boolean = false) {
        if (isDualPane) {
            // In dual-pane mode, broadcast to instrument list fragment
            val listFragment = supportFragmentManager.findFragmentById(R.id.instrument_list_fragment)
            if (listFragment is SearchQueryListener) {
                listFragment.onSearchQuery(query, isBarcode)
            }
        } else {
            // Get the current fragment
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.instrument_nav_host_fragment) as NavHostFragment
            val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()

            // Check if the current fragment implements the SearchQueryListener interface
            if (currentFragment is SearchQueryListener) {
                currentFragment.onSearchQuery(query, isBarcode)
            }
        }
    }

    // Interface for fragments to implement if they want to receive search queries
    interface SearchQueryListener {
        fun onSearchQuery(query: String?, isBarcode: Boolean = false)
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
            binding.toolbar?.let { toolbar ->
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