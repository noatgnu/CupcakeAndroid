package info.proteo.cupcake.ui.timekeeper

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.databinding.ItemActiveTimeKeeperPreviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.text.format

class ActiveTimeKeeperPreviewAdapter(
    private val onItemClick: (TimeKeeper) -> Unit
) : ListAdapter<TimeKeeper, ActiveTimeKeeperPreviewAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var activeTimers: Map<Int, TimeKeeperViewModel.TimerState> = emptyMap()

    fun updateActiveTimers(timers: Map<Int, TimeKeeperViewModel.TimerState>) {
        activeTimers = timers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActiveTimeKeeperPreviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timeKeeper = getItem(position)
        val timerState = activeTimers[timeKeeper.id]
        holder.bind(timeKeeper, timerState)
    }

    class ViewHolder(
        private val binding: ItemActiveTimeKeeperPreviewBinding,
        private val onItemClick: (TimeKeeper) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(timeKeeper: TimeKeeper, timerState: TimeKeeperViewModel.TimerState?) {
            binding.apply {
                // Handle start time display
                timeKeeper.startTime?.let {
                    try {
                        val date = parseApiDateTime(it)
                        textViewStartTime.text = formatDateForDisplay(date)
                    } catch (e: Exception) {
                        textViewStartTime.text = it
                    }
                } ?: run {
                    textViewStartTime.text = "Not started"
                }

                // Display session and step info
                textViewSession.text = "Session: ${timeKeeper.session}"
                textViewStep.text = if (timeKeeper.step != null) "Step: ${timeKeeper.step}" else "No step"

                // Calculate and display the correct duration
                if (timerState != null) {
                    // Use the timer state from the view model
                    textViewDuration.text = "Duration: ${formatDuration(timerState.currentDuration)}"
                } else if (timeKeeper.started == true && timeKeeper.startTime != null) {
                    // Calculate remaining time based on start time
                    try {
                        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                        formatter.timeZone = TimeZone.getTimeZone("UTC")
                        val startTime = formatter.parse(timeKeeper.startTime)?.time ?: 0L

                        val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                        val initialDuration = timeKeeper.currentDuration ?: 0
                        val remainingDuration = (initialDuration - elapsedSeconds).coerceAtLeast(0)

                        textViewDuration.text = "Duration: ${formatDuration(remainingDuration)}"
                    } catch (e: Exception) {
                        textViewDuration.text = "Duration: ${formatDuration(timeKeeper.currentDuration ?: 0)}"
                    }
                } else {
                    // Not active, show the current duration
                    textViewDuration.text = "Duration: ${formatDuration(timeKeeper.currentDuration ?: 0)}"
                }

                // Set click listener
                root.setOnClickListener {
                    onItemClick(timeKeeper)
                }
            }
        }

        private fun formatDuration(seconds: Int): String {
            val hours = seconds / 3600
            val mins = (seconds % 3600) / 60
            val secs = seconds % 60
            return String.format("%02d:%02d:%02d", hours, mins, secs)
        }

        private fun parseApiDateTime(dateTimeString: String?): Long {
            if (dateTimeString == null) return 0L
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                format.parse(dateTimeString)?.time ?: 0L
            } catch (e: Exception) {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    format.timeZone = TimeZone.getTimeZone("UTC")
                    format.parse(dateTimeString)?.time ?: 0L
                } catch (e: Exception) {
                    Log.e("DateParsing", "Error parsing date: $dateTimeString", e)
                    0L
                }
            }
        }

        private fun formatDateForDisplay(timestamp: Long): String {
            val displayFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            // This will use device's local timezone by default
            return displayFormat.format(Date(timestamp))
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