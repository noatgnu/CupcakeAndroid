package info.proteo.cupcake

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.ActivityStoredReagentBinding
import info.proteo.cupcake.ui.barcode.BarcodeScannerFragment
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

            loadStoredReagentFragment(storageObjectId)

            if (intent.getBooleanExtra(EXTRA_OPEN_SCANNER, false)) {
                openBarcodeScanner(storageObjectId)
            }
            if (intent.hasExtra(EXTRA_SEARCH_TERM)) {
                val searchTerm = intent.getStringExtra(EXTRA_SEARCH_TERM)
                if (!searchTerm.isNullOrEmpty()) {
                    binding.textViewSearchIndicator.visibility = View.VISIBLE
                    binding.textViewSearchIndicator.text = "Searching: $searchTerm"
                }
            }
        }
    }

    private fun openBarcodeScanner(storageObjectId: Int) {
        val barcodeFragment = BarcodeScannerFragment()

        barcodeFragment.setOnBarcodeDetectedListener { barcode ->
            supportFragmentManager.setFragmentResult(
                "barcode_result",
                Bundle().apply {
                    putString("barcode", barcode)
                    putInt("storage_object_id", storageObjectId)
                }
            )
            supportFragmentManager.popBackStack()
        }

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
    fun updateLocation(locationName: String) {
        binding.textViewLocation.text = locationName
    }
}