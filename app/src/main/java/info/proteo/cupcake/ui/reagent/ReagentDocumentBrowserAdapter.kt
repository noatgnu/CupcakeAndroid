package info.proteo.cupcake.ui.reagent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ReagentDocumentBrowserAdapter: RecyclerView.Adapter<ReagentDocumentBrowserAdapter.DocumentViewHolder>() {
    private var documents = emptyList<Annotation>()
    private var onItemClickListener: ((Annotation) -> Unit)? = null

    inner class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewName: TextView = itemView.findViewById(R.id.textViewDocumentName)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewDocumentDate)
        private val imageViewIcon: ImageView = itemView.findViewById(R.id.imageViewDocumentIcon)

        fun bind(document: Annotation) {
            textViewName.text = document.annotationName ?: "Unnamed document"
            textViewDate.text = document.updatedAt?.let { formatDate(it) } ?: "Unknown date"

            // Set appropriate icon based on file type
            val fileExtension = document.file?.substringAfterLast('.', "")?.lowercase() ?: ""
            val iconResId = when {
                fileExtension.matches(Regex("jpe?g|png|gif|bmp")) -> R.drawable.ic_image
                fileExtension.matches(Regex("pdf")) -> R.drawable.ic_pdf
                fileExtension.matches(Regex("docx?|xlsx?|pptx?")) -> R.drawable.ic_document
                else -> R.drawable.ic_file
            }
            imageViewIcon.setImageResource(iconResId)

            itemView.setOnClickListener {
                onItemClickListener?.invoke(document)
            }
        }

        private fun formatDate(timestamp: String): String {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(timestamp) ?: return timestamp
                return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
            } catch (e: Exception) {
                return timestamp
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reagent_document_browser, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(documents[position])
    }

    override fun getItemCount(): Int = documents.size

    fun submitList(newDocuments: List<Annotation>) {
        documents = newDocuments
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (Annotation) -> Unit) {
        onItemClickListener = listener
    }
}