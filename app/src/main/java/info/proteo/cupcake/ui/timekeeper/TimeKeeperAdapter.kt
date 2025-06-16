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
import info.proteo.cupcake.databinding.ItemTimeKeeperBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TimeKeeperAdapter(
    private val onItemClick: (TimeKeeper) -> Unit,
    private val onStartClick: (TimeKeeper) -> Unit,
    private val onPauseClick: (TimeKeeper) -> Unit,
    private val onResetClick: (TimeKeeper) -> Unit,
) : ListAdapter<TimeKeeper, TimeKeeperAdapter.TimeKeeperViewHolder>(DIFF_CALLBACK) {

    private var activeTimers: Map<Int, TimeKeeperViewModel.TimerState> = emptyMap()

    fun updateActiveTimers(timers: Map<Int, TimeKeeperViewModel.TimerState>) {
        activeTimers = timers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeKeeperViewHolder {
        val binding = ItemTimeKeeperBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TimeKeeperViewHolder(binding, onItemClick, onStartClick, onPauseClick, onResetClick)
    }

    override fun onBindViewHolder(holder: TimeKeeperViewHolder, position: Int) {
        val timeKeeper = getItem(position)
        val timerState = activeTimers[timeKeeper.id]
        holder.bind(timeKeeper, timerState)
    }

    class TimeKeeperViewHolder(
        private val binding: ItemTimeKeeperBinding,
        private val onItemClick: (TimeKeeper) -> Unit,
        private val onStartClick: (TimeKeeper) -> Unit,
        private val onPauseClick: (TimeKeeper) -> Unit,
        private val onResetClick: (TimeKeeper) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(timeKeeper: TimeKeeper, timerState: TimeKeeperViewModel.TimerState?) {
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

                textViewSession.text = "Session: ${timeKeeper.session}"
                textViewStep.text = timeKeeper.step?.let { "Step: $it" } ?: "No step"
                textViewStatus.text = if (timeKeeper.started == true) "Status: Active" else "Status: Inactive" // Explicitly check for true if nullable Boolean
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

        private fun formatDuration(seconds: Int): String {
            val totalSeconds = seconds.toInt()
            val hours = totalSeconds / 3600
            val mins = (totalSeconds % 3600) / 60
            val secs = totalSeconds % 60
            return String.format("%02d:%02d:%02d", hours, mins, secs)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TimeKeeper>() {
            override fun areItemsTheSame(oldItem: TimeKeeper, newItem: TimeKeeper): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TimeKeeper, newItem: TimeKeeper): Boolean {
                return oldItem == newItem
            }
        }
    }
}