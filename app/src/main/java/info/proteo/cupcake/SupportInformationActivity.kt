package info.proteo.cupcake

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.ActivitySupportInformationBinding
import info.proteo.cupcake.ui.instrument.SupportInformationFragment

@AndroidEntryPoint
class SupportInformationActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySupportInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge with transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        binding = ActivitySupportInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupStatusBarBackground()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Support Information"
        
        // Set toolbar colors
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.white))

        val instrumentId = intent.getIntExtra("INSTRUMENT_ID", -1)
        if (instrumentId != -1) {
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SupportInformationFragment.newInstance(instrumentId))
                    .commit()
            }
        } else {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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
            binding.toolbar.let { toolbar ->
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
}