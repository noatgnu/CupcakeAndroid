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

@AndroidEntryPoint
class StorageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStorageBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStorageBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        initializeStorage()
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
}