package info.proteo.cupcake

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.ActivityMessageBinding
import info.proteo.cupcake.ui.message.MessageFragment
import info.proteo.cupcake.ui.message.ThreadDetailFragment

@AndroidEntryPoint
class MessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageBinding
    private lateinit var navController: NavController
    private var isDualPane: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge with transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up window insets and status bar background
        setupStatusBarBackground()

        // Check if we have dual-pane layout
        isDualPane = findViewById<View>(R.id.thread_detail_container) != null
        
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
                title = "Messages"
            }
            
            // Set toolbar colors
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
            toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
            toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.white))
        }

        // Set up thread list fragment
        val messageFragment = MessageFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.thread_list_fragment, messageFragment)
            .commit()

        // Set up FAB for new message
        binding.fabNewMessage?.setOnClickListener {
            // TODO: Handle new message creation in dual-pane mode
            showNewMessageDialog()
        }

        // Set up message fragment communication
        setupMessageFragmentCallback(messageFragment)
    }

    private fun setupSinglePaneLayout() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.messages)

        // Make all toolbar elements white
        binding.toolbar.apply {
            // Set title text color to white
            setTitleTextColor(ContextCompat.getColor(context, R.color.white))

            // Set navigation icon (back button) to white
            navigationIcon?.setTint(ContextCompat.getColor(context, R.color.white))

            // Set overflow menu icon (three dots) to white
            overflowIcon?.setTint(ContextCompat.getColor(context, R.color.white))
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.fabNewMessage.setOnClickListener {
            navController.navigate(R.id.action_messageFragment_to_newThreadFragment)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.messageFragment -> binding.fabNewMessage.visibility = View.VISIBLE
                else -> binding.fabNewMessage.visibility = View.GONE
            }
        }
    }

    private fun setupMessageFragmentCallback(messageFragment: MessageFragment) {
        // Set up callback for when a thread is selected
        messageFragment.setOnThreadSelectedListener(object : MessageFragment.OnThreadSelectedListener {
            override fun onThreadSelected(threadId: Int) {
                showThreadDetail(threadId)
            }
        })
    }

    private fun showThreadDetail(threadId: Int) {
        if (isDualPane) {
            // Hide empty state and show detail fragment
            binding.emptyThreadState?.visibility = View.GONE
            binding.threadDetailFragment?.visibility = View.VISIBLE
            
            // Create and show thread detail fragment
            val detailFragment = ThreadDetailFragment.newInstance(threadId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.thread_detail_fragment, detailFragment)
                .commit()
        }
    }

    private fun showNewMessageDialog() {
        // TODO: Implement new message dialog for tablet mode
        // For now, just show a simple message
        android.widget.Toast.makeText(this, "New message functionality coming soon", android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (isDualPane) {
            finish()
            true
        } else {
            val currentDestination = navController.currentDestination?.id
            if (currentDestination == R.id.messageFragment) {
                finish()
                true
            } else {
                navController.navigateUp() || super.onSupportNavigateUp()
            }
        }
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