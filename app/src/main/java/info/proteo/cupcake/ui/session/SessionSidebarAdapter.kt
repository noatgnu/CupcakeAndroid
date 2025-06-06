package info.proteo.cupcake.ui.session

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.protocol.ProtocolSection
import info.proteo.cupcake.data.remote.model.protocol.ProtocolStep

class SessionSidebarAdapter(private val onStepClick: (ProtocolStep) -> Unit) :
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
        }

        refreshItems()
        notifyDataSetChanged()
    }

    private fun refreshItems() {
        val newItems = mutableListOf<SidebarItem>()

        sections.forEach { section ->
            newItems.add(SidebarItem.Section(section))

            if (expandedSections.contains(section.id)) {
                val steps = stepsMap[section.id] ?: emptyList()
                steps.forEach { step ->
                    newItems.add(SidebarItem.Step(step, section.id))
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
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SidebarItem.Section -> VIEW_TYPE_SECTION
            is SidebarItem.Step -> VIEW_TYPE_STEP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SECTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_session_sidebar_section, parent, false)
                SectionViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_session_sidebar_step, parent, false)
                StepViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SidebarItem.Section -> (holder as SectionViewHolder).bind(item.section)
            is SidebarItem.Step -> (holder as StepViewHolder).bind(item.step)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)
        private val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        private val sectionIndicator: View = itemView.findViewById(R.id.sectionIndicator)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position] as SidebarItem.Section
                    toggleSection(item.section.id)
                }
            }
        }

        fun bind(section: ProtocolSection) {
            sectionTitle.text = section.sectionDescription ?: "Section ${section.id}"

            expandIcon.setImageResource(
                if (expandedSections.contains(section.id))
                    R.drawable.ic_expand_less
                else
                    R.drawable.ic_expand_more
            )

            sectionIndicator.visibility = if (selectedSectionId == section.id) View.VISIBLE else View.GONE
        }
    }

    inner class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val stepTitle: TextView = itemView.findViewById(R.id.stepTitle)
        private val stepIndicator: View = itemView.findViewById(R.id.stepIndicator)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position] as SidebarItem.Step
                    onStepClick(item.step)
                    setSelectedStep(item.step.id, item.sectionId)
                }
            }
        }

        fun bind(step: ProtocolStep) {
            val plainText = Html.fromHtml(step.stepDescription, Html.FROM_HTML_MODE_COMPACT)
                .toString()
                .take(30)
            stepTitle.text = "$plainText..."

            stepIndicator.visibility = if (selectedStepId == step.id) View.VISIBLE else View.GONE
        }
    }

    sealed class SidebarItem {
        data class Section(val section: ProtocolSection) : SidebarItem()
        data class Step(val step: ProtocolStep, val sectionId: Int) : SidebarItem()
    }

    companion object {
        private const val VIEW_TYPE_SECTION = 0
        private const val VIEW_TYPE_STEP = 1
    }
}