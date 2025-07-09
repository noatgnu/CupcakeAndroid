package info.proteo.cupcake

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.ActivityProtocolBinding
import info.proteo.cupcake.ui.protocol.ProtocolDetailFragment
import info.proteo.cupcake.ui.protocol.ProtocolListFragment

@AndroidEntryPoint
class ProtocolActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProtocolBinding
    private var isDualPane: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge with transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        binding = ActivityProtocolBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up status bar background
        setupStatusBarBackground()
        
        // Check if we have dual-pane layout
        isDualPane = findViewById<View>(R.id.protocol_detail_container) != null
        
        if (isDualPane) {
            setupDualPaneLayout()
        } else {
            setupSinglePaneLayout()
        }
    }

    private fun setupDualPaneLayout() {
        // Set up toolbar
        binding.toolbar?.let { toolbar ->
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
                title = "Protocols"
            }
            
            // Set toolbar colors
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
            toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
        }

        // Set up protocol list fragment communication
        val listFragment = supportFragmentManager.findFragmentById(R.id.protocol_list_fragment) as? ProtocolListFragment
        listFragment?.setOnProtocolSelectedListener(object : ProtocolListFragment.OnProtocolSelectedListener {
            override fun onProtocolSelected(protocolId: Int) {
                showProtocolDetail(protocolId)
            }
        })
    }

    private fun setupSinglePaneLayout() {
        // For single-pane layout, fragments will handle their own toolbar setup
        // No need to set up action bar at activity level since there's no toolbar in the layout
    }

    private fun showProtocolDetail(protocolId: Int) {
        if (isDualPane) {
            // Hide empty state and show detail fragment
            binding.emptyDetailState?.visibility = View.GONE
            binding.protocolDetailFragment?.visibility = View.VISIBLE
            
            // Create and show protocol detail fragment
            val detailFragment = ProtocolDetailFragment.newInstance(protocolId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.protocol_detail_fragment, detailFragment)
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (isDualPane) {
            finish()
            true
        } else {
            // For single-pane layout, let fragments handle navigation
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.protocol_nav_host_fragment) as NavHostFragment
            navHostFragment.navController.navigateUp() || finish().let { true }
        }
    }

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