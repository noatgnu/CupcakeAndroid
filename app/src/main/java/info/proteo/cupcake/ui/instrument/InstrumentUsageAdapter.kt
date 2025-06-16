package info.proteo.cupcake.ui.instrument

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.shared.data.model.instrument.InstrumentUsage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InstrumentUsageAdapter(private val bookings: List<InstrumentUsage>) :
    RecyclerView.Adapter<InstrumentUsageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_instrument_booking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bind(booking)
    }

    override fun getItemCount() = bookings.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val instrumentName: TextView = view.findViewById(R.id.instrumentName)
        private val bookingStatus: TextView = view.findViewById(R.id.bookingStatus)
        private val bookingTime: TextView = view.findViewById(R.id.bookingTime)
        private val bookingDescription: TextView = view.findViewById(R.id.bookingDescription)

        fun bind(booking: InstrumentUsage) {
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            val startTime = parseApiDateTime(booking.timeStarted)
            val endTime = parseApiDateTime(booking.timeEnded)
            instrumentName.visibility = View.GONE
            bookingStatus.text = if (booking.approved == true) "Approved" else "Pending"
            bookingTime.text = "${dateFormat.format(Date(startTime))} - ${dateFormat.format(Date(endTime))}"
            bookingDescription.visibility = View.GONE
        }

        private fun parseApiDateTime(dateTimeString: String?): Long {
            if (dateTimeString == null) return 0L
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                format.parse(dateTimeString)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }
}