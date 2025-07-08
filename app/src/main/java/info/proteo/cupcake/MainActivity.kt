package info.proteo.cupcake

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.ActivityMainBinding
import info.proteo.cupcake.databinding.NavHeaderUserProfileBinding
import info.proteo.cupcake.ui.navigation.NavigationHeaderViewModel
import kotlinx.coroutines.launch
import androidx.core.view.get
import androidx.core.view.size


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val navigationHeaderViewModel: NavigationHeaderViewModel by viewModels()
    private var headerBinding: NavHeaderUserProfileBinding? = null
    
    // Dual-pane layout detection
    private val isDualPane: Boolean by lazy {
        findViewById<View>(R.id.detail_container) != null
    }


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
        
        setupNavigationHeader()

        // Make all toolbar elements white
        binding.appBarMain.toolbar.apply {
            // Set title text color to white
            setTitleTextColor(ContextCompat.getColor(context, R.color.white))

            // Set navigation icon (hamburger menu) to white
            navigationIcon?.setTint(ContextCompat.getColor(context, R.color.white))

            // Set overflow menu icon (three dots) to white
            overflowIcon?.setTint(ContextCompat.getColor(context, R.color.white))
        }

        binding.navView?.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_storage,
                R.id.nav_instruments,
                R.id.nav_messages,
                R.id.nav_timekeepers,
                R.id.nav_metadata,
                R.id.nav_protocols,
                R.id.nav_lab_groups -> {
                    navigateToSection(menuItem.itemId)
                    true
                }
                else -> {
                    navController.navigate(menuItem.itemId)
                    binding.drawerLayout?.closeDrawers()
                    true
                }
            }
        }
    }
    
    private fun setupNavigationHeader() {
        // Get the header view from NavigationView
        val headerView = binding.navView?.getHeaderView(0)
        headerView?.let { header ->
            headerBinding = NavHeaderUserProfileBinding.bind(header)
            
            // Set click listener on the header to navigate to profile
            headerBinding?.navHeaderLayout?.setOnClickListener {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.userProfileFragment)
                binding.drawerLayout?.closeDrawers()
            }
            
            // Observe user data
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    navigationHeaderViewModel.uiState.collect { state ->
                        updateNavigationHeader(state)
                    }
                }
            }
        }
    }
    
    private fun updateNavigationHeader(state: info.proteo.cupcake.ui.navigation.NavigationHeaderUiState) {
        headerBinding?.apply {
            // Loading state
            progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            
            // Content visibility
            if (state.user != null) {
                profileContent.visibility = View.VISIBLE
                errorState.visibility = View.GONE
                
                // User data
                val fullName = listOfNotNull(state.user.firstName, state.user.lastName)
                    .joinToString(" ")
                    .ifBlank { state.user.username }
                
                tvUserName.text = fullName
                tvUsername.text = "@${state.user.username}"
                
                // Lab groups count
                val labGroupsText = when (state.labGroupsCount) {
                    0 -> "No lab groups"
                    1 -> "1 Lab Group"
                    else -> "${state.labGroupsCount} Lab Groups"
                }
                tvLabGroupsCount.text = labGroupsText
                
                // Staff badge
                chipStaff.visibility = if (state.isStaff) View.VISIBLE else View.GONE
                
            } else if (state.error != null) {
                profileContent.visibility = View.GONE
                errorState.visibility = View.VISIBLE
            } else {
                profileContent.visibility = View.GONE
                errorState.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when returning to main activity
        navigationHeaderViewModel.refresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Apply white tint to all menu items
        for (i in 0 until menu.size) {
            val item = menu[i]
            item.icon?.setTint(ContextCompat.getColor(this, R.color.white))
        }

        return true
    }
    
    /**
     * Shows a fragment in the detail pane for dual-pane layouts
     */
    fun showDetailFragment(fragment: androidx.fragment.app.Fragment, title: String? = null) {
        if (isDualPane) {
            val detailContainer = findViewById<View>(R.id.detail_container)
            detailContainer?.visibility = View.VISIBLE
            
            supportFragmentManager.beginTransaction()
                .replace(R.id.detail_container, fragment)
                .commit()
                
            // Update breadcrumb if available
            updateBreadcrumb(title)
        }
    }
    
    /**
     * Hides the detail pane
     */
    fun hideDetailPane() {
        if (isDualPane) {
            val detailContainer = findViewById<View>(R.id.detail_container)
            detailContainer?.visibility = View.GONE
            updateBreadcrumb(null)
        }
    }
    
    /**
     * Updates breadcrumb navigation for tablet layouts
     */
    private fun updateBreadcrumb(detailTitle: String?) {
        val breadcrumbContainer = binding.appBarMain.toolbar.findViewById<View>(R.id.breadcrumb_container)
        val breadcrumbDetail = binding.appBarMain.toolbar.findViewById<android.widget.TextView>(R.id.breadcrumb_detail)
        
        breadcrumbContainer?.visibility = if (detailTitle != null) View.VISIBLE else View.GONE
        breadcrumbDetail?.text = detailTitle
    }
    
    /**
     * Checks if the current layout supports dual-pane
     */
    fun isDualPaneLayout(): Boolean = isDualPane
    
    /**
     * Navigates to a section, using dual-pane if available, otherwise starting an activity
     */
    private fun navigateToSection(sectionId: Int) {
        // For now, keep the existing activity-based navigation
        // Later we can implement fragment-based navigation for dual-pane
        when (sectionId) {
            R.id.nav_storage -> startActivity(Intent(this, StorageActivity::class.java))
            R.id.nav_instruments -> startActivity(Intent(this, InstrumentActivity::class.java))
            R.id.nav_messages -> startActivity(Intent(this, MessageActivity::class.java))
            R.id.nav_timekeepers -> startActivity(Intent(this, TimeKeeperActivity::class.java))
            R.id.nav_metadata -> startActivity(Intent(this, MetadataActivity::class.java))
            R.id.nav_protocols -> startActivity(Intent(this, ProtocolActivity::class.java))
            R.id.nav_lab_groups -> startActivity(Intent(this, info.proteo.cupcake.ui.labgroup.LabGroupManagementActivity::class.java))
        }
        binding.drawerLayout?.closeDrawers()
    }
}