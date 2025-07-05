package info.proteo.cupcake.ui.instrument

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import info.proteo.cupcake.R
import info.proteo.cupcake.data.repository.InstrumentRepository
import info.proteo.cupcake.databinding.DialogCreateInstrumentBinding
import info.proteo.cupcake.shared.data.model.instrument.Instrument
import kotlinx.coroutines.launch

class CreateInstrumentDialog(
    private val fragment: Fragment,
    private val instrumentRepository: InstrumentRepository,
    private val onInstrumentCreated: (Instrument) -> Unit,
    private val onError: (String) -> Unit
) {

    private var isCreating = false

    fun show() {
        val binding = DialogCreateInstrumentBinding.inflate(LayoutInflater.from(fragment.requireContext()))
        
        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(binding.root)
            .setCancelable(true)
            .create()

        setupValidation(binding)
        setupClickListeners(binding, dialog)
        
        dialog.show()
    }

    private fun setupValidation(binding: DialogCreateInstrumentBinding) {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm(binding)
            }
        }

        binding.etInstrumentName.addTextChangedListener(textWatcher)
        binding.etInstrumentDescription.addTextChangedListener(textWatcher)
    }

    private fun setupClickListeners(binding: DialogCreateInstrumentBinding, dialog: androidx.appcompat.app.AlertDialog) {
        binding.btnCancel.setOnClickListener {
            if (!isCreating) {
                dialog.dismiss()
            }
        }

        binding.btnCreate.setOnClickListener {
            if (!isCreating && validateForm(binding)) {
                createInstrument(binding, dialog)
            }
        }
    }

    private fun validateForm(binding: DialogCreateInstrumentBinding): Boolean {
        var isValid = true

        // Validate instrument name
        val name = binding.etInstrumentName.text.toString().trim()
        if (name.isEmpty()) {
            binding.tilInstrumentName.error = "Instrument name is required"
            isValid = false
        } else if (name.length < 3) {
            binding.tilInstrumentName.error = "Name must be at least 3 characters"
            isValid = false
        } else {
            binding.tilInstrumentName.error = null
        }

        // Validate description
        val description = binding.etInstrumentDescription.text.toString().trim()
        if (description.isEmpty()) {
            binding.tilInstrumentDescription.error = "Description is required"
            isValid = false
        } else if (description.length < 10) {
            binding.tilInstrumentDescription.error = "Description must be at least 10 characters"
            isValid = false
        } else {
            binding.tilInstrumentDescription.error = null
        }

        // Update create button state
        binding.btnCreate.isEnabled = isValid && !isCreating

        return isValid
    }

    private fun createInstrument(binding: DialogCreateInstrumentBinding, dialog: androidx.appcompat.app.AlertDialog) {
        if (isCreating) return

        val name = binding.etInstrumentName.text.toString().trim()
        val description = binding.etInstrumentDescription.text.toString().trim()

        setCreatingState(binding, dialog, true)

        fragment.lifecycleScope.launch {
            try {
                val result = instrumentRepository.createInstrument(name, description)
                result.onSuccess { instrument ->
                    onInstrumentCreated(instrument)
                    dialog.dismiss()
                }.onFailure { error ->
                    setCreatingState(binding, dialog, false)
                    onError(error.message ?: "Failed to create instrument")
                }
            } catch (e: Exception) {
                setCreatingState(binding, dialog, false)
                onError(e.message ?: "An unexpected error occurred")
            }
        }
    }

    private fun setCreatingState(binding: DialogCreateInstrumentBinding, dialog: androidx.appcompat.app.AlertDialog, creating: Boolean) {
        isCreating = creating
        
        // Update UI state
        binding.btnCreate.isEnabled = !creating && validateForm(binding)
        binding.btnCancel.isEnabled = !creating
        binding.etInstrumentName.isEnabled = !creating
        binding.etInstrumentDescription.isEnabled = !creating
        
        // Update button text and show loading
        if (creating) {
            binding.btnCreate.text = "Creating..."
        } else {
            binding.btnCreate.text = "Create"
        }
        
        // Update dialog cancelable state
        dialog.setCancelable(!creating)
        dialog.setCanceledOnTouchOutside(!creating)
    }
}