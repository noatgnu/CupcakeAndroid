package info.proteo.cupcake.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.FragmentProfileManagementBinding

@AndroidEntryPoint
class ProfileManagementFragment : Fragment() {

    private var _binding: FragmentProfileManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileManagementViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProfilesList()
    }

    private fun setupProfilesList() {
        val adapter = UserProfileAdapter { userId, hostname ->
            viewModel.setActiveProfile(userId, hostname)
        }

        binding.profilesList.adapter = adapter
        binding.profilesList.layoutManager = LinearLayoutManager(requireContext())

        viewModel.userProfiles.observe(viewLifecycleOwner) { profiles ->
            adapter.submitList(profiles)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}