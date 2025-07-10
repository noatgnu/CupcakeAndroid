package info.proteo.cupcake.ui.session

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R

class TranscriptionTabAdapter(
    private val transcription: String?,
    private val translation: String?
) : RecyclerView.Adapter<TranscriptionTabAdapter.TabViewHolder>() {
    
    private fun parseVttContent(vttContent: String): List<VttCue> {
        val cues = mutableListOf<VttCue>()
        val lines = vttContent.split(Regex("\r\n|\n|\r"))

        var i = 0
        while (i < lines.size && lines[i].trim().isEmpty() || lines[i].trim().equals("WEBVTT", ignoreCase = true) || lines[i].startsWith("Kind:") || lines[i].startsWith("Language:")) {
            i++
        }

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.contains("-->")) {
                val timeLine = line
                val timesParts = timeLine.split("-->")
                if (timesParts.size == 2) {
                    val startTime = parseVttTime(timesParts[0].trim())
                    val endTimeString = timesParts[1].trim().split(Regex("\\s+"))[0]
                    val endTime = parseVttTime(endTimeString)

                    val textBuilder = StringBuilder()
                    i++
                    while (i < lines.size && lines[i].trim().isNotEmpty() && !lines[i].contains("-->")) {
                        if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                        textBuilder.append(lines[i].trim())
                        i++
                    }
                    if (textBuilder.isNotEmpty()) {
                        cues.add(VttCue(startTime, endTime, textBuilder.toString()))
                    }
                   continue
                }
            }
            i++
        }
        return cues
    }

    private fun parseVttTime(timeString: String): Long {
        val parts = timeString.split(":", ".")
        return try {
            when (parts.size) {
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
        } catch (e: NumberFormatException) {
            0L
        }
    }

    private fun buildFullTranscriptText(vttCues: List<VttCue>): String {
        return vttCues.joinToString("\n") { it.text }
    }

    data class VttCue(val startTime: Long, val endTime: Long, val text: String)

    private val tabs = mutableListOf<TabContent>()

    init {
        if (!transcription.isNullOrBlank()) {
            val transcriptionCues = parseVttContent(transcription)
            val transcriptionText = buildFullTranscriptText(transcriptionCues)
            tabs.add(TabContent("Transcription", transcriptionText, R.layout.tab_transcription_content, R.id.transcription_text))
        }
        
        if (!translation.isNullOrBlank()) {
            val translationCues = parseVttContent(translation)
            val translationText = buildFullTranscriptText(translationCues)
            tabs.add(TabContent("Translation", translationText, R.layout.tab_translation_content, R.id.translation_text))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val tab = tabs[viewType]
        val view = LayoutInflater.from(parent.context).inflate(tab.layoutRes, parent, false)
        return TabViewHolder(view, tab.textViewId)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]
        holder.bind(tab.content)
    }

    override fun getItemCount(): Int = tabs.size

    override fun getItemViewType(position: Int): Int = position

    fun getTabTitle(position: Int): String = tabs[position].title

    fun hasTranscription(): Boolean = transcription != null && transcription.isNotBlank()
    fun hasTranslation(): Boolean = translation != null && translation.isNotBlank()

    class TabViewHolder(itemView: View, private val textViewId: Int) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(textViewId)

        fun bind(content: String) {
            textView.text = content
        }
    }

    private data class TabContent(
        val title: String,
        val content: String,
        val layoutRes: Int,
        val textViewId: Int
    )
}