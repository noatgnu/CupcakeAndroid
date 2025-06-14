package info.proteo.cupcake.ui.settings

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import info.proteo.cupcake.R
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.FragmentSettingsBinding

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setupSettingsCategories()

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
            findNavController().navigate(R.id.action_settings_to_login)
        }
    }

    private fun setupSettingsCategories() {
        val categories = listOf(
            SettingsCategoryAdapter.SettingsCategory(
                R.id.action_settings_to_profile_management,
                "User Profiles",
                R.drawable.ic_person
            ),
            SettingsCategoryAdapter.SettingsCategory(
                R.id.action_settings_to_update_metadata,
                "Update Metadata Tables",
                R.drawable.outline_deployed_code_update_24
            ),
            SettingsCategoryAdapter.SettingsCategory(
                -1,
                "Open Source Licenses",
                R.drawable.outline_info_24
            )
        )

        binding.settingsCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = SettingsCategoryAdapter(categories) { category ->
                if (category.id == -1) {
                    openLicensesDialog()
                } else {
                    findNavController().navigate(category.id)
                }
            }
        }
    }

    private fun openLicensesDialog() {
        val webView = WebView(requireActivity())
        webView.loadUrl("file:///android_asset/open_source_licenses.html")

        val dialog = AlertDialog.Builder(requireActivity())
            .setTitle("Open Source Licenses")
            .setView(webView)
            .setPositiveButton("OK"
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .create()
        dialog.show()

    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}