package info.proteo.cupcake.ui.instrument

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import info.proteo.cupcake.databinding.ItemInstrumentAnnotationBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.util.Log

class InstrumentAnnotationAdapter(
    private val onItemClicked: (Annotation) -> Unit
) : ListAdapter<Annotation, InstrumentAnnotationAdapter.InstrumentAnnotationViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstrumentAnnotationViewHolder {
        val binding = ItemInstrumentAnnotationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return InstrumentAnnotationViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: InstrumentAnnotationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InstrumentAnnotationViewHolder(
        private val binding: ItemInstrumentAnnotationBinding,
        private val onItemClicked: (Annotation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(annotation: Annotation) {
            binding.annotationName.text = annotation.annotationName ?: "Unnamed Annotation"
            binding.annotationText.text = annotation.annotation ?: "No content"
            binding.annotationCreatedAt.text = formatDate(annotation.createdAt)

            binding.root.setOnClickListener {
                onItemClicked(annotation)
            }
        }

        private fun formatDate(timestamp: String?): String {
            if (timestamp.isNullOrEmpty()) return "N/A"
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(timestamp) ?: return timestamp

                val outputFormat = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
                outputFormat.timeZone = TimeZone.getDefault() // Local time zone
                return outputFormat.format(date)
            } catch (e: Exception) {
                Log.e("AnnotationAdapter", "Error formatting date: $timestamp", e)

                try {
                    val fallbackInputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    fallbackInputFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val date = fallbackInputFormat.parse(timestamp) ?: return timestamp
                    val outputFormat = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
                    outputFormat.timeZone = TimeZone.getDefault()
                    return outputFormat.format(date)
                } catch (e2: Exception) {
                    Log.e("AnnotationAdapter", "Error formatting date with fallback: $timestamp", e2)
                    return timestamp
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Annotation>() {
            override fun areItemsTheSame(oldItem: Annotation, newItem: Annotation): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Annotation, newItem: Annotation): Boolean {
                return oldItem == newItem
            }
        }
    }
}