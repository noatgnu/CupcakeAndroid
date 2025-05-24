package info.proteo.cupcake.ui.message

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.FragmentThreadDetailBinding
import jp.wasabeef.richeditor.RichEditor
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ThreadDetailFragment : Fragment() {
    private var _binding: FragmentThreadDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ThreadDetailViewModel by viewModels()

    // Workaround for SafeArgs issue
    private val threadId: Int by lazy {
        arguments?.getInt("threadId") ?: -1
    }

    // Rich text editor
    private lateinit var richTextEditor: RichEditor

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThreadDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRichTextEditor()
        setupMessageAdapter()
        setupObservers()
        setupClickListeners()
        setupStaffOptions()

        viewModel.loadThreadDetails(threadId)
    }

    private fun setupRichTextEditor() {
        // Create a new RichEditor instance
        richTextEditor = RichEditor(requireContext())
        richTextEditor.setEditorHeight(40)
        richTextEditor.setEditorFontSize(16)
        richTextEditor.setPadding(10, 10, 10, 10)
        richTextEditor.setPlaceholder("Type your message here...")

        // Set theme-aware colors
        val isNightMode = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES

        if (isNightMode) {
            // Dark theme colors
            richTextEditor.setBackgroundColor(ContextCompat.getColor(requireContext(),
                R.color.editor_background_dark))
            richTextEditor.setEditorBackgroundColor(ContextCompat.getColor(requireContext(),
                R.color.editor_background_dark))
            // Force white text for dark theme
            richTextEditor.setEditorFontColor(Color.WHITE)
        } else {
            // Light theme colors
            richTextEditor.setBackgroundColor(ContextCompat.getColor(requireContext(),
                R.color.editor_background_light))
            richTextEditor.setEditorBackgroundColor(ContextCompat.getColor(requireContext(),
                R.color.editor_background_light))
            // Force black text for light theme
            richTextEditor.setEditorFontColor(Color.BLACK)
        }

        binding.editorContainer.addView(richTextEditor)
        binding.buttonBold.setOnClickListener { richTextEditor.setBold() }
        binding.buttonItalic.setOnClickListener { richTextEditor.setItalic() }
        binding.buttonUnderline.setOnClickListener { richTextEditor.setUnderline() }
    }

    private fun setupMessageAdapter() {
        // Make sure MessageAdapter can handle ThreadMessage objects
        val messageAdapter = MessageAdapter()
        binding.recyclerViewMessages.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.threadDetails.collect { thread ->
                    thread?.let {
                        binding.textViewThreadTitle.text = it.title
                        (binding.recyclerViewMessages.adapter as MessageAdapter).submitList(it.messages)
                        binding.recyclerViewMessages.scrollToPosition((it.messages.size - 1).coerceAtLeast(0))

                        // Hide reply options for system threads
                        if (it.isSystemThread) {
                            binding.editorContainer.visibility = View.GONE
                            binding.formattingToolbar.visibility = View.GONE
                            binding.buttonSend.visibility = View.GONE
                        } else {
                            binding.editorContainer.visibility = View.VISIBLE
                            binding.formattingToolbar.visibility = View.VISIBLE
                            binding.buttonSend.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonSend.setOnClickListener {
            val htmlContent = richTextEditor.html
            if (!htmlContent.isNullOrBlank()) {
                val cleanedHtml = cleanHtmlContent(htmlContent)
                if (cleanedHtml.isNotBlank()) {
                    viewModel.sendMessage(cleanedHtml)
                }

                richTextEditor.html = ""
                richTextEditor.postDelayed({
                    val isNightMode = resources.configuration.uiMode and
                            Configuration.UI_MODE_NIGHT_MASK ==
                            Configuration.UI_MODE_NIGHT_YES
                    if (isNightMode) {
                        richTextEditor.html = "<style>body{color: #FFFFFF;}</style>"
                    } else {
                        richTextEditor.html = "<style>body{color: #000000;}</style>"
                    }
                }, 100)
            }
        }
    }

    private fun cleanHtmlContent(html: String): String {
        // More robust style tag removal (works anywhere in the content)
        var cleaned = html.replace("<style>[^<]*</style>", "")

        // Edge case handling for incomplete style tags
        cleaned = cleaned.replace("<style>.*$", "")
        cleaned = cleaned.replace(".*body\\{color:[^}]*\\}.*", "")
        cleaned = cleaned.replace("body\\{color:[^}]*\\}", "")

        // Remove other styling
        cleaned = cleaned.replace("style=\"color:[^\"]*\"", "")
            .replace(" color=\"[^\"]*\"", "")
            .replace("<font color=\"[^\"]*\">(.*?)</font>", "$1")

        // Make sure we didn't remove all content
        return if (cleaned.trim().isEmpty()) "" else cleaned.trim()
    }

    private fun setupStaffOptions() {
        // Observe staff status
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isStaffUser.collect { isStaff ->
                binding.staffMessageOptions.visibility = if (isStaff) View.VISIBLE else View.GONE
            }
        }


        val messageTypeLabels = arrayOf("User Message", "System Notification", "Alert", "Announcement")
        val messageTypeValues = arrayOf("user_message", "system_notification", "alert", "announcement")

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}