package info.proteo.cupcake.ui.base

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import info.proteo.cupcake.R

/**
 * Base activity that handles edge-to-edge display setup for Android 15+
 * All activities should extend this to ensure consistent system bar handling
 */
abstract class BaseActivity<T : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle edge-to-edge for Android 15+ while maintaining colored status bar
        setupEdgeToEdge()
        
        // Inflate the binding
        binding = createBinding()
        setContentView(binding.root)
        
        // Apply window insets to handle system bars properly
        setupWindowInsets()
        
        // Call the activity-specific setup
        onCreateBinding(savedInstanceState)
    }

    /**
     * Create the ViewBinding instance for this activity
     */
    protected abstract fun createBinding(): T

    /**
     * Called after binding is created and window insets are set up
     * Activities should override this instead of onCreate
     */
    protected abstract fun onCreateBinding(savedInstanceState: Bundle?)

    /**
     * Sets up edge-to-edge display with proper status bar handling
     */
    private fun setupEdgeToEdge() {
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Make navigation bar transparent first
        window.navigationBarColor = Color.TRANSPARENT
        
        // Ensure status bar icons are light (white) for dark primary color
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false
        windowInsetsController.isAppearanceLightNavigationBars = true // Light icons for transparent nav bar
    }
    
    /**
     * Call this method after the toolbar is set up to sync colors
     */
    protected fun syncStatusBarWithToolbar(toolbar: androidx.appcompat.widget.Toolbar) {
        // Post to ensure toolbar is fully laid out and styled
        toolbar.post {
            val toolbarBackground = toolbar.background
            if (toolbarBackground is android.graphics.drawable.ColorDrawable) {
                // Get the actual color from the toolbar's background
                val toolbarColor = toolbarBackground.color
                window.statusBarColor = toolbarColor
            } else {
                // Fallback to theme color
                val typedArray = theme.obtainStyledAttributes(intArrayOf(
                    com.google.android.material.R.attr.colorPrimary
                ))
                val primaryColor = typedArray.getColor(0, ContextCompat.getColor(this, R.color.primary))
                typedArray.recycle()
                window.statusBarColor = primaryColor
            }
        }
    }
    
    /**
     * Sets up window insets handling for proper edge-to-edge support
     * Can be overridden by activities that need custom inset handling
     */
    protected open fun setupWindowInsets() {
        // Apply insets to the root view to handle system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Only apply top padding for status bar (no bottom padding since nav bar is transparent)
            view.setPadding(
                view.paddingLeft,
                systemBars.top,
                view.paddingRight,
                0 // No bottom padding - let content flow to bottom
            )
            
            windowInsets
        }
    }

    /**
     * Helper method for activities with AppBarLayout to handle insets properly
     */
    protected fun setupAppBarInsets(appBarView: android.view.View) {
        ViewCompat.setOnApplyWindowInsetsListener(appBarView) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply top padding to AppBarLayout to account for status bar
            view.setPadding(
                view.paddingLeft,
                systemBars.top,
                view.paddingRight,
                view.paddingBottom
            )
            
            windowInsets
        }
        
        // Don't apply any insets to root when app bar handles them and nav bar is transparent
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            view.setPadding(
                view.paddingLeft,
                0, // App bar handles top insets
                view.paddingRight,
                0  // No bottom padding - transparent nav bar
            )
            
            windowInsets
        }
    }
}