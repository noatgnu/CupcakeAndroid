package info.proteo.cupcake.ui.settings

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import info.proteo.cupcake.R
import androidx.navigation.fragment.findNavController
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
            )
        )

        binding.settingsCategories.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = SettingsCategoryAdapter(categories) { category ->
                findNavController().navigate(category.id)
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}