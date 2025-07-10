package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.R
import info.proteo.cupcake.data.repository.AnnotationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.RequestListener
import info.proteo.cupcake.data.local.entity.user.UserPreferencesEntity
import info.proteo.cupcake.shared.data.model.annotation.AnnotationWithPermissions
import info.proteo.cupcake.data.repository.InstrumentRepository
import info.proteo.cupcake.data.repository.InstrumentUsageRepository
import kotlin.or
import kotlin.text.format
import kotlin.text.toInt
import kotlin.toString


data class VttCue(
    val startTime: Long,
    val endTime: Long,
    val text: String
)

data class ChecklistData(
    val name: String,
    val checkList: List<ChecklistItem>
)

data class ChecklistItem(
    var checked: Boolean,
    val content: String
)

data class CounterData(
    val name: String,
    val total: Int,
    val current: Int
)

data class TableData(
    val name: String,
    val nRow: Int,
    val nCol: Int,
    val content: List<List<String>>,
    val tracking: Boolean,
    val trackingMap: Map<String, Boolean>
)

class SessionAnnotationAdapter(
    private val onItemClick: (Annotation) -> Unit,
    private val onRetranscribeClick: (Annotation) -> Unit,
    private val onAnnotationUpdate: (Annotation, String?, String?) -> Unit,
    private val onAnnotationRename: (Annotation) -> Unit,
    private val onAnnotationDelete: (Annotation) -> Unit,
    private val annotationRepository: AnnotationRepository,
    private val instrumentRepository: InstrumentRepository,
    private val instrumentUsageRepository: InstrumentUsageRepository,
    private val userPreferencesEntity: UserPreferencesEntity,

    private val baseUrl: String
) : ListAdapter<AnnotationWithPermissions, SessionAnnotationAdapter.ViewHolder>(DIFF_CALLBACK) {
    private var recyclerView: RecyclerView? = null

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingAnnotationId: Int? = null
    private var currentMaxPositionReached: Int = 0

    private var mediaPlayerUpdateHandler: android.os.Handler? = null
    private var mediaPlayerUpdateRunnable: Runnable? = null

    private var tableAnnotationHandler: TableAnnotationHandler? = null
    private var counterAnnotationHandler: CounterAnnotationHandler? = null
    private var checklistAnnotationHandler: ChecklistAnnotationHandler? = null
    private var mediaAnnotationHandler: MediaAnnotationHandler? = null
    private var calculatorAnnotationHandler: CalculatorAnnotationHandler? = null
    private var mCalculatorAnnotationHandler: MCalculatorAnnotationHandler? = null
    private var randomizationAnnotationHandler: RandomizationAnnotationHandler? = null
    private var imageAnnotationHandler: ImageAnnotationHandler? = null
    private var sketchAnnotationHandler: SketchAnnotationHandler? = null
    private var alignmentAnnotationHandler: AlignmentAnnotationHandler? = null
    private var instrumentAnnotationHandler: InstrumentAnnotationHandler? = null

    private fun getInstrumentAnnotationHandler(context: Context): InstrumentAnnotationHandler {
        if (instrumentAnnotationHandler == null) {
            instrumentAnnotationHandler = InstrumentAnnotationHandler(
                context,
                instrumentRepository,
                instrumentUsageRepository
            )
        }
        return instrumentAnnotationHandler!!
    }

    private fun getAlignmentAnnotationHandler(context: Context): AlignmentAnnotationHandler {
        if (alignmentAnnotationHandler == null) {
            alignmentAnnotationHandler = AlignmentAnnotationHandler(context, annotationRepository, baseUrl)
        }
        return alignmentAnnotationHandler!!
    }

    private fun getSketchAnnotationHandler(context: Context): SketchAnnotationHandler {
        if (sketchAnnotationHandler == null) {
            sketchAnnotationHandler = SketchAnnotationHandler(context, annotationRepository, baseUrl)
        }
        return sketchAnnotationHandler!!
    }

    private fun getImageAnnotationHandler(context: Context): ImageAnnotationHandler {
        if (imageAnnotationHandler == null) {
            imageAnnotationHandler = ImageAnnotationHandler(context, annotationRepository, baseUrl)
        }
        return imageAnnotationHandler!!
    }

    private fun getRandomizationAnnotationHandler(context: Context): RandomizationAnnotationHandler {
        if (randomizationAnnotationHandler == null) {
            randomizationAnnotationHandler = RandomizationAnnotationHandler(context)
        }
        return randomizationAnnotationHandler!!
    }

    private fun getMCalculatorAnnotationHandler(context: Context): MCalculatorAnnotationHandler {
        if (mCalculatorAnnotationHandler == null) {
            mCalculatorAnnotationHandler = MCalculatorAnnotationHandler(context, onAnnotationUpdate)
        }
        return mCalculatorAnnotationHandler!!
    }

    private fun getCalculatorAnnotationHandler(context: Context): CalculatorAnnotationHandler {
        if (calculatorAnnotationHandler == null) {
            calculatorAnnotationHandler = CalculatorAnnotationHandler(context, onAnnotationUpdate)
        }
        return calculatorAnnotationHandler!!
    }

    private fun getMediaAnnotationHandler(context: Context): MediaAnnotationHandler {
        if (mediaAnnotationHandler == null) {
            mediaAnnotationHandler = MediaAnnotationHandler(context, annotationRepository, baseUrl, onAnnotationUpdate)
        }
        return mediaAnnotationHandler!!
    }

    private fun getTableAnnotationHandler(context: Context): TableAnnotationHandler {
        if (tableAnnotationHandler == null) {
            tableAnnotationHandler = TableAnnotationHandler(context, onAnnotationUpdate)
        }
        return tableAnnotationHandler!!
    }

    private fun getCounterAnnotationHandler(context: Context): CounterAnnotationHandler {
        if (counterAnnotationHandler == null) {
            counterAnnotationHandler = CounterAnnotationHandler(context, onAnnotationUpdate)
        }
        return counterAnnotationHandler!!
    }

    private fun getChecklistAnnotationHandler(context: Context): ChecklistAnnotationHandler {
        if (checklistAnnotationHandler == null) {
            checklistAnnotationHandler = ChecklistAnnotationHandler(context, onAnnotationUpdate)
        }
        return checklistAnnotationHandler!!
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session_annotation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val annotation = getItem(position)
        holder.bind(annotation)

    }



    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val metadataContainer = itemView.findViewById<ViewGroup>(R.id.metadata_container)
        private val textAnnotation: TextView = itemView.findViewById(R.id.annotation_text)
        private val createdDate: TextView = itemView.findViewById(R.id.annotation_created_date)
        private val textUsername: TextView = itemView.findViewById(R.id.annotation_username)
        private val menuButton: ImageButton = itemView.findViewById(R.id.menu_button)
        private var vttCues: List<VttCue> = emptyList()
        private val annotationImage: ImageView = itemView.findViewById(R.id.annotation_image)
        private val imageContainer: ViewGroup = itemView.findViewById(R.id.image_container)
        private val annotationName: TextView = itemView.findViewById(R.id.annotation_name)
        private val annotationUpdatedDate: TextView = itemView.findViewById(R.id.annotation_updated_date)
        private val checklistContainer = itemView.findViewById<ViewGroup>(R.id.checklist_container)
        private val counterContainer = itemView.findViewById<ViewGroup>(R.id.counter_container)
        private val tableContainer = itemView.findViewById<ViewGroup>(R.id.table_container)
        private val calculatorContainer = itemView.findViewById<ViewGroup>(R.id.calculator_container) // Add this

        private val randomizationContainer = itemView.findViewById<ViewGroup>(R.id.randomization_container)
        private val mediaPlayerContainer: View? = itemView.findViewById(R.id.mediaPlayerContainer)
        private val playButton: ImageButton? = itemView.findViewById(R.id.playButton)
        private val progressBar: ProgressBar? = itemView.findViewById(R.id.mediaLoadingProgressBar)

        private val transcriptionContainer: View = itemView.findViewById(R.id.transcription_container)
        private val transcriptionText: TextView? = itemView.findViewById(R.id.transcription_text)
        private val instrumentContainer = itemView.findViewById<ViewGroup>(R.id.instrument_container)
        private val mcalculatorContainer = itemView.findViewById<ViewGroup>(R.id.molarityCalculatorContainer)

        fun bind(annotationWithPermissions: AnnotationWithPermissions) {
            metadataContainer.removeAllViews()
            val annotation = annotationWithPermissions.annotation
            menuButton.setOnClickListener { view ->
                showPopupMenu(view, annotationWithPermissions)
            }
            Log.d("SessionAnnotationAdapter", "Binding annotation: editable=${annotationWithPermissions.canEdit}, deletable=${annotationWithPermissions.canDelete}")
            menuButton.visibility = if (annotationWithPermissions.canEdit ||
                annotationWithPermissions.canDelete) {
                View.VISIBLE
            } else {
                View.GONE
            }

            textAnnotation.text = annotation.annotation
            createdDate.text = "Created:  ${formatDate(annotation.createdAt)}"
            annotationUpdatedDate.text = "Updated: ${formatDate(annotation.updatedAt)}"

            if (!annotation.annotationName.isNullOrEmpty()) {
                annotationName.text = annotation.annotationName
                annotationName.visibility = View.VISIBLE
            } else {
                annotationName.visibility = View.GONE
            }



            textUsername.text = annotation.user?.username?.let { "Created by: $it" } ?: ""
            imageContainer.visibility = View.GONE

            textAnnotation.visibility = View.GONE
            annotationImage.visibility = View.GONE
            mediaPlayerContainer?.visibility = View.GONE
            transcriptionContainer.visibility = View.GONE
            checklistContainer.visibility = View.GONE
            counterContainer.visibility = View.GONE
            tableContainer.visibility = View.GONE
            instrumentContainer.visibility = View.GONE
            calculatorContainer.visibility = View.GONE
            mcalculatorContainer?.visibility = View.GONE


            when (annotation.annotationType) {
                "instrument" -> {
                    textAnnotation.visibility = View.GONE
                    instrumentContainer.visibility = View.VISIBLE
                    getInstrumentAnnotationHandler(itemView.context)
                        .displayInstrumentBooking(annotation, instrumentContainer, userPreferencesEntity)
                }
                "alignment" -> {
                    textAnnotation.visibility = View.GONE
                    annotationImage.visibility = View.VISIBLE
                    imageContainer.visibility = View.VISIBLE
                    mediaPlayerContainer?.visibility = View.GONE
                    getAlignmentAnnotationHandler(itemView.context)
                        .displayAlignment(annotation, annotationImage, imageContainer)
                }
                "sketch" -> {
                    textAnnotation.visibility = View.VISIBLE
                    annotationImage.visibility = View.VISIBLE
                    imageContainer.visibility = View.VISIBLE
                    mediaPlayerContainer?.visibility = View.GONE
                    getSketchAnnotationHandler(itemView.context)
                        .displaySketch(
                            annotation,
                            annotationImage,
                            transcriptionText ?: TextView(itemView.context),
                            imageContainer,
                            transcriptionContainer
                        )
                }
                "randomization" -> {
                    textAnnotation.visibility = View.GONE
                    randomizationContainer.visibility = View.VISIBLE
                    getRandomizationAnnotationHandler(itemView.context)
                        .displayRandomizationData(annotation, randomizationContainer)
                }
                "mcalculator" -> {
                    calculatorContainer.visibility = View.VISIBLE
                    getMCalculatorAnnotationHandler(itemView.context).displayMolarityCalculator(annotation, calculatorContainer)
                }
                "calculator" -> {
                    calculatorContainer.visibility = View.VISIBLE
                    getCalculatorAnnotationHandler(itemView.context).displayCalculator(annotation, calculatorContainer)
                }
                "table" -> {
                    tableContainer.visibility = View.VISIBLE
                    getTableAnnotationHandler(itemView.context)
                        .displayTable(annotation, tableContainer)
                }
                "counter" -> {
                    counterContainer.visibility = View.VISIBLE
                    getCounterAnnotationHandler(itemView.context)
                        .displayCounter(annotation, counterContainer)
                }
                "checklist" -> {
                    checklistContainer.visibility = View.VISIBLE
                    getChecklistAnnotationHandler(itemView.context).displayChecklist(annotation, checklistContainer)
                }
                "text", "file" -> {
                    textAnnotation.visibility = View.VISIBLE
                    annotationImage.visibility = View.GONE
                    mediaPlayerContainer?.visibility = View.GONE
                    transcriptionContainer.visibility = View.GONE
                }
                "image" -> {
                    textAnnotation.visibility = View.VISIBLE
                    imageContainer.visibility = View.VISIBLE
                    annotationImage.visibility = View.VISIBLE
                    annotationImage.setImageDrawable(null)
                    getImageAnnotationHandler(itemView.context).displayImage(annotation, annotationImage, imageContainer)
                }
                "audio", "video" -> {
                    mediaPlayerContainer?.visibility = View.VISIBLE

                    val mediaSeekBar = itemView.findViewById<SeekBar>(R.id.mediaSeekBar)
                    val mediaTimerText = itemView.findViewById<TextView>(R.id.mediaTimerText)

                    getMediaAnnotationHandler(itemView.context).displayMedia(
                        annotation,
                        playButton,
                        mediaSeekBar,
                        mediaTimerText,
                        progressBar,
                        transcriptionContainer,
                        transcriptionText ?: TextView(itemView.context)
                    )
                }
                else -> {
                    textAnnotation.visibility = View.GONE
                    annotationImage.visibility = View.GONE
                    mediaPlayerContainer?.visibility = View.GONE
                    transcriptionContainer.visibility = View.GONE
                }
            }
            displayMetadataBadges(annotation, itemView, metadataContainer)

            //itemView.setOnClickListener { onItemClick(annotation) }
        }
    }

    private fun displayMetadataBadges(annotation: Annotation, itemView: View, metadataContainer: ViewGroup) {
        val metadataColumns = annotation.metadataColumns ?: return

        if (metadataColumns.isEmpty()) {
            metadataContainer.visibility = View.GONE
            return
        }

        metadataContainer.visibility = View.VISIBLE

        val context = itemView.context
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 8.dpToPx(context), 4.dpToPx(context))
        }

        for (metadata in metadataColumns) {
            if (metadata.value.isNullOrEmpty()) continue

            val badge = TextView(context).apply {
                text = "${metadata.name}: ${metadata.value}"
                setTextColor(Color.WHITE)
                setPadding(8.dpToPx(context), 4.dpToPx(context), 8.dpToPx(context), 4.dpToPx(context))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)

                // Set background color based on type
                background = when (metadata.type) {
                    "Characteristics" -> getBackgroundDrawable(context, R.color.primary)
                    "Comment" -> getBackgroundDrawable(context, R.color.secondary)
                    "Factor value" -> getBackgroundDrawable(context, R.color.success)
                    else -> getBackgroundDrawable(context, R.color.danger)
                }
            }

            metadataContainer.addView(badge, layoutParams)
        }
    }

    private fun getBackgroundDrawable(context: Context, colorResId: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = 16f
            setColor(ContextCompat.getColor(context, colorResId))
        }
    }

    private fun showPopupMenu(view: View, annotationWithPermissions: AnnotationWithPermissions) {
        val annotation = annotationWithPermissions.annotation
        val popup = PopupMenu(view.context, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.session_annotation_item_menu, popup.menu)

        popup.menu.findItem(R.id.action_download).isVisible = annotation.file != null
        popup.menu.findItem(R.id.action_retranscribe).isVisible =
            (annotation.annotationType == "audio" || annotation.annotationType == "video") &&
                !annotation.transcription.isNullOrBlank() && userPreferencesEntity.useWhisper

        popup.menu.findItem(R.id.action_edit).isVisible = false

        if (annotationWithPermissions.canEdit) {
            popup.menu.add(0, R.id.action_rename, 0, "Rename").isVisible = true

            if (annotation.annotationType == "text" || annotation.annotationType == "file") {
                popup.menu.add(0, R.id.action_edit, 0, "Edit Content").isVisible = true
            }
        }

        popup.menu.findItem(R.id.action_delete).isVisible = annotationWithPermissions.canDelete

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_rename -> {
                    showRenameDialog(annotation, view)
                    true
                }
                R.id.action_edit -> {
                    showEditContentDialog(annotation, view)
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmationDialog(annotation, view)
                    true
                }
                R.id.action_download -> {
                    true
                }
                R.id.action_retranscribe -> {
                    onRetranscribeClick(annotation)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun showRenameDialog(annotation: Annotation, itemView: View) {
        val context = itemView.context

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dpToPx(context), 8.dpToPx(context), 16.dpToPx(context), 0)
        }

        val nameEditText = EditText(context).apply {
            setText(annotation.annotationName)
            isSingleLine = true
            hint = "Enter annotation title"
        }

        layout.addView(nameEditText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        AlertDialog.Builder(context)
            .setTitle("Rename Annotation")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val updatedName = nameEditText.text.toString()
                if (updatedName != annotation.annotationName) {
                    val updatedAnnotation = annotation.copy(
                        annotationName = updatedName
                    )
                    onAnnotationRename(updatedAnnotation)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showEditContentDialog(annotation: Annotation, itemView: View) {
        val context = itemView.context

        if (annotation.annotationType != "text" && annotation.annotationType != "file") {
            Toast.makeText(context, "This annotation type cannot be edited", Toast.LENGTH_SHORT).show()
            return
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dpToPx(context), 8.dpToPx(context), 16.dpToPx(context), 0)
        }

        val contentEditText = EditText(context).apply {
            setText(annotation.annotation)
            gravity = Gravity.TOP or Gravity.START
            minLines = 5
            isSingleLine = false
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        layout.addView(contentEditText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        AlertDialog.Builder(context)
            .setTitle("Edit Content")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val updatedContent = contentEditText.text.toString()
                if (updatedContent != annotation.annotation) {
                    val updatedAnnotation = annotation.copy(
                        annotation = updatedContent
                    )
                    onAnnotationUpdate(updatedAnnotation, null, null)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showDeleteConfirmationDialog(annotation: Annotation, itemView: View) {
        AlertDialog.Builder(itemView.context)
            .setTitle("Delete Annotation")
            .setMessage("Are you sure you want to delete this annotation?")
            .setPositiveButton("Delete") { _, _ ->
                onAnnotationDelete(annotation)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        this.recyclerView = null
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }


    private fun formatDate(dateString: String?): String {
        return try {
            if (dateString == null) return ""

            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString)

            val outputFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault()
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString ?: ""
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AnnotationWithPermissions>() {
            override fun areItemsTheSame(oldItem: AnnotationWithPermissions, newItem: AnnotationWithPermissions): Boolean {
                return oldItem.annotation.id == newItem.annotation.id
            }

            override fun areContentsTheSame(oldItem: AnnotationWithPermissions, newItem: AnnotationWithPermissions): Boolean {
                return oldItem == newItem
            }
        }
    }
}