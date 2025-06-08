import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
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
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.RequestListener


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
    private val baseUrl: String
) : ListAdapter<Annotation, SessionAnnotationAdapter.ViewHolder>(DIFF_CALLBACK) {
    private var recyclerView: RecyclerView? = null

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingAnnotationId: Int? = null
    private var currentMaxPositionReached: Int = 0

    private var mediaPlayerUpdateHandler: android.os.Handler? = null
    private var mediaPlayerUpdateRunnable: Runnable? = null


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
        private val checklistContainer = itemView.findViewById<ViewGroup>(R.id.checklist_container)
        private val counterContainer = itemView.findViewById<ViewGroup>(R.id.counter_container)
        private val tableContainer = itemView.findViewById<ViewGroup>(R.id.table_container)



        private val mediaPlayerContainer: View? = itemView.findViewById(R.id.mediaPlayerContainer)
        private val playButton: ImageButton? = itemView.findViewById(R.id.playButton)
        private val progressBar: ProgressBar? = itemView.findViewById(R.id.mediaLoadingProgressBar)

        private val transcriptionContainer: View = itemView.findViewById(R.id.transcription_container)
        private val transcriptionText: TextView = itemView.findViewById(R.id.transcription_text)

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

            textAnnotation.visibility = View.GONE
            annotationImage.visibility = View.GONE
            mediaPlayerContainer?.visibility = View.GONE
            transcriptionContainer.visibility = View.GONE
            checklistContainer.visibility = View.GONE
            counterContainer.visibility = View.GONE
            tableContainer.visibility = View.GONE

            when (annotation.annotationType) {
                "table" -> {
                    tableContainer.visibility = View.VISIBLE
                    displayTable(annotation, tableContainer)
                }
                "counter" -> {
                    counterContainer.visibility = View.VISIBLE
                    displayCounter(annotation, counterContainer)
                }
                "checklist" -> {
                    checklistContainer.visibility = View.VISIBLE
                    displayChecklist(annotation, checklistContainer)
                }
                "text", "file" -> {
                    textAnnotation.visibility = View.VISIBLE
                    annotationImage.visibility = View.GONE
                    mediaPlayerContainer?.visibility = View.GONE
                    transcriptionContainer.visibility = View.GONE
                }
                "image" -> {
                    textAnnotation.visibility = View.VISIBLE
                    annotationImage.visibility = View.VISIBLE
                    mediaPlayerContainer?.visibility = View.GONE
                    transcriptionContainer.visibility = View.GONE
                    loadAnnotationImage(annotation, annotationImage)
                }
                "audio", "video" -> {
                    if (!annotation.transcription.isNullOrBlank()) {
                        transcriptionContainer.visibility = View.VISIBLE
                        vttCues = parseVttContent(annotation.transcription)
                        transcriptionText.text = buildFullTranscriptText()

                        transcriptionText.text = annotation.transcription
                    }

                    mediaPlayerContainer?.visibility = View.VISIBLE

                    val mediaSeekBar = itemView.findViewById<SeekBar>(R.id.mediaSeekBar)
                    val mediaTimerText = itemView.findViewById<TextView>(R.id.mediaTimerText)

                    if (annotation.id == currentPlayingAnnotationId) {
                        playButton?.setImageResource(R.drawable.ic_pause)
                        mediaSeekBar.visibility = View.VISIBLE
                        mediaTimerText.visibility = View.VISIBLE
                    } else {
                        playButton?.setImageResource(R.drawable.ic_play_arrow)
                        mediaSeekBar.visibility = View.GONE
                        mediaTimerText.visibility = View.GONE
                        progressBar?.visibility = View.GONE
                    }

                    playButton?.setOnClickListener {
                        if (annotation.id == currentPlayingAnnotationId && mediaPlayer?.isPlaying == true) {
                            mediaPlayer?.pause()
                            playButton.setImageResource(R.drawable.ic_play_arrow)
                        } else if (annotation.id == currentPlayingAnnotationId && mediaPlayer?.isPlaying == false) {
                            mediaPlayer?.start()
                            playButton.setImageResource(R.drawable.ic_pause)
                        } else {
                            progressBar?.visibility = View.VISIBLE
                            playButton.isEnabled = false
                            requestSignedUrlAndPlay(annotation, mediaSeekBar, mediaTimerText)
                        }
                    }

                    // Setup seekbar interaction
                    mediaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                mediaPlayer?.seekTo(progress)
                                updateTimerText(mediaTimerText)
                            }
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })
                }
                else -> {
                    textAnnotation.visibility = View.GONE
                    annotationImage.visibility = View.GONE
                    mediaPlayerContainer?.visibility = View.GONE
                    transcriptionContainer.visibility = View.GONE
                }
            }

            itemView.setOnClickListener { onItemClick(annotation) }
        }





        private fun buildFullTranscriptText(): SpannableString {
            val fullText = vttCues.joinToString("\n") { it.text }
            return SpannableString(fullText)
        }

        fun highlightCurrentTranscriptSection(currentPositionMs: Int) {
            if (vttCues.isEmpty()) return

            val fullText = vttCues.joinToString("\n") { it.text }
            val spannableString = SpannableString(fullText)

            val currentCue = vttCues.find {
                currentPositionMs >= it.startTime && currentPositionMs <= it.endTime
            }

            currentCue?.let { cue ->
                val startIndex = fullText.indexOf(cue.text)
                if (startIndex >= 0) {
                    val endIndex = startIndex + cue.text.length

                    spannableString.setSpan(
                        BackgroundColorSpan("#1565C0".toColorInt()),
                        startIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    spannableString.setSpan(

                        StyleSpan(Typeface.BOLD),
                        startIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            transcriptionText.text = spannableString

            currentCue?.let { cue ->
                val layout = transcriptionText.layout ?: return
                val startIndex = fullText.indexOf(cue.text)
                if (startIndex >= 0) {
                    val lineStart = layout.getLineForOffset(startIndex)
                    transcriptionText.scrollTo(0, layout.getLineTop(lineStart))
                }
            }
        }
        private fun createChecklistContainer(itemView: View): ViewGroup {
            val parent = (itemView as ViewGroup).getChildAt(0) as ViewGroup


            val container = LinearLayout(itemView.context).apply {
                id = View.generateViewId()
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            parent.addView(container)
            return container
        }

        private fun displayChecklist(annotation: Annotation, container: ViewGroup) {
            container.removeAllViews()

            try {
                if (annotation.annotation == null || annotation.annotation.isBlank()) {
                    val errorText = TextView(container.context).apply {
                        text = "No checklist data available"
                        setTextColor(Color.RED)
                    }
                    container.addView(errorText)
                    return
                }

                val checklistData = parseChecklistData(annotation.annotation)

                val titleView = TextView(container.context).apply {
                    text = checklistData.name
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 16
                    }
                }
                container.addView(titleView)

                val checkboxLayout = LinearLayout(container.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                checklistData.checkList.forEachIndexed { index, item ->
                    val checkbox = CheckBox(container.context).apply {
                        text = item.content
                        isChecked = item.checked
                        id = View.generateViewId()

                        setOnCheckedChangeListener { _, isChecked ->
                            updateChecklistItemState(annotation, index, isChecked)
                        }
                    }
                    checkboxLayout.addView(checkbox)
                }

                container.addView(checkboxLayout)
            } catch (e: Exception) {
                // Show error if JSON parsing fails
                val errorText = TextView(container.context).apply {
                    text = "Error loading checklist: ${e.message}"
                    setTextColor(Color.RED)
                }
                container.addView(errorText)
            }
        }

        private fun parseChecklistData(jsonString: String): ChecklistData {
            return try {
                val json = org.json.JSONObject(jsonString)
                val name = json.getString("name")
                val checklistArray = json.getJSONArray("checkList")

                val items = mutableListOf<ChecklistItem>()
                for (i in 0 until checklistArray.length()) {
                    val item = checklistArray.getJSONObject(i)
                    items.add(
                        ChecklistItem(
                            checked = item.getBoolean("checked"),
                            content = item.getString("content")
                        )
                    )
                }

                ChecklistData(name, items)
            } catch (e: Exception) {
                throw Exception("Invalid checklist format: ${e.message}")
            }
        }

        private fun updateChecklistItemState(annotation: Annotation, itemIndex: Int, isChecked: Boolean) {
            try {
                if (annotation.annotation == null || annotation.annotation.isBlank()) {
                    Toast.makeText(
                        itemView.context,
                        "No checklist data available to update",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val checklistData = parseChecklistData(annotation.annotation)

                if (itemIndex >= 0 && itemIndex < checklistData.checkList.size) {
                    checklistData.checkList[itemIndex].checked = isChecked
                }

                val json = org.json.JSONObject().apply {
                    put("name", checklistData.name)
                    put("checkList", org.json.JSONArray().apply {
                        checklistData.checkList.forEach { item ->
                            put(org.json.JSONObject().apply {
                                put("checked", item.checked)
                                put("content", item.content)
                            })
                        }
                    })
                }

                // Create updated annotation and use callback
                val updatedAnnotation = annotation.copy(annotation = json.toString())
                onAnnotationUpdate(updatedAnnotation, null, null)
            } catch (e: Exception) {
                Toast.makeText(
                    itemView.context,
                    "Error updating checklist: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        private fun displayCounter(annotation: Annotation, container: ViewGroup) {
            container.removeAllViews()

            try {
                if (annotation.annotation == null || annotation.annotation.isBlank()) {
                    val errorText = TextView(container.context).apply {
                        text = "No counter data available"
                        setTextColor(Color.RED)
                    }
                    container.addView(errorText)
                    return
                }

                val counterData = parseCounterData(annotation.annotation)
                // Create mutable variable to track current value
                var currentValue = counterData.current

                // Add title/name
                val titleView = TextView(container.context).apply {
                    text = counterData.name
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 8
                    }
                }
                container.addView(titleView)

                val counterLayout = LinearLayout(container.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val counterText = TextView(container.context).apply {
                    text = "${currentValue}/${counterData.total}"
                    textSize = 16f
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val decreaseButton = Button(container.context).apply {
                    text = "-"
                    isEnabled = currentValue > 0
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        val newValue = currentValue - 1
                        if (newValue >= 0) {
                            // Update tracked value
                            currentValue = newValue
                            // Update UI
                            counterText.text = "${currentValue}/${counterData.total}"
                            // Update button state
                            isEnabled = currentValue > 0
                            // Update backend
                            updateCounterValue(annotation, currentValue, counterData.total)
                        }
                    }
                }

                val increaseButton = Button(container.context).apply {
                    text = "+"
                    isEnabled = currentValue < counterData.total
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        val newValue = currentValue + 1
                        if (newValue <= counterData.total) {
                            // Update tracked value
                            currentValue = newValue
                            // Update UI
                            counterText.text = "${currentValue}/${counterData.total}"
                            // Update button states
                            isEnabled = currentValue < counterData.total
                            decreaseButton.isEnabled = true
                            // Update backend
                            updateCounterValue(annotation, currentValue, counterData.total)
                        }
                    }
                }

                counterLayout.addView(decreaseButton)
                counterLayout.addView(counterText)
                counterLayout.addView(increaseButton)

                container.addView(counterLayout)

            } catch (e: Exception) {
                val errorText = TextView(container.context).apply {
                    text = "Error loading counter: ${e.message}"
                    setTextColor(Color.RED)
                }
                container.addView(errorText)
            }
        }

        private fun updateCounterValue(annotation: Annotation, newValue: Int, total: Int) {
            try {
                if (annotation.annotation == null || annotation.annotation.isBlank()) {
                    Toast.makeText(
                        itemView.context,
                        "No counter data available to update",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val counterData = parseCounterData(annotation.annotation)

                // Create updated JSON
                val json = org.json.JSONObject().apply {
                    put("name", counterData.name)
                    put("total", total)
                    put("current", newValue)
                }

                // Create updated annotation and use callback
                val updatedAnnotation = annotation.copy(annotation = json.toString())
                onAnnotationUpdate(updatedAnnotation, null, null)
            } catch (e: Exception) {
                Toast.makeText(
                    itemView.context,
                    "Error updating counter: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        private fun displayTable(annotation: Annotation, container: ViewGroup) {
            container.removeAllViews()

            try {
                if (annotation.annotation == null || annotation.annotation.isBlank()) {
                    val errorText = TextView(container.context).apply {
                        text = "No table data available"
                        setTextColor(Color.RED)
                    }
                    container.addView(errorText)
                    return
                }

                val tableData = parseTableData(annotation.annotation)
                var isEditMode = false

                // Create a container for the entire table including header
                val mainLayout = LinearLayout(container.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                container.addView(mainLayout)

                // Header layout with switches
                val headerLayout = LinearLayout(container.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 8
                    }
                }

                val titleView = TextView(container.context).apply {
                    text = tableData.name
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val editToggle = androidx.appcompat.widget.SwitchCompat(container.context).apply {
                    text = "Edit"
                    isChecked = isEditMode
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = 8
                    }

                    setOnCheckedChangeListener { _, isChecked ->
                        isEditMode = isChecked
                        // Reference only the table content container
                        val contentContainer = mainLayout.findViewWithTag<ViewGroup>("table_content")
                        refreshTableContent(contentContainer, annotation, isChecked, tableData.tracking)
                    }
                }

                val trackingToggle = androidx.appcompat.widget.SwitchCompat(container.context).apply {
                    text = "Track"
                    isChecked = tableData.tracking
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    setOnCheckedChangeListener { _, isChecked ->
                        updateTableTracking(annotation, isChecked)
                    }
                }

                headerLayout.addView(titleView)
                headerLayout.addView(editToggle)
                headerLayout.addView(trackingToggle)
                mainLayout.addView(headerLayout)

                // Table content container - this is what we'll refresh
                val tableContainer = LinearLayout(container.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    tag = "table_content"
                }
                mainLayout.addView(tableContainer)

                refreshTableContent(tableContainer, annotation, isEditMode, tableData.tracking)

            } catch (e: Exception) {
                val errorText = TextView(container.context).apply {
                    text = "Error loading table: ${e.message}"
                    setTextColor(Color.RED)
                }
                container.addView(errorText)
            }
        }

        private fun refreshTableContent(container: ViewGroup, annotation: Annotation, isEditMode: Boolean, isTrackingMode: Boolean) {

            container.removeAllViews()

            try {
                if (annotation.annotation == null || annotation.annotation.isBlank()) {
                    val errorText = TextView(container.context).apply {
                        text = "No table data available"
                        setTextColor(Color.RED)
                    }
                    container.addView(errorText)
                    return
                }
                val tableData = parseTableData(annotation.annotation)

                for (i in 0 until tableData.nRow) {
                    val rowLayout = LinearLayout(container.context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    for (j in 0 until tableData.nCol) {
                        val cellKey = "$i,$j"
                        val isHighlighted = tableData.trackingMap[cellKey] == true
                        val cellContent = tableData.content[i][j]

                        if (isEditMode) {
                            val editText = EditText(container.context).apply {
                                setText(cellContent)
                                gravity = android.view.Gravity.CENTER
                                layoutParams = LinearLayout.LayoutParams(100, 50).apply {
                                    setMargins(1, 1, 1, 1)
                                }
                                background = getTableCellBackground(isHighlighted)
                                setTextColor(if (isHighlighted) Color.WHITE else Color.BLACK)

                                setOnFocusChangeListener { _, hasFocus ->
                                    if (!hasFocus) {
                                        updateTableCellContent(annotation, i, j, text.toString())
                                    }
                                }

                                if (isTrackingMode) {
                                    setOnClickListener {
                                        updateTableCellState(annotation, i, j, !isHighlighted)
                                    }
                                }
                            }
                            rowLayout.addView(editText)
                        } else {
                            val cell = TextView(container.context).apply {
                                text = cellContent
                                gravity = android.view.Gravity.CENTER
                                layoutParams = LinearLayout.LayoutParams(100, 50).apply {
                                    setMargins(1, 1, 1, 1)
                                }
                                background = getTableCellBackground(isHighlighted)
                                setTextColor(if (isHighlighted) Color.WHITE else Color.BLACK)

                                if (isTrackingMode) {
                                    setOnClickListener {
                                        updateTableCellState(annotation, i, j, !isHighlighted)
                                    }
                                }
                            }
                            rowLayout.addView(cell)
                        }
                    }
                    container.addView(rowLayout)
                }
            } catch (e: Exception) {
                val errorText = TextView(container.context).apply {
                    text = "Error refreshing table: ${e.message}"
                    setTextColor(Color.RED)
                }
                container.addView(errorText)
            }
        }

        private fun updateTableTracking(annotation: Annotation, isTracking: Boolean) {
            try {
                if (annotation.annotation == null || annotation.annotation.isBlank()) {
                    Toast.makeText(
                        itemView.context,
                        "No table data available to update",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val json = org.json.JSONObject(annotation.annotation)
                json.put("tracking", isTracking)

                val updatedAnnotation = annotation.copy(annotation = json.toString())
                onAnnotationUpdate(updatedAnnotation, null, null)
            } catch (e: Exception) {
                Toast.makeText(
                    itemView.context,
                    "Error updating table tracking: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        private fun updateTableCellContent(annotation: Annotation, row: Int, col: Int, newContent: String) {
            try {
                if (annotation.annotation == null || annotation.annotation.isBlank()) {
                    Toast.makeText(
                        itemView.context,
                        "No table data available to update",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val json = org.json.JSONObject(annotation.annotation)
                val contentArray = json.getJSONArray("content")
                val rowArray = contentArray.getJSONArray(row)
                rowArray.put(col, newContent)

                val updatedAnnotation = annotation.copy(annotation = json.toString())
                onAnnotationUpdate(updatedAnnotation, null, null)
            } catch (e: Exception) {
                Toast.makeText(
                    itemView.context,
                    "Error updating table content: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        private fun getTableCellBackground(isHighlighted: Boolean): android.graphics.drawable.GradientDrawable {
            return android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(if (isHighlighted) Color.parseColor("#0d6efd") else Color.WHITE)
                setStroke(1, Color.GRAY)
            }
        }

        private fun updateTableCellState(annotation: Annotation, row: Int, col: Int, highlighted: Boolean) {
            try {
                if (annotation.annotation == null || annotation.annotation.isBlank()) {
                    Toast.makeText(
                        itemView.context,
                        "No table data available to update",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val cellKey = "$row,$col"

                val json = org.json.JSONObject(annotation.annotation)
                val trackingMapJson = json.getJSONObject("trackingMap")
                trackingMapJson.put(cellKey, highlighted)

                val updatedAnnotation = annotation.copy(annotation = json.toString())
                onAnnotationUpdate(updatedAnnotation, null, null)
            } catch (e: Exception) {
                Toast.makeText(
                    itemView.context,
                    "Error updating table: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun parseTableData(jsonString: String): TableData {
        return try {
            val json = org.json.JSONObject(jsonString)
            val name = json.getString("name")
            val nRow = json.getInt("nRow")
            val nCol = json.getInt("nCol")
            val tracking = json.getBoolean("tracking")

            // Parse content 2D array
            val contentArray = json.getJSONArray("content")
            val content = mutableListOf<List<String>>()

            for (i in 0 until nRow) {
                val row = mutableListOf<String>()
                val jsonRow = contentArray.getJSONArray(i)

                for (j in 0 until nCol) {
                    val cellValue = jsonRow.getString(j)
                    row.add(cellValue)
                }

                content.add(row)
            }

            // Parse tracking map
            val trackingMapJson = json.getJSONObject("trackingMap")
            val trackingMap = mutableMapOf<String, Boolean>()

            trackingMapJson.keys().forEach { key ->
                trackingMap[key] = trackingMapJson.getBoolean(key)
            }

            TableData(name, nRow, nCol, content, tracking, trackingMap)
        } catch (e: Exception) {
            throw Exception("Invalid table format: ${e.message}")
        }
    }



    private fun parseCounterData(jsonString: String): CounterData {
        return try {
            val json = org.json.JSONObject(jsonString)
            CounterData(
                name = json.getString("name"),
                total = json.getInt("total"),
                current = json.getInt("current")
            )
        } catch (e: Exception) {
            throw Exception("Invalid counter format: ${e.message}")
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
                    downloadAnnotationFile(annotation)
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

    private fun requestSignedUrlAndPlay(
        annotation: Annotation,
        seekBar: SeekBar?,
        timerText: TextView?
    ) {
        // Release previous media player if any
        releaseMediaPlayer()

        // Request signed URL in a coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = annotationRepository.getSignedUrl(annotation.id)

                if (result.isSuccess) {
                    val signedToken = result.getOrNull()?.signedToken
                    if (signedToken != null) {
                        val signedUrl = "${baseUrl}/api/annotation/download_signed/?token=$signedToken"

                        withContext(Dispatchers.Main) {
                            prepareAndPlayMedia(signedUrl, annotation, seekBar, timerText)
                        }
                    } else {
                        showPlaybackError("Failed to get signed URL")
                    }
                } else {
                    showPlaybackError("Error: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showPlaybackError("Error: ${e.message}")
            }
        }
    }

    private fun prepareAndPlayMedia(
        url: String,
        annotation: Annotation,
        initialSeekBar: SeekBar?,
        initialTimerText: TextView?
    ) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                prepareAsync()

                setOnPreparedListener { preparedMediaPlayer ->
                    Log.d("SessionAnnotationAdapter", "Media prepared for duration: $duration")

                    initialSeekBar?.max = 0
                    initialSeekBar?.visibility = View.VISIBLE
                    initialTimerText?.visibility = View.VISIBLE

                    currentMaxPositionReached = 0

                    this@SessionAnnotationAdapter.currentPlayingAnnotationId = annotation.id
                    notifyDataSetChanged()

                    preparedMediaPlayer.start()
                    mediaPlayerUpdateHandler?.removeCallbacks(mediaPlayerUpdateRunnable!!)

                    mediaPlayerUpdateHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mediaPlayerUpdateRunnable = object : Runnable {
                        override fun run() {
                            if (preparedMediaPlayer == this@SessionAnnotationAdapter.mediaPlayer &&
                                annotation.id == currentPlayingAnnotationId &&
                                this@SessionAnnotationAdapter.mediaPlayer != null) {

                                val mp = this@SessionAnnotationAdapter.mediaPlayer!!
                                val currentPosition = mp.currentPosition

                                val currentViewHolder = findViewHolderForAnnotationId(annotation.id)
                                currentViewHolder?.let { holder ->
                                    val seekBarView = holder.itemView.findViewById<SeekBar>(R.id.mediaSeekBar)
                                    val timerTextView = holder.itemView.findViewById<TextView>(R.id.mediaTimerText)

                                    if (mp.isPlaying) {
                                        if (currentPosition > currentMaxPositionReached) {
                                            currentMaxPositionReached = currentPosition
                                        }
                                    }
                                    seekBarView?.max = currentMaxPositionReached
                                    seekBarView?.progress = currentPosition

                                    val currentPosStr = formatTime(currentPosition)
                                    val mediaDuration = mp.duration
                                    if (mediaDuration <= 0) {
                                        timerTextView?.text = "Position: $currentPosStr"
                                    } else {
                                        val durationStr = formatTime(mediaDuration)
                                        timerTextView?.text = "$currentPosStr / $durationStr"
                                    }
                                    holder.highlightCurrentTranscriptSection(currentPosition)
                                }

                                if (annotation.id == currentPlayingAnnotationId) {
                                    mediaPlayerUpdateHandler?.postDelayed(this, 100)
                                }
                            }
                        }
                    }
                    mediaPlayerUpdateHandler?.post(mediaPlayerUpdateRunnable!!)
                }

                setOnCompletionListener { mp ->
                    if (mp == this@SessionAnnotationAdapter.mediaPlayer) {
                        mediaPlayerUpdateHandler?.removeCallbacks(mediaPlayerUpdateRunnable!!)
                        notifyDataSetChanged()
                    }
                }

                setOnErrorListener { mp, _, _ ->
                    if (mp == this@SessionAnnotationAdapter.mediaPlayer) {
                        showPlaybackError("Failed to play media")
                    }
                    true
                }
            }
        } catch (e: Exception) {
            showPlaybackError("Error setting up media player: ${e.message}")
        }
    }


    private fun updateTimerText(timerText: TextView?) {
        mediaPlayer?.let { player ->
            val currentPos = formatTime(player.currentPosition)

            if (player.duration <= 0) {
                timerText?.text = "Position: $currentPos"
            } else {
                val duration = formatTime(player.duration)
                timerText?.text = "$currentPos / $duration"
            }
        }
    }

    private fun formatTime(ms: Int): String {
        val seconds = (ms / 1000) % 60
        val minutes = ms / 60000
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun releaseMediaPlayer() {
        mediaPlayerUpdateHandler?.removeCallbacks(mediaPlayerUpdateRunnable!!)
        mediaPlayerUpdateHandler = null
        mediaPlayerUpdateRunnable = null

        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    private fun showPlaybackError(message: String) {
        // Get context from the RecyclerView
        val context = recyclerView?.context
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadAnnotationFile(annotation: Annotation) {
        val context = recyclerView?.context ?: return

        Toast.makeText(context, "Preparing download...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = annotationRepository.getSignedUrl(annotation.id)

                if (result.isSuccess) {
                    val signedToken = result.getOrNull()?.signedToken
                    if (signedToken != null) {
                        val signedUrl = "${baseUrl}/api/annotation/download_signed/?token=$signedToken"

                        withContext(Dispatchers.Main) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(signedUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "No browser found to download file", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        showPlaybackError("Failed to get download link")
                    }
                } else {
                    showPlaybackError("Error: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showPlaybackError("Error: ${e.message}")
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        releaseMediaPlayer()
        this.recyclerView = null
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    private fun parseVttContent(vttContent: String): List<VttCue> {
        val cues = mutableListOf<VttCue>()
        val lines = vttContent.split("\n")

        var i = 0
        while (i < lines.size && !lines[i].contains("-->")) i++

        while (i < lines.size) {
            if (lines[i].contains("-->")) {
                val timeLine = lines[i]
                val timesParts = timeLine.split("-->")
                if (timesParts.size == 2) {
                    val startTime = parseVttTime(timesParts[0].trim())
                    val endTime = parseVttTime(timesParts[1].trim())

                    val textBuilder = StringBuilder()
                    i++
                    while (i < lines.size && lines[i].isNotBlank() && !lines[i].contains("-->")) {
                        if (textBuilder.isNotEmpty()) textBuilder.append(" ")
                        textBuilder.append(lines[i])
                        i++
                    }

                    if (textBuilder.isNotEmpty()) {
                        cues.add(VttCue(startTime, endTime, textBuilder.toString()))
                    }
                } else {
                    i++
                }
            } else {
                i++
            }
        }

        return cues
    }

    private fun parseVttTime(timeString: String): Long {
        val parts = timeString.split(":", ".")
        return when (parts.size) {
            3 -> {
                val minutes = parts[0].toLong() * 60 * 1000
                val seconds = parts[1].toLong() * 1000
                val millis = parts[2].toLong()
                minutes + seconds + millis
            }
            4 -> {
                val hours = parts[0].toLong() * 60 * 60 * 1000
                val minutes = parts[1].toLong() * 60 * 1000
                val seconds = parts[2].toLong() * 1000
                val millis = parts[3].toLong()
                hours + minutes + seconds + millis
            }
            else -> 0L
        }
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

    private fun findViewHolderForAnnotationId(annotationId: Int): ViewHolder? {
        recyclerView?.let { rv ->
            for (i in 0 until rv.childCount) {
                val child = rv.getChildAt(i) ?: continue
                val holder = rv.getChildViewHolder(child) as? ViewHolder
                if (holder != null && holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    if (getItem(holder.bindingAdapterPosition).id == annotationId) {
                        return holder
                    }
                }
            }
        }
        return null
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