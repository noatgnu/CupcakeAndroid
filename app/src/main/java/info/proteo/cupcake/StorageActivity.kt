package info.proteo.cupcake

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.ActivityStorageBinding
import info.proteo.cupcake.ui.storage.StorageFragment
import androidx.core.view.WindowCompat
import info.proteo.cupcake.ui.storage.StorageHierarchyAdapter

@AndroidEntryPoint
class StorageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStorageBinding
    private var isDualPane: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge with transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        binding = ActivityStorageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupStatusBarBackground()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Storage"

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

        // Check if we have dual-pane layout
        isDualPane = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewHierarchy) != null
        
        initializeStorage()
        
        if (isDualPane) {
            setupDualPaneLayout()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initializeStorage() {
        val storageFragment = StorageFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.storage_container, storageFragment)
            .commit()
    }
    
    private fun setupDualPaneLayout() {
        // Set up hierarchy RecyclerView for tablet layout
        binding.recyclerViewHierarchy?.let { hierarchyRecyclerView ->
            hierarchyRecyclerView.layoutManager = LinearLayoutManager(this)
            
            // Create a simple adapter for storage hierarchy
            val hierarchyItems = listOf(
                "Home",
                "Building A",
                "Floor 1",
                "Room 101",
                "Fridge 1",
                "Shelf A"
            )
            
            val hierarchyAdapter = StorageHierarchyAdapter(hierarchyItems) { item ->
                // Handle hierarchy navigation
                navigateToHierarchyItem(item)
            }
            
            hierarchyRecyclerView.adapter = hierarchyAdapter
        }
    }
    
    private fun navigateToHierarchyItem(item: String) {
        // TODO: Implement navigation to specific storage hierarchy item
        // For now, just show a toast
        android.widget.Toast.makeText(this, "Navigate to: $item", android.widget.Toast.LENGTH_SHORT).show()
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