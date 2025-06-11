package info.proteo.cupcake.ui.session

import android.content.Context
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
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import info.proteo.cupcake.data.remote.model.instrument.Instrument
import info.proteo.cupcake.data.remote.model.instrument.InstrumentUsage
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

class InstrumentAnnotationHandler(
    private val context: Context,
    private val instrumentRepository: InstrumentRepository,
    private val instrumentUsageRepository: InstrumentUsageRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val displayDateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    fun displayInstrumentBooking(
        annotation: Annotation,
        container: ViewGroup
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
        Log.d("InstrumentAnnotationHandler", "Displaying instrument booking for annotation: ${annotation.id}")
        try {
            Log.d("InstrumentAnnotationHandler", "Processing annotation: ${annotation.id} with instrumentUsage: ${annotation.instrumentUsage?.size ?: 0} usages")
            val usageIds = annotation.instrumentUsage?.map { it.id } ?: emptyList()

            if (usageIds.isEmpty()) {
                progressBar.visibility = View.GONE
                noBookingsText.visibility = View.VISIBLE
                return
            }

            val bookingAdapter = InstrumentBookingAdapter()
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = bookingAdapter
            Log.d("InstrumentAnnotationHandler", "Loading instrument bookings for IDs: $usageIds")
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

    inner class InstrumentBookingAdapter :
        RecyclerView.Adapter<InstrumentBookingAdapter.BookingViewHolder>() {

        private var bookings = listOf<BookingItem>()

        fun submitList(newList: List<BookingItem>) {
            bookings = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_instrument_booking, parent, false)
            return BookingViewHolder(view)
        }

        override fun getItemCount() = bookings.size

        override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
            holder.bind(bookings[position])
        }

        inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val instrumentName: TextView = itemView.findViewById(R.id.instrumentName)
            private val bookingTime: TextView = itemView.findViewById(R.id.bookingTime)
            private val bookingStatus: TextView = itemView.findViewById(R.id.bookingStatus)
            private val bookingDescription: TextView = itemView.findViewById(R.id.bookingDescription)

            fun bind(item: BookingItem) {
                instrumentName.text = item.instrument.instrumentName ?: "Unknown Instrument"

                val startTime = parseAndFormatDate(item.usage.timeStarted)
                val endTime = parseAndFormatDate(item.usage.timeEnded)
                bookingTime.text = "From: $startTime\nTo: $endTime"

                val isApproved = item.usage.approved == true
                bookingStatus.text = if (isApproved) "Approved" else "Pending Approval"

                val colorResId = if (isApproved) {
                    R.color.success
                } else {
                    R.color.danger
                }
                bookingStatus.setTextColor(ContextCompat.getColor(context, colorResId))

                bookingDescription.text = item.usage.description ?: "No description provided"
            }

            private fun parseAndFormatDate(dateString: String?): String {
                if (dateString == null) return "N/A"

                return try {
                    val date = dateFormat.parse(dateString)
                    displayDateFormat.format(date ?: Date())
                } catch (e: Exception) {
                    dateString
                }
            }
        }
    }
}