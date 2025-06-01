package info.proteo.cupcake.ui.timekeeper

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.databinding.ItemActiveTimeKeeperPreviewBinding

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
                textViewSession.text = "Session: ${timeKeeper.session ?: "N/A"}"
                textViewStep.text = timeKeeper.step?.let { "Step: $it" } ?: "No step"

                textViewDuration.text = if (timerState != null) {
                    val totalSeconds = timerState.currentDuration.toInt()
                    val hours = totalSeconds / 3600
                    val mins = (totalSeconds % 3600) / 60
                    val secs = totalSeconds % 60
                    "Duration: ${String.format("%02d:%02d:%02d", hours, mins, secs)}"
                } else {
                    timeKeeper.currentDuration?.let {
                        val totalSeconds = it.toInt()
                        val hours = totalSeconds / 3600
                        val mins = (totalSeconds % 3600) / 60
                        val secs = totalSeconds % 60
                        "Duration: ${String.format("%02d:%02d:%02d", hours, mins, secs)}"
                    } ?: "No duration"
                }

                root.setOnClickListener { onItemClick(timeKeeper) }
            }
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