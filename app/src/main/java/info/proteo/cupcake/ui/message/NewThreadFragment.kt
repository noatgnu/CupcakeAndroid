package info.proteo.cupcake.ui.message

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.user.UserBasic
import info.proteo.cupcake.databinding.FragmentNewThreadBinding
import jp.wasabeef.richeditor.RichEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

@AndroidEntryPoint
class NewThreadFragment : Fragment() {

    private var _binding: FragmentNewThreadBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewThreadViewModel by viewModels()




    private lateinit var richTextEditor: RichEditor


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewThreadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUserSelection()
        setupRichTextEditor()
        setupStaffOptions()
        setupObservers()

        binding.buttonSend.setOnClickListener {
            val title = binding.editTextSubject.text.toString().trim()
            val content = richTextEditor.html


            if (title.isEmpty()) {
                binding.editTextSubject.error = "Subject is required"
                return@setOnClickListener
            }

            if (content.isNullOrBlank()) {
                Snackbar.make(binding.root, "Message cannot be empty", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.createThreadAndSendMessage(title, content)
        }
    }

    private fun setupStaffOptions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isStaffUser.collect { isStaff ->
                    binding.staffOptionsContainer.visibility = if (isStaff) View.VISIBLE else View.GONE
                }
            }
        }

        val messageTypeLabels = arrayOf("User Message", "Alert", "Announcement")
        val messageTypeValues = arrayOf("user_message", "alert", "announcement")

        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, messageTypeLabels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerMessageType.adapter = it
        }

        binding.spinnerMessageType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                viewModel.setMessageType(messageTypeValues[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val priorityLabels = arrayOf("Low", "Normal", "High", "Urgent")
        val priorityValues = arrayOf("low", "normal", "high", "urgent")

        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorityLabels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPriority.adapter = it
            binding.spinnerPriority.setSelection(1)
        }

        binding.spinnerPriority.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                viewModel.setMessagePriority(priorityValues[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupRichTextEditor() {
        richTextEditor = RichEditor(requireContext())
        richTextEditor.setEditorHeight(50)
        richTextEditor.setEditorFontSize(16)
        richTextEditor.setPadding(10, 10, 10, 10)
        richTextEditor.setPlaceholder("Type your message here...")

        val isNightMode = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES

        if (isNightMode) {
            richTextEditor.setBackgroundColor(ContextCompat.getColor(requireContext(),
                R.color.editor_background_dark))
            richTextEditor.setEditorBackgroundColor(ContextCompat.getColor(requireContext(),
                R.color.editor_background_dark))
            richTextEditor.setEditorFontColor(Color.WHITE)
        } else {
            richTextEditor.setBackgroundColor(ContextCompat.getColor(requireContext(),
                R.color.editor_background_light))
            richTextEditor.setEditorBackgroundColor(ContextCompat.getColor(requireContext(),
                R.color.editor_background_light))
            richTextEditor.setEditorFontColor(Color.BLACK)
        }

        binding.editorContainer.addView(richTextEditor)

        binding.buttonBold.setOnClickListener { richTextEditor.setBold() }
        binding.buttonItalic.setOnClickListener { richTextEditor.setItalic() }
        binding.buttonUnderline.setOnClickListener { richTextEditor.setUnderline() }
    }

    private fun findScrollViewParent(view: View): ScrollView? {
        var parent = view.parent
        while (parent != null) {
            if (parent is ScrollView) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }

    private fun setupUserSelection() {
        val userAdapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        binding.autocompleteUsers.setAdapter(userAdapter)

        binding.autocompleteUsers.doAfterTextChanged { text ->
            text?.toString()?.let {
                viewModel.updateSearchQuery(it)
            }
        }

        binding.autocompleteUsers.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = viewModel.searchResults.value[position]
            viewModel.addUser(selectedUser)
            binding.autocompleteUsers.text.clear()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.searchResults.collect { users ->
                        val userNames = users.map { user ->
                            if (user.firstName.isNullOrBlank() && user.lastName.isNullOrBlank()) {
                                user.username
                            } else {
                                "${user.firstName} ${user.lastName} (${user.username})"
                            }
                        }
                        userAdapter.clear()
                        userAdapter.addAll(userNames)
                        userAdapter.notifyDataSetChanged()
                    }
                }

                launch {
                    viewModel.selectedUsers.collect { users ->
                        updateSelectedUserChips(users)
                    }
                }
            }
        }

        binding.layoutRecipients.visibility = View.GONE
    }

    private fun updateSelectedUserChips(users: List<UserBasic>) {
        binding.chipGroupRecipients.removeAllViews()

        users.forEach { user ->
            val chip = Chip(requireContext()).apply {
                text = user.username
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    viewModel.removeUser(user)
                }
            }
            binding.chipGroupRecipients.addView(chip)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.isVisible = isLoading
                        binding.buttonSend.isEnabled = !isLoading
                    }
                }

                launch {
                    viewModel.errorMessage.collect { errorMessage ->
                        errorMessage?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }

                launch {
                    viewModel.threadCreated.collect { success ->
                        if (success) {
                            Snackbar.make(binding.root, "Message sent successfully", Snackbar.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.shouldNavigateBack.collect { shouldNavigate ->
                    if (shouldNavigate) {
                        findNavController().navigateUp()
                        viewModel.onNavigatedBack()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}