package info.proteo.cupcake.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.user.User
import info.proteo.cupcake.databinding.FragmentLoginBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            val username = binding.usernameField.text.toString()
            val password = binding.passwordField.text.toString()
            val hostname = binding.hostnameField.text.toString()

            if (validateInputs(username, password, hostname)) {
                login(username, password, hostname)
            }
        }

        // Check if user is already logged in
        lifecycleScope.launch {
            viewModel.checkExistingLogin()
        }

        // Observe login state
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginViewModel.LoginState.Loading -> {
                    showLoading(true)
                    binding.statusText.text = "Logging in..."
                }
                is LoginViewModel.LoginState.VerifyingToken -> {
                    binding.statusText.text = "Verifying authentication..."
                }
                is LoginViewModel.LoginState.Success -> handleLoginSuccess(state.user)
                is LoginViewModel.LoginState.Error -> showError(state.message)
                is LoginViewModel.LoginState.ExistingLoginFound -> navigateToMainScreen()
                else -> showLoading(false)
            }
        }
    }

    private fun handleLoginSuccess(user: User) {
        showLoading(false)

        // Show success with user name
        val message = "Welcome, ${user.firstName} ${user.lastName}"
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

        // Navigate after a short delay to show the welcome message
        lifecycleScope.launch {
            delay(800)
            navigateToMainScreen()
        }
    }

    private fun validateInputs(username: String, password: String, hostname: String): Boolean {
        var isValid = true

        if (username.isBlank()) {
            binding.usernameLayout.error = getString(R.string.username_required)
            isValid = false
        } else {
            binding.usernameLayout.error = null
        }

        if (password.isBlank()) {
            binding.passwordLayout.error = getString(R.string.password_required)
            isValid = false
        } else {
            binding.passwordLayout.error = null
        }

        if (hostname.isBlank()) {
            binding.hostnameLayout.error = getString(R.string.hostname_required)
            isValid = false
        } else {
            binding.hostnameLayout.error = null
        }

        return isValid
    }

    private fun login(username: String, password: String, hostname: String) {
        viewModel.login(username, password, hostname)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        showLoading(false)
        binding.statusText.text = ""
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }


    private fun navigateToMainScreen() {
        try {
            if (findNavController().currentDestination?.id == R.id.loginFragment) {
                findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
            } else {
                Log.w("LoginFragment", "Navigation attempted when not at loginFragment. Current destination: ${findNavController().currentDestination?.label}")
            }
        } catch (e: Exception) {
            Log.e("LoginFragment", "Navigation error", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}