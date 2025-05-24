package info.proteo.cupcake

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}