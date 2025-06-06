package info.proteo.cupcake.ui.protocol

import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.data.remote.model.protocol.ProtocolSection
import info.proteo.cupcake.data.remote.model.protocol.ProtocolStep
import info.proteo.cupcake.databinding.ItemProtocolSectionBinding
import info.proteo.cupcake.databinding.ItemProtocolStepBinding

class ProtocolSectionAdapter(
    private val onStepClick: (ProtocolStep) -> Unit
) : RecyclerView.Adapter<ProtocolSectionAdapter.SectionViewHolder>() {

    private var sections: List<ProtocolSection> = emptyList()
    private var stepsMap: Map<Int, List<ProtocolStep>> = emptyMap()
    private val expandedSections = mutableSetOf<Int>()

    fun updateData(sections: List<ProtocolSection>, stepsMap: Map<Int, List<ProtocolStep>>) {
        this.sections = sections
        this.stepsMap = stepsMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemProtocolSectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return SectionViewHolder(binding)
    }

    override fun getItemCount() = sections.size

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]
        holder.bind(section, stepsMap[section.id] ?: emptyList(),
            expandedSections.contains(section.id))
    }

    inner class SectionViewHolder(private val binding: ItemProtocolSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val stepsAdapter = ProtocolStepsAdapter(onStepClick)

        init {
            binding.stepsRecyclerView.apply {
                layoutManager = LinearLayoutManager(binding.root.context)
                adapter = stepsAdapter
            }

            binding.sectionHeader.setOnClickListener {
                val section = sections[bindingAdapterPosition]
                toggleExpansion(section.id)
            }
        }

        fun bind(section: ProtocolSection, steps: List<ProtocolStep>, isExpanded: Boolean) {
            binding.sectionTitle.text = section.sectionDescription
            binding.sectionDuration.text = "Duration: ${formatDuration(section.sectionDuration)}"

            binding.expandIcon.rotation = if (isExpanded) 180f else 0f
            binding.stepsRecyclerView.visibility = if (isExpanded) View.VISIBLE else View.GONE

            stepsAdapter.updateSteps(steps)
        }

        private fun formatDuration(duration: Long?): String {
            if (duration == null) return "N/A"
            val minutes = duration / 60
            val seconds = duration % 60
            return "${minutes}m ${seconds}s"
        }
    }

    private fun toggleExpansion(sectionId: Int) {
        if (expandedSections.contains(sectionId)) {
            expandedSections.remove(sectionId)
        } else {
            expandedSections.add(sectionId)
        }
        notifyDataSetChanged()
    }
}

class ProtocolStepsAdapter(
    private val onStepClick: (ProtocolStep) -> Unit
) : RecyclerView.Adapter<ProtocolStepsAdapter.StepViewHolder>() {

    private var steps: List<ProtocolStep> = emptyList()

    fun updateSteps(steps: List<ProtocolStep>) {
        this.steps = steps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val binding = ItemProtocolStepBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return StepViewHolder(binding)
    }

    override fun getItemCount() = steps.size

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        holder.bind(steps[position])
    }

    inner class StepViewHolder(private val binding: ItemProtocolStepBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onStepClick(steps[bindingAdapterPosition])
                }
            }
        }

        fun bind(step: ProtocolStep) {
            binding.stepNumber.text = "Step ${bindingAdapterPosition + 1}"


            // Set up WebView for HTML content
            if (step.stepDescription != null) {
                binding.stepDescription.visibility = View.GONE
                binding.stepWebView.visibility = View.VISIBLE

                // Configure WebView
                binding.stepWebView.settings.apply {
                    javaScriptEnabled = false
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                    loadWithOverviewMode = true
                }

                val nightModeFlags = binding.root.context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
                val isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

                binding.stepWebView.setBackgroundColor(
                    if (isDarkMode) Color.BLACK else Color.WHITE
                )

                val cssStyle = if (isDarkMode) {
                    "body{color:#FFFFFF;background-color:#121212;font-family:sans-serif;font-size:14px;} " +
                        "table{width:100%;border-collapse:collapse;} " +
                        "th,td{border:1px solid #444;padding:8px;}"
                } else {
                    "body{color:#000000;background-color:#FFFFFF;font-family:sans-serif;font-size:14px;} " +
                        "table{width:100%;border-collapse:collapse;} " +
                        "th,td{border:1px solid #ddd;padding:8px;}"
                }

                binding.stepWebView.loadDataWithBaseURL(
                    null,
                    "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<style>$cssStyle</style>" +
                        "</head><body>${step.stepDescription}</body></html>",
                    "text/html",
                    "UTF-8",
                    null
                )
            } else {
                binding.stepDescription.visibility = View.VISIBLE
                binding.stepWebView.visibility = View.GONE
                binding.stepDescription.text = ""
            }

            binding.stepDuration.text = if (step.stepDuration != null) {
                "Duration: ${formatDuration(step.stepDuration.toLong())}s"
            } else {
                ""
            }
        }
    }

    private fun formatDuration(durationSeconds: Long): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60

        return if (minutes > 0) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
    }
}