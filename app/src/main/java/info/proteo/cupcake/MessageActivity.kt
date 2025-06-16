package info.proteo.cupcake

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.ActivityMessageBinding

@AndroidEntryPoint
class MessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    override fun onSupportNavigateUp(): Boolean {
        val currentDestination = navController.currentDestination?.id
        return if (currentDestination == R.id.messageFragment) {
            finish()
            true
        } else {
            navController.navigateUp() || super.onSupportNavigateUp()
        }
    }
}