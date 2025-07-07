package info.proteo.cupcake.ui.timekeeper

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.shared.data.model.protocol.TimeKeeper
import info.proteo.cupcake.shared.data.model.protocol.Session
import info.proteo.cupcake.shared.data.model.protocol.ProtocolStep
import info.proteo.cupcake.databinding.ItemTimeKeeperBinding
import info.proteo.cupcake.util.ProtocolHtmlRenderer.htmlToPlainText
import java.text.SimpleDateFormat
import java.util.Locale

data class TimeKeeperDisplayItem(
    val timeKeeper: TimeKeeper,
    val session: Session?,
    val step: ProtocolStep?,
    val timerState: TimeKeeperViewModel.TimerState?
)

class TimeKeeperAdapter(
    private val onItemClick: (TimeKeeper) -> Unit,
    private val onStartClick: (TimeKeeper) -> Unit,
    private val onPauseClick: (TimeKeeper) -> Unit,
    private val onResetClick: (TimeKeeper) -> Unit,
) : ListAdapter<TimeKeeperDisplayItem, TimeKeeperAdapter.TimeKeeperViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeKeeperViewHolder {
        val binding = ItemTimeKeeperBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TimeKeeperViewHolder(binding, onItemClick, onStartClick, onPauseClick, onResetClick)
    }

    override fun onBindViewHolder(holder: TimeKeeperViewHolder, position: Int) {
        val displayItem = getItem(position)
        holder.bind(displayItem)
    }

    override fun onBindViewHolder(holder: TimeKeeperViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.contains("TIMER_UPDATE")) {
            // Only update timer-related views
            val displayItem = getItem(position)
            holder.updateTimerOnly(displayItem)
        } else {
            // Full bind
            onBindViewHolder(holder, position)
        }
    }

    class TimeKeeperViewHolder(
        private val binding: ItemTimeKeeperBinding,
        private val onItemClick: (TimeKeeper) -> Unit,
        private val onStartClick: (TimeKeeper) -> Unit,
        private val onPauseClick: (TimeKeeper) -> Unit,
        private val onResetClick: (TimeKeeper) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(displayItem: TimeKeeperDisplayItem) {
            val timeKeeper = displayItem.timeKeeper
            val session = displayItem.session
            val step = displayItem.step
            val timerState = displayItem.timerState
            
            binding.apply {
                timeKeeper.startTime?.let {
                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val date = inputFormat.parse(it)
                        textViewStartTime.text = date?.let { it1 -> outputFormat.format(it1) } ?: it
                    } catch (e: Exception) {
                        textViewStartTime.text = it
                    }
                } ?: run {
                    textViewStartTime.text = "Not started"
                }

                // Display session name or unique ID
                textViewSession.text = if (session?.name != null) {
                    "Session: ${session.name}"
                } else {
                    "Session: ${session?.uniqueId ?: timeKeeper.session}"
                }
                
                // Display truncated step description
                textViewStep.text = if (step != null) {
                    val stepDesc = step.htmlToPlainText("No description")
                    val truncated = if (stepDesc.length > 60) {
                        stepDesc.take(60) + "..."
                    } else {
                        stepDesc
                    }
                    "Step: $truncated"
                } else {
                    "Step: ${timeKeeper.step ?: "No step"}"
                }
                
                textViewStatus.text = if (timeKeeper.started == true) "Status: Active" else "Status: Inactive"
                textViewDuration.text = timeKeeper.currentDuration?.let { "Duration: $it min" } ?: "No duration"

                root.setOnClickListener {
                    onItemClick(timeKeeper)
                }

                if (timerState != null) {
                    textViewDuration.text = "Duration: ${formatDuration(timerState.currentDuration)}"
                    buttonStart.visibility = if (timerState.started) View.GONE else View.VISIBLE
                    buttonPause.visibility = if (timerState.started) View.VISIBLE else View.GONE
                    buttonReset.visibility = View.VISIBLE
                    textViewStatus.text = if (timerState.started) "Status: Active" else "Status: Inactive"
                } else {
                    val isActuallyStarted = timeKeeper.started == true
                    textViewDuration.text = timeKeeper.currentDuration?.let {
                        "Duration: ${formatDuration(it)}"
                    } ?: "No duration"

                    buttonStart.visibility = if (isActuallyStarted) View.GONE else View.VISIBLE
                    buttonPause.visibility = if (isActuallyStarted) View.VISIBLE else View.GONE
                    buttonReset.visibility = View.VISIBLE
                    textViewStatus.text = if (timeKeeper.started == true) "Status: Active" else "Status: Inactive"
                }

                if (timerState != null && timerState.started) {
                    root.background = root.context.getDrawable(R.drawable.active_timekeeper_border)
                } else {
                    root.background = null
                }

                buttonStart.setOnClickListener { onStartClick(timeKeeper) }
                buttonPause.setOnClickListener {
                    Log.d("TimeKeeperAdapter", "Pause clicked for TimeKeeper ID: ${timeKeeper.id}")
                    onPauseClick(timeKeeper)
                }
                buttonReset.setOnClickListener { onResetClick(timeKeeper) }
            }
        }

        fun updateTimerOnly(displayItem: TimeKeeperDisplayItem) {
            val timeKeeper = displayItem.timeKeeper
            val timerState = displayItem.timerState
            
            binding.apply {
                // Update only timer-related views
                if (timerState != null) {
                    textViewDuration.text = "Duration: ${formatDuration(timerState.currentDuration)}"
                    buttonStart.visibility = if (timerState.started) View.GONE else View.VISIBLE
                    buttonPause.visibility = if (timerState.started) View.VISIBLE else View.GONE
                    textViewStatus.text = if (timerState.started) "Status: Active" else "Status: Inactive"
                } else {
                    val isActuallyStarted = timeKeeper.started == true
                    textViewDuration.text = timeKeeper.currentDuration?.let {
                        "Duration: ${formatDuration(it)}"
                    } ?: "No duration"
                    
                    buttonStart.visibility = if (isActuallyStarted) View.GONE else View.VISIBLE
                    buttonPause.visibility = if (isActuallyStarted) View.VISIBLE else View.GONE
                    textViewStatus.text = if (timeKeeper.started == true) "Status: Active" else "Status: Inactive"
                }

                // Update background based on timer state
                if (timerState != null && timerState.started) {
                    root.background = root.context.getDrawable(R.drawable.active_timekeeper_border)
                } else {
                    root.background = null
                }
            }
        }

        private fun formatDuration(seconds: Int): String {
            val totalSeconds = seconds.toInt()
            val hours = totalSeconds / 3600
            val mins = (totalSeconds % 3600) / 60
            val secs = totalSeconds % 60
            return String.format("%02d:%02d:%02d", hours, mins, secs)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TimeKeeperDisplayItem>() {
            override fun areItemsTheSame(oldItem: TimeKeeperDisplayItem, newItem: TimeKeeperDisplayItem): Boolean {
                return oldItem.timeKeeper.id == newItem.timeKeeper.id
            }

            override fun areContentsTheSame(oldItem: TimeKeeperDisplayItem, newItem: TimeKeeperDisplayItem): Boolean {
                // Compare all fields except timer state for content equality
                return oldItem.timeKeeper == newItem.timeKeeper &&
                        oldItem.session == newItem.session &&
                        oldItem.step == newItem.step &&
                        oldItem.timerState?.started == newItem.timerState?.started &&
                        oldItem.timerState?.currentDuration == newItem.timerState?.currentDuration
            }

            override fun getChangePayload(oldItem: TimeKeeperDisplayItem, newItem: TimeKeeperDisplayItem): Any? {
                // Only timer state changed - return payload to trigger partial update
                if (oldItem.timeKeeper == newItem.timeKeeper &&
                    oldItem.session == newItem.session &&
                    oldItem.step == newItem.step &&
                    oldItem.timerState != newItem.timerState) {
                    return "TIMER_UPDATE"
                }
                return null
            }
        }
    }
}