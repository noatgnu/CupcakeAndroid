package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.data.remote.model.annotation.Annotation
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
import info.proteo.cupcake.data.repository.InstrumentRepository
import info.proteo.cupcake.data.repository.InstrumentUsageRepository


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
    private val annotationRepository: AnnotationRepository,
    private val instrumentRepository: InstrumentRepository,
    private val instrumentUsageRepository: InstrumentUsageRepository,

    private val baseUrl: String
) : ListAdapter<Annotation, SessionAnnotationAdapter.ViewHolder>(DIFF_CALLBACK) {
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
        private val textAnnotation: TextView = itemView.findViewById(R.id.annotation_text)
        private val textDate: TextView = itemView.findViewById(R.id.annotation_created_date)
        private val textUsername: TextView = itemView.findViewById(R.id.annotation_username)
        private val menuButton: ImageButton = itemView.findViewById(R.id.menu_button)
        private var vttCues: List<VttCue> = emptyList()
        private val annotationImage: ImageView = itemView.findViewById(R.id.annotation_image)
        private val imageContainer: ViewGroup = itemView.findViewById(R.id.image_container)

        private val checklistContainer = itemView.findViewById<ViewGroup>(R.id.checklist_container)
        private val counterContainer = itemView.findViewById<ViewGroup>(R.id.counter_container)
        private val tableContainer = itemView.findViewById<ViewGroup>(R.id.table_container)
        private val calculatorContainer = itemView.findViewById<ViewGroup>(R.id.calculator_container) // Add this

        private val randomizationContainer = itemView.findViewById<ViewGroup>(R.id.randomization_container)
        private val mediaPlayerContainer: View? = itemView.findViewById(R.id.mediaPlayerContainer)
        private val playButton: ImageButton? = itemView.findViewById(R.id.playButton)
        private val progressBar: ProgressBar? = itemView.findViewById(R.id.mediaLoadingProgressBar)

        private val transcriptionContainer: View = itemView.findViewById(R.id.transcription_container)
        private val transcriptionText: TextView = itemView.findViewById(R.id.transcription_text)
        private val instrumentContainer = itemView.findViewById<ViewGroup>(R.id.instrument_container)

        fun bind(annotation: Annotation) {
            menuButton.setOnClickListener { view ->
                showPopupMenu(view, annotation)
            }
            val formattedDate = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(annotation.createdAt)

                val outputFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                outputFormat.timeZone = TimeZone.getDefault()
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                annotation.createdAt
            }

            textAnnotation.text = annotation.annotation
            textDate.text = formattedDate
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


            when (annotation.annotationType) {
                "instrument" -> {
                    textAnnotation.visibility = View.GONE
                    instrumentContainer.visibility = View.VISIBLE
                    getInstrumentAnnotationHandler(itemView.context)
                        .displayInstrumentBooking(annotation, instrumentContainer)
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
                            transcriptionText,
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
                    textAnnotation.visibility = View.VISIBLE // Or GONE if only image is shown
                    imageContainer.visibility = View.VISIBLE
                    annotationImage.visibility = View.VISIBLE // Make sure ImageView itself is visible
                    // Ensure image is reset for recycled views
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
                        transcriptionText
                    )
                }
                else -> {
                    textAnnotation.visibility = View.GONE
                    annotationImage.visibility = View.GONE
                    mediaPlayerContainer?.visibility = View.GONE
                    transcriptionContainer.visibility = View.GONE
                }
            }

            //itemView.setOnClickListener { onItemClick(annotation) }
        }
    }

    private fun showPopupMenu(view: View, annotation: Annotation) {
        val popup = PopupMenu(view.context, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.session_annotation_item_menu, popup.menu)

        popup.menu.findItem(R.id.action_download).isVisible = annotation.file != null

        popup.menu.findItem(R.id.action_retranscribe).isVisible =
            (annotation.annotationType == "audio" || annotation.annotationType == "video") &&
                !annotation.transcription.isNullOrBlank()

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
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




    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        this.recyclerView = null
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }




    private fun loadAnnotationImage(annotation: Annotation, imageView: ImageView) {
        val progressBar = ProgressBar(imageView.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Add progress indicator to parent layout
        val parent = imageView.parent as ViewGroup
        val imageIndex = parent.indexOfChild(imageView)
        parent.addView(progressBar, imageIndex)

        // Request signed URL in a coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = annotationRepository.getSignedUrl(annotation.id)

                if (result.isSuccess) {
                    val signedToken = result.getOrNull()?.signedToken
                    if (signedToken != null) {
                        val signedUrl = "${baseUrl}/api/annotation/download_signed/?token=$signedToken"

                        withContext(Dispatchers.Main) {
                            Glide.with(imageView)
                                .load(signedUrl)
                                .listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        parent.removeView(progressBar)
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: Drawable,
                                        model: Any?,
                                        target: Target<Drawable>,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        parent.removeView(progressBar)
                                        return false
                                    }
                                })
                                .into(imageView)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            parent.removeView(progressBar)
                            showImageLoadError("Failed to get image URL", imageView)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        parent.removeView(progressBar)
                        showImageLoadError("Error: ${result.exceptionOrNull()?.message}", imageView)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    parent.removeView(progressBar)
                    showImageLoadError("Error: ${e.message}", imageView)
                }
            }
        }
    }

    private fun showImageLoadError(message: String, imageView: ImageView) {
        Toast.makeText(imageView.context, message, Toast.LENGTH_SHORT).show()
        imageView.setImageResource(R.drawable.ic_more_vert)
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