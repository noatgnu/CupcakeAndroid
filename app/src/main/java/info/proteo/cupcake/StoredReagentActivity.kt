package info.proteo.cupcake

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.ActivityStoredReagentBinding
import info.proteo.cupcake.ui.barcode.BarcodeScannerFragment
import info.proteo.cupcake.ui.reagent.CreateStoredReagentFragment
import info.proteo.cupcake.ui.reagent.StoredReagentFragment
import info.proteo.cupcake.ui.reagent.StoredReagentViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.text.replace

@AndroidEntryPoint
class StoredReagentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STORAGE_OBJECT_ID = "extra_storage_object_id"
        const val EXTRA_OPEN_SCANNER = "extra_open_scanner"
        const val EXTRA_SEARCH_TERM = "extra_search_term"
        const val EXTRA_CREATE_REAGENT = "extra_create_reagent"
        const val EXTRA_STORAGE_NAME = "extra_storage_name"
    }

    private lateinit var binding: ActivityStoredReagentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoredReagentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        val storageObjectId = intent.getIntExtra(EXTRA_STORAGE_OBJECT_ID, -1)

        if (storageObjectId == -1) {
            supportActionBar?.title = "All Locations"
            binding.textViewLocation.text = "All Locations"
        }

        if (savedInstanceState == null) {
            if (intent.getBooleanExtra(EXTRA_CREATE_REAGENT, false)) {
                // Load create reagent fragment
                loadCreateReagentFragment(storageObjectId)
                supportActionBar?.title = "Create Reagent"
            } else if (intent.getBooleanExtra(EXTRA_OPEN_SCANNER, false)) {
                openBarcodeScanner(storageObjectId)
            } else {
                // Load regular stored reagent fragment
                loadStoredReagentFragment(storageObjectId)

                if (intent.hasExtra(EXTRA_SEARCH_TERM)) {
                    val searchTerm = intent.getStringExtra(EXTRA_SEARCH_TERM)
                    if (!searchTerm.isNullOrEmpty()) {
                        binding.textViewSearchIndicator.visibility = View.VISIBLE
                        binding.textViewSearchIndicator.text = "Searching: $searchTerm"
                    }
                }
            }
        }


    }

    private fun openBarcodeScanner(storageObjectId: Int) {
        // Set up result listener before navigating to the fragment
        supportFragmentManager.setFragmentResultListener(
            "barcode_result",
            this
        ) { _, bundle ->
            val barcode = bundle.getString("barcode")
            barcode?.let {
                // Handle the scanned barcode here
                binding.textViewSearchIndicator.visibility = View.VISIBLE
                binding.textViewSearchIndicator.text = "Searching: $it"

                // Refresh the fragment with search term
                val fragment = StoredReagentFragment().apply {
                    arguments = Bundle().apply {
                        putInt("STORAGE_OBJECT_ID", storageObjectId)
                        putString("SEARCH_TERM", it)
                    }
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
            }
        }

        // Navigate to barcode scanner fragment
        val barcodeFragment = BarcodeScannerFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, barcodeFragment)
            .addToBackStack("barcode_scanner")
            .commit()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Stored Reagents"

        // Set navigation icon color to white
        binding.toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(this, R.color.white)
        )

        // Set title text color to white
        binding.toolbar.setTitleTextColor(
            ContextCompat.getColor(this, R.color.white)
        )

        // Set overflow icon color to white
        binding.toolbar.overflowIcon?.setTint(
            ContextCompat.getColor(this, R.color.white)
        )

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadStoredReagentFragment(storageObjectId: Int) {
        val fragment = StoredReagentFragment().apply {
            arguments = Bundle().apply {
                putInt("STORAGE_OBJECT_ID", storageObjectId)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }


    private fun loadCreateReagentFragment(storageObjectId: Int) {
        // Get the storage name from intent extras
        val storageName = intent.getStringExtra(EXTRA_STORAGE_NAME) ?: ""

        // Create fragment with required arguments
        val fragment = CreateStoredReagentFragment().apply {
            arguments = Bundle().apply {
                putInt("storageObjectId", storageObjectId)
                putString("storageName", storageName)
            }
        }

        // Replace current fragment with create fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        // Update activity title
        supportActionBar?.title = "Create Reagent"
        updateLocation(storageName)
        binding.textViewSearchIndicator.visibility = View.GONE
    }

    fun updateLocation(locationName: String) {
        binding.textViewLocation.text = locationName
    }
}