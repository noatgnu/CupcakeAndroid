package info.proteo.cupcake.ui.reagent

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.data.remote.model.reagent.StoredReagent
import info.proteo.cupcake.databinding.FragmentStoredReagentDetailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StoredReagentDetailFragment : Fragment() {

    private var _binding: FragmentStoredReagentDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StoredReagentDetailViewModel by viewModels()

    // Map of format values to display labels
    private val barcodeFormatMap = mapOf(
        "CODE128" to "CODE 128 (Auto)",
        "CODE128A" to "CODE 128 A",
        "CODE128B" to "CODE 128 B",
        "CODE128C" to "CODE 128 C",
        "EAN13" to "EAN-13",
        "EAN8" to "EAN-8",
        "UPC" to "UPC-A",
        "UPCE" to "UPC-E",
        "CODE39" to "CODE 39",
        "ITF14" to "ITF-14",
        "MSI" to "MSI",
        "pharmacode" to "Pharmacode",
        "codabar" to "Codabar"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStoredReagentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBarcodeFormatSpinner()
        setupObservers()

        arguments?.getInt("REAGENT_ID", -1)?.let { reagentId ->
            if (reagentId != -1) {
                viewModel.loadStoredReagent(reagentId)
            } else {
                showError("Invalid reagent ID")
            }
        }
    }

    private fun setupBarcodeFormatSpinner() {
        val formatLabels = barcodeFormatMap.values.toList()
        val formatValues = barcodeFormatMap.keys.toList()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, formatLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerBarcodeFormat.adapter = adapter
        binding.spinnerBarcodeFormat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.updateBarcodeFormat(formatValues[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.storedReagent.collectLatest { reagent ->
                reagent?.let {
                    displayReagentInfo(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.barcodeBitmap.collectLatest { bitmap ->
                binding.progressBarBarcode.visibility = View.GONE
                if (bitmap != null) {
                    binding.imageViewBarcode.setImageBitmap(bitmap)
                    binding.imageViewBarcode.visibility = View.VISIBLE
                    binding.textViewBarcodeError.visibility = View.GONE
                } else {
                    binding.imageViewBarcode.visibility = View.GONE
                    binding.textViewBarcodeError.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedBarcodeFormat.collectLatest { format ->
                val position = barcodeFormatMap.keys.toList().indexOf(format)
                if (position >= 0) {
                    binding.spinnerBarcodeFormat.setSelection(position)
                }
            }
        }
    }

    private fun displayReagentInfo(reagent: StoredReagent) {
        binding.textViewReagentName.text = reagent.reagent.name
        binding.textViewQuantity.text = "${reagent.quantity} ${reagent.reagent.unit}"
        binding.textViewCurrentQuantity.text = "${reagent.currentQuantity} ${reagent.reagent.unit}"
        binding.textViewDescription.text = "${reagent.notes}"
        binding.textViewOwner.text = reagent.user.username
        binding.textViewLastUpdated.text = reagent.updatedAt
        binding.textViewExpiryDate.text = reagent.expirationDate ?: "N/A"

        if (reagent.barcode !== null) {
            binding.progressBarBarcode.visibility = View.VISIBLE
            binding.imageViewBarcode.visibility = View.GONE
            binding.textViewBarcodeError.visibility = View.GONE
            viewModel.generateBarcode(reagent.barcode)
        } else {
            binding.progressBarBarcode.visibility = View.GONE
            binding.imageViewBarcode.visibility = View.GONE
            binding.textViewBarcodeError.visibility = View.VISIBLE
        }

    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.contentLayout.visibility = View.GONE
        binding.layoutError.visibility = View.VISIBLE
        binding.textViewError.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}