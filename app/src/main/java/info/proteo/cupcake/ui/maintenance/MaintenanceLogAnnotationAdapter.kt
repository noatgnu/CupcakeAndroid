package info.proteo.cupcake.ui.maintenance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.databinding.ItemMaintenanceLogAnnotationBinding
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import java.text.SimpleDateFormat
import java.util.Locale

class MaintenanceLogAnnotationAdapter(
    private val onItemClick: (Annotation) -> Unit,
    private val onDeleteClick: (Annotation) -> Unit
) : ListAdapter<Annotation, MaintenanceLogAnnotationAdapter.AnnotationViewHolder>(AnnotationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnotationViewHolder {
        val binding = ItemMaintenanceLogAnnotationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnnotationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnnotationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AnnotationViewHolder(
        private val binding: ItemMaintenanceLogAnnotationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(annotation: Annotation) {
            binding.apply {
                // Annotation name/title
                textViewAnnotationName.text = annotation.annotationName ?: "Untitled"

                // Annotation type and content info
                when {
                    annotation.annotation?.isNotBlank() == true -> {
                        imageViewAnnotationType.setImageResource(info.proteo.cupcake.R.drawable.ic_edit)
                        textViewAnnotationType.text = "Text Note"
                        textViewAnnotationPreview.isVisible = true
                        textViewAnnotationPreview.text = annotation.annotation?.take(100)?.let {
                            if (annotation.annotation!!.length > 100) "$it..." else it
                        }
                    }
                    annotation.file?.isNotBlank() == true -> {
                        val fileName = annotation.file!!.substringAfterLast("/")
                        val fileExtension = fileName.substringAfterLast(".", "")
                        
                        // Set icon based on file type
                        imageViewAnnotationType.setImageResource(getFileIcon(fileExtension))
                        textViewAnnotationType.text = "File: $fileName"
                        textViewAnnotationPreview.isVisible = true
                        textViewAnnotationPreview.text = "File size: ${getFileSize(annotation)}"
                    }
                    else -> {
                        imageViewAnnotationType.setImageResource(info.proteo.cupcake.R.drawable.ic_attachment)
                        textViewAnnotationType.text = "Unknown"
                        textViewAnnotationPreview.isVisible = false
                    }
                }

                // Created date
                textViewCreatedDate.text = formatDate(annotation.createdAt)

                // Created by user
                textViewCreatedBy.text = annotation.user?.fullName ?: "Unknown"

                // Click listeners
                root.setOnClickListener {
                    onItemClick(annotation)
                }

                buttonDelete.setOnClickListener {
                    onDeleteClick(annotation)
                }

                // Hide delete button if user doesn't have permission
                // TODO: Add permission check based on user role/ownership
                buttonDelete.isVisible = true
            }
        }

        private fun getFileIcon(extension: String): Int {
            return when (extension.lowercase()) {
                "pdf" -> info.proteo.cupcake.R.drawable.ic_attachment
                "doc", "docx" -> info.proteo.cupcake.R.drawable.ic_edit
                "xls", "xlsx" -> info.proteo.cupcake.R.drawable.ic_attachment
                "ppt", "pptx" -> info.proteo.cupcake.R.drawable.ic_attachment
                "jpg", "jpeg", "png", "gif", "bmp" -> info.proteo.cupcake.R.drawable.ic_attachment
                "mp4", "avi", "mov", "wmv" -> info.proteo.cupcake.R.drawable.ic_attachment
                "mp3", "wav", "aac" -> info.proteo.cupcake.R.drawable.ic_attachment
                "zip", "rar", "7z" -> info.proteo.cupcake.R.drawable.ic_attachment
                "txt", "log" -> info.proteo.cupcake.R.drawable.ic_edit
                else -> info.proteo.cupcake.R.drawable.ic_attachment
            }
        }

        private fun getFileSize(annotation: Annotation): String {
            // TODO: Get actual file size from annotation metadata if available
            return "Unknown size"
        }

        private fun formatDate(dateString: String?): String {
            if (dateString.isNullOrBlank()) return "Unknown date"
            
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString.split("T").firstOrNull() ?: dateString
            }
        }
    }

    private class AnnotationDiffCallback : DiffUtil.ItemCallback<Annotation>() {
        override fun areItemsTheSame(oldItem: Annotation, newItem: Annotation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Annotation, newItem: Annotation): Boolean {
            return oldItem == newItem
        }
    }
}