package info.proteo.cupcake.ui.session

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.repository.StoredReagentRepository
import info.proteo.cupcake.databinding.ItemReagentBookingBinding
import info.proteo.cupcake.databinding.ItemSessionStepReagentBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class SessionStepReagentAdapter(
    private val onBookReagentClick: (DisplayableStepReagent) -> Unit,
    private val storedReagentRepository: StoredReagentRepository
) : ListAdapter<DisplayableStepReagent, SessionStepReagentAdapter.ViewHolder>(DiffCallback()) {

    private val job = SupervisorJob()
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + job)


    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancel()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSessionStepReagentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onBookReagentClick, storedReagentRepository, coroutineScope)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemSessionStepReagentBinding,
        private val onBookReagentClick: (DisplayableStepReagent) -> Unit,
        private val storedReagentRepository: StoredReagentRepository,
        private val coroutineScope: CoroutineScope
    ) : RecyclerView.ViewHolder(binding.root) {

        private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private val outputDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())


        private fun formatDate(dateString: String?): String {
            if (dateString.isNullOrEmpty()) return "N/A"
            return try {
                val date = inputDateFormat.parse(dateString)
                if (date != null) outputDateFormat.format(date) else "N/A"
            } catch (e: Exception) {
                Log.w("ViewHolderDate", "Could not parse date: $dateString", e)
                dateString
            }
        }

        fun bind(item: DisplayableStepReagent) {
            binding.stepReagentName.text = item.stepReagent.reagent.name
            binding.textViewQuantityValue.text = item.stepReagent.quantity.toString()
            binding.textViewUnitValue.text = item.stepReagent.reagent.unit

            binding.buttonBookReagent.setOnClickListener {
                onBookReagentClick(item)
            }

            binding.stepReagentBookingsContainer.removeAllViews()
            if (item.existingBookings.isNotEmpty()) {
                binding.stepReagentBookingsHeader.text = "(${item.existingBookings.size}) Existing Inventory Reservations"
                binding.stepReagentBookingsHeader.visibility = View.VISIBLE
                binding.buttonExpandBookings.visibility = View.VISIBLE

                var areBookingsVisible = false
                binding.stepReagentBookingsContainer.visibility = View.GONE
                binding.buttonExpandBookings.setImageResource(R.drawable.ic_expand_more)

                binding.buttonExpandBookings.setOnClickListener {
                    areBookingsVisible = !areBookingsVisible
                    binding.stepReagentBookingsContainer.visibility = if (areBookingsVisible) View.VISIBLE else View.GONE
                    binding.buttonExpandBookings.setImageResource(
                        if (areBookingsVisible) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                    )
                }

                item.existingBookings.forEachIndexed { index, bookingAction ->
                    val bookingViewBinding = ItemReagentBookingBinding.inflate(
                        LayoutInflater.from(itemView.context),
                        binding.stepReagentBookingsContainer,
                        false
                    )

                    bookingViewBinding.bookingReagentName.text = ""
                    bookingViewBinding.bookingQuantity.text = ""

                    val storedReagentId = bookingAction.reagent
                    coroutineScope.launch {
                        try {
                            val result = storedReagentRepository.getStoredReagentById(storedReagentId).firstOrNull()
                            if (result != null && result.isSuccess) {
                                val storedReagent = result.getOrNull()
                                if (storedReagent != null) {
                                    Log.d("ViewHolderBind", "Fetched StoredReagent (ID: $storedReagentId) for booking: ${storedReagent.reagent.name}")
                                    bookingViewBinding.bookingReagentName.text = storedReagent.reagent.name
                                    bookingViewBinding.bookingQuantity.text = "${bookingAction.quantity} ${storedReagent.reagent.unit}"
                                } else {
                                    bookingViewBinding.bookingReagentName.text = "Unknown Reagent"
                                    bookingViewBinding.bookingQuantity.text = "${bookingAction.quantity}"
                                }
                            } else {
                                Log.w("ViewHolderBind", "Failed to fetch StoredReagent (ID: $storedReagentId) details for booking. Error: ${result?.exceptionOrNull()?.message}")
                                bookingViewBinding.bookingReagentName.text = "Unknown Reagent"
                                bookingViewBinding.bookingQuantity.text = "${bookingAction.quantity}"
                            }
                        } catch (e: Exception) {
                            Log.e("ViewHolderBind", "Exception fetching StoredReagent (ID: $storedReagentId) for booking", e)
                            bookingViewBinding.bookingReagentName.text = "Unknown Reagent"
                            bookingViewBinding.bookingQuantity.text = "${bookingAction.quantity}"
                        }
                    }

                    bookingViewBinding.bookingDate.text = formatDate(bookingAction.createdAt)
                    if (!bookingAction.notes.isNullOrEmpty()) {
                        bookingViewBinding.bookingNotes.text = bookingAction.notes
                        bookingViewBinding.bookingNotes.visibility = View.VISIBLE
                    } else {
                        bookingViewBinding.bookingNotes.visibility = View.GONE
                    }

                    binding.stepReagentBookingsContainer.addView(bookingViewBinding.root)

                    if (index < item.existingBookings.size - 1) {
                        val divider = View(itemView.context)
                        val layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                        )
                        val margin = (8 * itemView.context.resources.displayMetrics.density).toInt() // 8dp margin
                        layoutParams.setMargins(0, margin, 0, margin)
                        divider.layoutParams = layoutParams
                        divider.setBackgroundColor(itemView.context.getColor(R.color.secondary)) // Define this color in your colors.xml
                        binding.stepReagentBookingsContainer.addView(divider)
                    }
                }
            } else {
                binding.stepReagentBookingsHeader.visibility = View.GONE
                binding.stepReagentBookingsContainer.visibility = View.GONE
                binding.buttonExpandBookings.visibility = View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DisplayableStepReagent>() {
        override fun areItemsTheSame(oldItem: DisplayableStepReagent, newItem: DisplayableStepReagent): Boolean {
            return oldItem.stepReagent.id == newItem.stepReagent.id && oldItem.existingBookings.size == newItem.existingBookings.size
        }

        override fun areContentsTheSame(oldItem: DisplayableStepReagent, newItem: DisplayableStepReagent): Boolean {
            return oldItem == newItem
        }
    }
}