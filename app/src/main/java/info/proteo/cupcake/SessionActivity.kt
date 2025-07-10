package info.proteo.cupcake

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.ActivitySessionBinding
import info.proteo.cupcake.ui.session.SessionFragment

@AndroidEntryPoint
class SessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var isDualPane: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge with transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        binding = ActivitySessionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up status bar background
        setupStatusBarBackground()

        // Check if we have dual-pane layout
        isDualPane = findViewById<View>(R.id.session_detail_container) != null
        
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
                title = "Session"
            }
            
            // Set toolbar colors
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
            toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
            toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.white))
        }

        // Get session arguments
        val protocolId = intent.getIntExtra("protocolId", -1)
        val sessionId = intent.getStringExtra("sessionId") ?: ""
        val isNewSession = intent.getBooleanExtra("isNewSession", false)

        // Create session fragment with arguments
        val sessionFragment = SessionFragment()
        val args = Bundle().apply {
            putInt("protocolId", protocolId)
            putString("sessionId", sessionId)
            putBoolean("isNewSession", isNewSession)
        }
        sessionFragment.arguments = args

        // Add fragment to container
        supportFragmentManager.beginTransaction()
            .replace(R.id.session_fragment_container, sessionFragment)
            .commit()

        // Set up sidebar for tablet mode
        setupTabletSidebar(sessionFragment)

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun setupSinglePaneLayout() {
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        
        // Set toolbar colors for single pane mode
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.white))

        // Set up navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up the graph with the arguments
        val protocolId = intent.getIntExtra("protocolId", -1)
        val sessionId = intent.getStringExtra("sessionId") ?: ""
        val isNewSession = intent.getBooleanExtra("isNewSession", false)

        // Create a bundle for the arguments
        val args = Bundle().apply {
            putInt("protocolId", protocolId)
            putString("sessionId", sessionId)
            putBoolean("isNewSession", isNewSession)
        }

        // Set the graph with arguments
        navController.setGraph(R.navigation.session_nav_graph, args)

        // Set up ActionBar with NavController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Handle back press with the new approach
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navController.navigateUp()) {
                    finish()
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (isDualPane) {
            finish()
            true
        } else {
            navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        }
    }

    private fun setupStatusBarBackground() {
        // Get the actual resolved color from the theme
        val typedArray = theme.obtainStyledAttributes(intArrayOf(
            android.R.attr.colorPrimary
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
                val typedValue = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)
                val actionBarHeight = resources.getDimensionPixelSize(typedValue.resourceId)
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

    private fun setupTabletSidebar(sessionFragment: SessionFragment) {
        // Wait for fragment to be ready and get the sidebar adapter
        sessionFragment.viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                val sidebarAdapter = sessionFragment.getSidebarAdapter()
                if (sidebarAdapter != null) {
                    binding.sidebarRecyclerView?.apply {
                        layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@SessionActivity)
                        adapter = sidebarAdapter
                    }
                }
            }
        })
    }
}