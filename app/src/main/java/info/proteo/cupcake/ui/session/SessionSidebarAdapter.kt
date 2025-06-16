package info.proteo.cupcake.ui.session

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.shared.data.model.protocol.ProtocolSection
import info.proteo.cupcake.shared.data.model.protocol.ProtocolStep
import info.proteo.cupcake.util.ProtocolHtmlRenderer.htmlToPlainText

class SessionSidebarAdapter(private val onStepClick: (ProtocolStep, ProtocolSection) -> Unit) : // Changed lambda
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var sections: List<ProtocolSection> = emptyList()
    private var stepsMap: Map<Int, List<ProtocolStep>> = emptyMap()
    private var expandedSections = mutableSetOf<Int>()
    private var items: List<SidebarItem> = emptyList()

    private var selectedSectionId: Int? = null
    private var selectedStepId: Int? = null

    fun updateData(sections: List<ProtocolSection>, stepsMap: Map<Int, List<ProtocolStep>>) {
        this.sections = sections
        this.stepsMap = stepsMap

        if (expandedSections.isEmpty() && sections.isNotEmpty()) {
            expandedSections.add(sections[0].id)
        } else {
            val currentSectionIds = sections.map { it.id }.toSet()
            expandedSections.retainAll(currentSectionIds)
        }

        refreshItems()
        notifyDataSetChanged()
    }

    private fun refreshItems() {
        val newItems = mutableListOf<SidebarItem>()
        sections.forEach { section ->
            newItems.add(SidebarItem.SectionItem(section)) // Renamed for clarity
            if (expandedSections.contains(section.id)) {
                val steps = stepsMap[section.id] ?: emptyList()
                steps.forEach { step ->
                    newItems.add(SidebarItem.StepItem(step, section)) // Pass section object
                }
            }
        }
        items = newItems
    }

    fun toggleSection(sectionId: Int) {
        if (expandedSections.contains(sectionId)) {
            expandedSections.remove(sectionId)
        } else {
            expandedSections.add(sectionId)
        }
        refreshItems()
        notifyDataSetChanged()
    }

    fun setSelectedStep(stepId: Int, sectionId: Int) {
        selectedStepId = stepId
        selectedSectionId = sectionId
        var needsRefresh = false
        if (!expandedSections.contains(sectionId)) {
            expandedSections.add(sectionId)
            needsRefresh = true
        }

        if (needsRefresh) {
            refreshItems()
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SidebarItem.SectionItem -> VIEW_TYPE_SECTION
            is SidebarItem.StepItem -> VIEW_TYPE_STEP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SECTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_session_sidebar_section, parent, false)
                SectionViewHolder(view)
            }
            else -> { // VIEW_TYPE_STEP
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_session_sidebar_step, parent, false)
                StepViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SidebarItem.SectionItem -> (holder as SectionViewHolder).bind(item.section)
            is SidebarItem.StepItem -> (holder as StepViewHolder).bind(item.step)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)
        private val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        private val sectionIndicator: View = itemView.findViewById(R.id.sectionIndicator)

        init {
            itemView.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                    (items[pos] as? SidebarItem.SectionItem)?.let {
                        toggleSection(it.section.id)
                    }
                }
            }
        }

        fun bind(section: ProtocolSection) {
            sectionTitle.text = section.sectionDescription ?: "Section ${section.id}"
            expandIcon.setImageResource(
                if (expandedSections.contains(section.id)) R.drawable.ic_expand_less
                else R.drawable.ic_expand_more
            )
            sectionIndicator.visibility = if (selectedSectionId == section.id && expandedSections.contains(section.id)) View.VISIBLE else View.INVISIBLE
        }
    }

    inner class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val stepTitle: TextView = itemView.findViewById(R.id.stepTitle)
        private val stepIndicator: View = itemView.findViewById(R.id.stepIndicator)

        init {
            itemView.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                    (items[pos] as? SidebarItem.StepItem)?.let { item ->
                        onStepClick(item.step, item.section) // Pass section here
                        setSelectedStep(item.step.id, item.section.id)
                    }
                }
            }
        }

        fun bind(step: ProtocolStep) {
            val plainTextDescription = step.htmlToPlainText()
            stepTitle.text = "${plainTextDescription.take(50)}${if (plainTextDescription.length > 50) "..." else ""}"
            stepIndicator.visibility = if (selectedStepId == step.id) View.VISIBLE else View.INVISIBLE
        }
    }

    sealed class SidebarItem {
        data class SectionItem(val section: ProtocolSection) : SidebarItem() // Renamed
        data class StepItem(val step: ProtocolStep, val section: ProtocolSection) : SidebarItem() // Changed to include section
    }

    companion object {
        private const val VIEW_TYPE_SECTION = 0
        private const val VIEW_TYPE_STEP = 1
    }
}