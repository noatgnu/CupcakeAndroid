package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.local.entity.user.UserPreferencesEntity
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.shared.data.model.instrument.Instrument
import info.proteo.cupcake.shared.data.model.instrument.InstrumentUsage
import info.proteo.cupcake.data.repository.InstrumentRepository
import info.proteo.cupcake.data.repository.InstrumentUsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.text.format

class InstrumentAnnotationHandler(
    private val context: Context,
    private val instrumentRepository: InstrumentRepository,
    private val instrumentUsageRepository: InstrumentUsageRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val displayDateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    fun displayInstrumentBooking(
        annotation: Annotation,
        container: ViewGroup,
        userPreferencesEntity: UserPreferencesEntity
    ) {
        container.removeAllViews()

        val bookingView = LayoutInflater.from(context).inflate(
            R.layout.instrument_booking_layout,
            container,
            false
        )

        val progressBar = bookingView.findViewById<ProgressBar>(R.id.instrumentLoadingProgress)
        val recyclerView = bookingView.findViewById<RecyclerView>(R.id.instrumentUsageRecyclerView)
        val noBookingsText = bookingView.findViewById<TextView>(R.id.noBookingsText)

        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        noBookingsText.visibility = View.GONE

        container.addView(bookingView)
        try {
            val usageIds = annotation.instrumentUsage?.map { it.id } ?: emptyList()

            if (usageIds.isEmpty()) {
                progressBar.visibility = View.GONE
                noBookingsText.visibility = View.VISIBLE
                return
            }

            val bookingAdapter = InstrumentBookingAdapter(userPreferencesEntity.allowOverlapBookings)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = bookingAdapter
            loadInstrumentUsages(usageIds, bookingAdapter, progressBar, recyclerView, noBookingsText)

        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            noBookingsText.visibility = View.VISIBLE
            noBookingsText.text = "Error loading instrument bookings: ${e.message}"
        }
    }

    private fun loadInstrumentUsages(
        usageIds: List<Int>,
        adapter: InstrumentBookingAdapter,
        progressBar: ProgressBar,
        recyclerView: RecyclerView,
        noBookingsText: TextView
    ) {
        val bookingItems = mutableListOf<BookingItem>()
        var loadedCount = 0

        CoroutineScope(Dispatchers.IO).launch {
            for (id in usageIds) {
                instrumentUsageRepository.getInstrumentUsageById(id).collect { result ->
                    if (result.isSuccess) {
                        Log.d("InstrumentAnnotationHandler", "Loaded usage for ID: $id")
                        val usage = result.getOrNull()
                        if (usage != null) {
                            usage.instrument?.let { instrumentId ->
                                Log.d("InstrumentAnnotationHandler", "Loading instrument for ID: $instrumentId")
                                instrumentRepository.getInstrument(instrumentId).collect { instrumentResult ->
                                    if (instrumentResult.isSuccess) {
                                        val instrument = instrumentResult.getOrNull()
                                        if (instrument != null) {
                                            val windowStart = calculateWindowStart(usage.timeStarted)
                                            val windowEnd = calculateWindowEnd(usage.timeEnded)
                                            bookingItems.add(BookingItem(usage, instrument, windowStart, windowEnd))
                                        }
                                    }
                                    loadedCount++
                                    checkIfComplete(loadedCount, usageIds.size, bookingItems, adapter, progressBar, recyclerView, noBookingsText)
                                }
                            } ?: run {
                                loadedCount++
                                checkIfComplete(loadedCount, usageIds.size, bookingItems, adapter, progressBar, recyclerView, noBookingsText)
                            }
                        } else {
                            loadedCount++
                            checkIfComplete(loadedCount, usageIds.size, bookingItems, adapter, progressBar, recyclerView, noBookingsText)
                        }
                    } else {
                        loadedCount++
                        checkIfComplete(loadedCount, usageIds.size, bookingItems, adapter, progressBar, recyclerView, noBookingsText)
                    }
                }
            }
        }
    }

    private suspend fun checkIfComplete(
        loaded: Int,
        total: Int,
        items: List<BookingItem>,
        adapter: InstrumentBookingAdapter,
        progressBar: ProgressBar,
        recyclerView: RecyclerView,
        noBookingsText: TextView
    ) {
        if (loaded >= total) {
            withContext(Dispatchers.Main) {
                if (items.isEmpty()) {
                    progressBar.visibility = View.GONE
                    noBookingsText.visibility = View.VISIBLE
                } else {
                    adapter.submitList(items)
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun calculateWindowStart(timeStarted: String?): Date? {
        if (timeStarted == null) return null

        try {
            val date = dateFormat.parse(timeStarted) ?: return null
            val calendar = java.util.Calendar.getInstance()
            calendar.time = date
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.add(java.util.Calendar.DAY_OF_MONTH, -1) // One day before
            return calendar.time
        } catch (e: Exception) {
            return null
        }
    }

    private fun calculateWindowEnd(timeEnded: String?): Date? {
        if (timeEnded == null) return null

        try {
            val date = dateFormat.parse(timeEnded) ?: return null
            val calendar = java.util.Calendar.getInstance()
            calendar.time = date
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 2) // Two days after
            return calendar.time
        } catch (e: Exception) {
            return null
        }
    }

    data class BookingItem(
        val usage: InstrumentUsage,
        val instrument: Instrument,
        val windowStart: Date?,
        val windowEnd: Date?
    )

    inner class InstrumentBookingAdapter(private val allowOverlapBookings: Boolean) :
        RecyclerView.Adapter<InstrumentBookingAdapter.BookingViewHolder>() {

        private var bookings = listOf<BookingItem>()

        fun submitList(newList: List<BookingItem>) {
            bookings = newList.sortedBy { parseApiDateTime(it.usage.timeStarted) }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_instrument_booking, parent, false)
            return BookingViewHolder(view)
        }

        override fun getItemCount() = bookings.size

        override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
            val booking = bookings[position]
            val hasOverlap = if (position > 0) {
                checkForOverlap(booking, bookings[position - 1])
            } else false

            holder.bind(booking, hasOverlap && allowOverlapBookings)
        }

        private fun checkForOverlap(booking1: BookingItem, booking2: BookingItem): Boolean {
            val start1 = parseApiDateTime(booking1.usage.timeStarted)
            val end1 = parseApiDateTime(booking1.usage.timeEnded)
            val start2 = parseApiDateTime(booking2.usage.timeStarted)
            val end2 = parseApiDateTime(booking2.usage.timeEnded)

            return (start1 < end2 && start2 < end1)
        }

        inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val instrumentName: TextView = itemView.findViewById(R.id.instrumentName)
            private val bookingTime: TextView = itemView.findViewById(R.id.bookingTime)
            private val bookingStatus: TextView = itemView.findViewById(R.id.bookingStatus)
            private val bookingDescription: TextView = itemView.findViewById(R.id.bookingDescription)

            fun bind(item: BookingItem, isOverlapping: Boolean) {
                instrumentName.text = item.instrument.instrumentName ?: "Unknown Instrument"

                val startTime = parseApiDateTime(item.usage.timeStarted)
                val endTime = parseApiDateTime(item.usage.timeEnded)
                bookingTime.text = "From: ${formatDateForDisplay(startTime)}\nTo: ${formatDateForDisplay(endTime)}"

                val isApproved = item.usage.approved == true

                val statusText = if (isOverlapping) {
                    if (isApproved) "Approved (Overlapping)" else "Pending Approval (Overlapping)"
                } else {
                    if (isApproved) "Approved" else "Pending Approval"
                }
                bookingStatus.text = statusText

                val colorResId = when {
                    isOverlapping -> R.color.warning
                    isApproved -> R.color.success
                    else -> R.color.danger
                }
                bookingStatus.setTextColor(ContextCompat.getColor(context, colorResId))

                //if (isOverlapping) {
                //    itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.warning))
                //} else {
                 //   itemView.setBackgroundColor(Color.TRANSPARENT)
                //}

                bookingDescription.text = item.usage.description ?: "No description provided"
            }
        }
    }
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
    return displayFormat.format(Date(timestamp))
}