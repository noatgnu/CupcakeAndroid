package info.proteo.cupcake.ui.instrument

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.SessionManager
import info.proteo.cupcake.data.remote.model.annotation.AnnotationFolder
import info.proteo.cupcake.databinding.FragmentInstrumentDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class InstrumentDetailFragment : Fragment() {

    private var _binding: FragmentInstrumentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InstrumentDetailViewModel by viewModels()
    private val args: InstrumentDetailFragmentArgs by navArgs()

    private lateinit var annotationAdapter: InstrumentAnnotationAdapter
    private var currentSelectedFolderId: Int? = null
    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstrumentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        viewModel.loadInstrumentDetails(args.instrumentId)

        binding.tabLayoutAnnotationFolders.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val folder = tab?.tag as? AnnotationFolder
                folder?.let {
                    currentSelectedFolderId = it.id
                    Log.d("InstrumentDetail", "Tab selected: ${it.folderName}, ID: ${it.id}")
                    viewModel.loadAnnotationsForFolder(it.id)
                    binding.textViewEmptyAnnotations.visibility = View.GONE
                    binding.recyclerViewAnnotations.visibility = View.GONE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                val folder = tab?.tag as? AnnotationFolder
                folder?.let {
                    currentSelectedFolderId = it.id
                    Log.d("InstrumentDetail", "Tab reselected: ${it.folderName}, ID: ${it.id}")
                    viewModel.loadAnnotationsForFolder(it.id)
                }
            }
        })
    }

    private fun setupRecyclerView() {
        annotationAdapter = InstrumentAnnotationAdapter { annotation ->
            // Existing click listener functionality (e.g., Toast) can be kept or removed
            // Toast.makeText(context, "${annotation.annotationName}", Toast.LENGTH_SHORT).show()
            downloadAnnotation(annotation.id)
        }


        binding.recyclerViewAnnotations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = annotationAdapter
        }
    }



    private fun observeViewModel() {
        viewModel.isLoadingInstrument.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                binding.layoutContent.visibility = View.VISIBLE
            } else {
                binding.layoutContent.visibility = View.GONE
            }
        }

        viewModel.instrument.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { instrument ->
                    binding.instrumentDetailName.text = instrument.instrumentName ?: "N/A"
                    binding.instrumentDetailDescription.text = instrument.instrumentDescription ?: "No description available."
                    binding.instrumentDetailCreatedAt.text = formatDate(instrument.createdAt)
                    binding.instrumentDetailUpdatedAt.text = formatDate(instrument.updatedAt)
                    binding.instrumentDetailMaxPreapprovalDays.text = instrument.maxDaysAheadPreApproval?.toString() ?: "N/A"
                    binding.instrumentDetailMaxUsagePreapprovalWindow.text = instrument.maxDaysWithinUsagePreApproval?.toString() ?: "N/A"

                    if (!instrument.image.isNullOrEmpty()) {
                        binding.instrumentDetailImage.load(instrument.image) {
                            placeholder(R.drawable.ic_placeholder_image)
                            error(R.drawable.ic_placeholder_image)
                        }
                        binding.instrumentDetailImage.visibility = View.VISIBLE
                    } else {
                        binding.instrumentDetailImage.visibility = View.GONE
                    }

                    setupAnnotationFolderTabs(instrument.annotationFolders)
                },
                onFailure = { error ->
                    Log.e("InstrumentDetail", "Error loading instrument details", error)
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    binding.instrumentDetailName.text = "Error loading data"
                    binding.instrumentDetailDescription.text = error.message
                }
            )
        }

        viewModel.isLoadingFolderAnnotations.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarAnnotations.isVisible = isLoading
            if (isLoading) {
                binding.recyclerViewAnnotations.isVisible = false
                binding.textViewEmptyAnnotations.isVisible = false
            }
        }

        viewModel.folderAnnotations.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { response ->
                    val annotations = response.results
                    if (annotations.isNullOrEmpty()) {
                        binding.textViewEmptyAnnotations.isVisible = true
                        binding.recyclerViewAnnotations.isVisible = false
                        annotationAdapter.submitList(emptyList())
                    } else {
                        binding.textViewEmptyAnnotations.isVisible = false
                        binding.recyclerViewAnnotations.isVisible = true
                        annotationAdapter.submitList(annotations)
                    }
                    Log.d("InstrumentDetail", "Annotations loaded: ${annotations?.size ?: 0}")
                },
                onFailure = { error ->
                    Log.e("InstrumentDetail", "Error loading annotations for folder", error)
                    Toast.makeText(context, "Error loading annotations: ${error.message}", Toast.LENGTH_SHORT).show()
                    binding.textViewEmptyAnnotations.text = "Error loading annotations."
                    binding.textViewEmptyAnnotations.isVisible = true
                    binding.recyclerViewAnnotations.isVisible = false
                    annotationAdapter.submitList(emptyList())
                }
            )
        }
    }

    private fun setupAnnotationFolderTabs(folders: List<AnnotationFolder>?) {
        binding.tabLayoutAnnotationFolders.removeAllTabs()
        if (folders.isNullOrEmpty()) {
            binding.tabLayoutAnnotationFolders.visibility = View.GONE
            binding.textViewEmptyAnnotations.text = "No annotation folders available."
            binding.textViewEmptyAnnotations.visibility = View.VISIBLE
            binding.recyclerViewAnnotations.visibility = View.GONE
            binding.progressBarAnnotations.visibility = View.GONE
            Log.d("InstrumentDetail", "No annotation folders to display.")
            return
        }

        binding.tabLayoutAnnotationFolders.visibility = View.VISIBLE
        folders.forEachIndexed { index, folder ->
            val tab = binding.tabLayoutAnnotationFolders.newTab()
            tab.text = folder.folderName
            tab.tag = folder
            binding.tabLayoutAnnotationFolders.addTab(tab)
            Log.d("InstrumentDetail", "Added tab: ${folder.folderName}")
        }

        if (binding.tabLayoutAnnotationFolders.tabCount > 0 && currentSelectedFolderId == null) {
            binding.tabLayoutAnnotationFolders.getTabAt(0)?.select()
        } else if (currentSelectedFolderId != null) {
            folders.indexOfFirst { it.id == currentSelectedFolderId }.takeIf { it != -1 }?.let {
                binding.tabLayoutAnnotationFolders.getTabAt(it)?.select()
            } ?: binding.tabLayoutAnnotationFolders.getTabAt(0)?.select()
        }
    }


    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "N/A"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault()
            date?.let { outputFormat.format(it) } ?: "N/A"
        } catch (e: Exception) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(dateString)
                val outputFormat = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
                outputFormat.timeZone = TimeZone.getDefault()
                date?.let { outputFormat.format(it) } ?: "N/A"
            } catch (e2: Exception) {
                Log.e("InstrumentDetail", "Error formatting date: $dateString", e2)
                dateString
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewAnnotations.adapter = null
        _binding = null
    }

    private fun downloadAnnotation(annotationId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getAnnotationDownloadToken(annotationId).collect { result ->
                result.onSuccess { response ->
                    Log.d("InstrumentDetail", "Download token received: ${response.signedToken}")
                    val baseUrl = sessionManager.getBaseUrl()
                    val downloadUrl = "${baseUrl}api/annotation/download_signed?token=${response.signedToken}"

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = downloadUrl.toUri()
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Snackbar.make(binding.root, "No app available to open this document",
                            Snackbar.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    Log.e("InstrumentDetail", "Error downloading annotation", error)
                    Snackbar.make(binding.root, "Error downloading annotation: ${error.message}",
                        Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}