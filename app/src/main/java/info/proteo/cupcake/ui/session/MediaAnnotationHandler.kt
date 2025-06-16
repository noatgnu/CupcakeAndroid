package info.proteo.cupcake.ui.session
import android.content.Context
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import info.proteo.cupcake.R
import info.proteo.cupcake.data.repository.AnnotationRepository
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class MediaAnnotationHandler(
    private val context: Context,
    private val annotationRepository: AnnotationRepository,
    private val baseUrl: String,
    private val onAnnotationUpdate: (Annotation, String?, String?) -> Unit
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingAnnotationId: Int? = null
    private var mediaPlayerUpdateHandler: Handler? = null
    private var mediaPlayerUpdateRunnable: Runnable? = null
    private var currentVttCues: List<VttCue> = emptyList()

    private var onMediaStateChanged: (() -> Unit)? = null

    fun setOnMediaStateChangedListener(listener: () -> Unit) {
        this.onMediaStateChanged = listener
    }

    fun displayMedia(
        annotation: Annotation,
        playButton: ImageButton?,
        mediaSeekBar: SeekBar?,
        mediaTimerText: TextView?,
        progressBar: ProgressBar?,
        transcriptionContainer: View,
        transcriptionText: TextView
    ) {
        if (annotation.id == currentPlayingAnnotationId && mediaPlayer != null) {
            val mp = mediaPlayer!!
            playButton?.setImageResource(if (mp.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)
            mediaSeekBar?.visibility = View.VISIBLE
            mediaTimerText?.visibility = View.VISIBLE
            progressBar?.visibility = View.GONE

            val currentMediaPlayerDuration = mp.duration
            val knownSeekBarMax = mediaSeekBar?.max ?: 0
            // Use media player duration if valid, otherwise use the greater of current seekbar max or current position
            val actualDuration = if (currentMediaPlayerDuration > 0) currentMediaPlayerDuration else max(knownSeekBarMax, mp.currentPosition)

            mediaSeekBar?.max = actualDuration
            mediaSeekBar?.progress = mp.currentPosition
            updateTimerText(mediaTimerText, mp.currentPosition, actualDuration)
        } else {
            playButton?.setImageResource(R.drawable.ic_play_arrow)
            mediaSeekBar?.visibility = View.GONE
            mediaTimerText?.visibility = View.GONE
            progressBar?.visibility = View.GONE
            // Reset cues if it's a new media item or no media is playing
            currentVttCues = emptyList()
        }

        if (!annotation.transcription.isNullOrBlank()) {
            transcriptionContainer.visibility = View.VISIBLE
            // Parse VTT cues only once when media is set up or changed
            if (currentVttCues.isEmpty() || annotation.id != currentPlayingAnnotationId) {
                currentVttCues = parseVttContent(annotation.transcription!!)
            }
            transcriptionText.text = buildFullTranscriptText(currentVttCues)
            // Initial highlight if media is already playing for this annotation
            mediaPlayer?.let { mp ->
                if (mp.isPlaying && annotation.id == currentPlayingAnnotationId) {
                    highlightCurrentTranscriptSection(mp.currentPosition, currentVttCues, transcriptionText)
                }
            }
        } else {
            transcriptionContainer.visibility = View.GONE
            currentVttCues = emptyList()
        }

        playButton?.setOnClickListener {
            if (annotation.id == currentPlayingAnnotationId && mediaPlayer != null) {
                val mp = mediaPlayer!!
                if (mp.isPlaying) {
                    mp.pause()
                    playButton.setImageResource(R.drawable.ic_play_arrow)
                    mediaPlayerUpdateHandler?.removeCallbacks(mediaPlayerUpdateRunnable!!)
                } else { // Was paused or completed
                    // If completed (or very near end), seek to 0 before starting
                    val durationThreshold = if (mp.duration > 0) mp.duration else (mediaSeekBar?.max ?: 0)
                    if (mp.currentPosition >= durationThreshold - 100 && durationThreshold > 0) { // 100ms tolerance
                        mp.seekTo(0)
                        mediaSeekBar?.progress = 0
                        updateTimerText(mediaTimerText, 0, durationThreshold)
                    }
                    mp.start()
                    playButton.setImageResource(R.drawable.ic_pause)
                    if (mediaPlayerUpdateRunnable != null) { // Ensure runnable is initialized
                        mediaPlayerUpdateHandler?.post(mediaPlayerUpdateRunnable!!)
                    }
                }
            } else {
                progressBar?.visibility = View.VISIBLE
                playButton.isEnabled = false
                // Parse VTT cues before requesting URL, as they are needed in prepareAndPlayMedia
                currentVttCues = if (!annotation.transcription.isNullOrBlank()) parseVttContent(annotation.transcription!!) else emptyList()
                requestSignedUrlAndPlay(annotation, playButton, mediaSeekBar, mediaTimerText, progressBar, transcriptionText, currentVttCues)
            }
            onMediaStateChanged?.invoke()
        }

        mediaSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && annotation.id == currentPlayingAnnotationId && mediaPlayer != null) {
                    mediaPlayer!!.seekTo(progress)
                    updateTimerText(mediaTimerText, mediaPlayer!!.currentPosition, sBar?.max ?: mediaPlayer!!.duration)
                    highlightCurrentTranscriptSection(mediaPlayer!!.currentPosition, currentVttCues, transcriptionText)
                }
            }

            override fun onStartTrackingTouch(sBar: SeekBar?) {
                if (annotation.id == currentPlayingAnnotationId && mediaPlayer?.isPlaying == true) {
                    mediaPlayerUpdateHandler?.removeCallbacks(mediaPlayerUpdateRunnable!!)
                }
            }

            override fun onStopTrackingTouch(sBar: SeekBar?) {
                if (annotation.id == currentPlayingAnnotationId && mediaPlayer != null) {
                    // If was playing, resume handler
                    if (mediaPlayer!!.isPlaying && mediaPlayerUpdateRunnable != null && mediaPlayerUpdateHandler != null) {
                        mediaPlayerUpdateHandler?.post(mediaPlayerUpdateRunnable!!)
                    }
                    // Update timer text with potentially new max from seekbar if duration was unknown
                    updateTimerText(mediaTimerText, mediaPlayer!!.currentPosition, sBar?.max ?: mediaPlayer!!.duration)
                }
            }
        })
    }

    private fun requestSignedUrlAndPlay(
        annotation: Annotation,
        playButton: ImageButton?,
        seekBar: SeekBar?,
        timerText: TextView?,
        progressBar: ProgressBar?,
        transcriptionTextView: TextView?,
        vttCues: List<VttCue> // Pass parsed VTT cues
    ) {
        releaseMediaPlayer() // Releases old player and clears currentPlayingAnnotationId

        progressBar?.visibility = View.VISIBLE
        playButton?.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = annotationRepository.getSignedUrl(annotation.id)
                if (result.isSuccess) {
                    val signedToken = result.getOrNull()?.signedToken
                    if (signedToken != null) {
                        val signedUrl = "${baseUrl}/api/annotation/download_signed/?token=$signedToken"
                        withContext(Dispatchers.Main) {
                            prepareAndPlayMedia(signedUrl, annotation, playButton, seekBar, timerText, progressBar, transcriptionTextView, vttCues)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showPlaybackError("Failed to get signed URL")
                            progressBar?.visibility = View.GONE
                            playButton?.isEnabled = true
                            onMediaStateChanged?.invoke()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showPlaybackError("Error: ${result.exceptionOrNull()?.message}")
                        progressBar?.visibility = View.GONE
                        playButton?.isEnabled = true
                        onMediaStateChanged?.invoke()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showPlaybackError("Error: ${e.message}")
                    progressBar?.visibility = View.GONE
                    playButton?.isEnabled = true
                    onMediaStateChanged?.invoke()
                }
            }
        }
    }

    private fun prepareAndPlayMedia(
        url: String,
        annotation: Annotation,
        playButton: ImageButton?,
        seekBar: SeekBar?,
        timerText: TextView?,
        progressBar: ProgressBar?,
        transcriptionTextView: TextView?,
        vttCues: List<VttCue> // Use passed VTT cues
    ) {
        this.currentVttCues = vttCues // Store for use in updater
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
                    Log.d("MediaAnnotationHandler", "Media prepared. Reported duration: ${preparedMediaPlayer.duration}")
                    progressBar?.visibility = View.GONE
                    playButton?.isEnabled = true
                    playButton?.setImageResource(R.drawable.ic_pause)

                    val initialDuration = preparedMediaPlayer.duration
                    // If duration is 0 or less, it's likely unknown (streaming). Start with 0 or a small value.
                    // The seekbar max will be dynamically updated.
                    seekBar?.max = if (initialDuration > 0) initialDuration else 0
                    seekBar?.progress = 0
                    seekBar?.visibility = View.VISIBLE
                    timerText?.visibility = View.VISIBLE
                    updateTimerText(timerText, 0, seekBar?.max ?: 0)

                    this@MediaAnnotationHandler.currentPlayingAnnotationId = annotation.id
                    preparedMediaPlayer.start()

                    mediaPlayerUpdateHandler?.removeCallbacks(mediaPlayerUpdateRunnable!!) // Clear existing
                    mediaPlayerUpdateHandler = Handler(Looper.getMainLooper())
                    mediaPlayerUpdateRunnable = object : Runnable {
                        override fun run() {
                            if (this@MediaAnnotationHandler.mediaPlayer == preparedMediaPlayer &&
                                annotation.id == currentPlayingAnnotationId &&
                                preparedMediaPlayer.isPlaying) {

                                val mp = this@MediaAnnotationHandler.mediaPlayer!!
                                val currentPosition = mp.currentPosition
                                val currentMediaPlayerDuration = mp.duration
                                val currentSeekBarMax = seekBar?.max ?: 0

                                val newSeekBarMax = if (currentMediaPlayerDuration > 0) {
                                    currentMediaPlayerDuration
                                } else {
                                    max(currentSeekBarMax, currentPosition) // Dynamically extend seekbar max
                                }

                                seekBar?.max = newSeekBarMax
                                seekBar?.progress = currentPosition
                                updateTimerText(timerText, currentPosition, newSeekBarMax)

                                if (transcriptionTextView != null && this@MediaAnnotationHandler.currentVttCues.isNotEmpty()) {
                                    highlightCurrentTranscriptSection(currentPosition, this@MediaAnnotationHandler.currentVttCues, transcriptionTextView)
                                }
                                mediaPlayerUpdateHandler?.postDelayed(this, 250) // Update interval
                            }
                        }
                    }
                    mediaPlayerUpdateHandler?.post(mediaPlayerUpdateRunnable!!)
                    onMediaStateChanged?.invoke() // Notify adapter about new media playing
                }

                setOnCompletionListener { mp ->
                    if (mp == this@MediaAnnotationHandler.mediaPlayer && annotation.id == currentPlayingAnnotationId) {
                        Log.d("MediaAnnotationHandler", "Media completed.")
                        mediaPlayerUpdateHandler?.removeCallbacks(mediaPlayerUpdateRunnable!!)

                        val finalPosition = mp.currentPosition
                        val finalMediaPlayerDuration = mp.duration
                        val currentSeekBarMax = seekBar?.max ?: 0
                        val newSeekBarMax = if (finalMediaPlayerDuration > 0) finalMediaPlayerDuration else max(currentSeekBarMax, finalPosition)

                        seekBar?.max = newSeekBarMax
                        seekBar?.progress = newSeekBarMax
                        updateTimerText(timerText, newSeekBarMax, newSeekBarMax)


                        playButton?.setImageResource(R.drawable.ic_play_arrow)
                        onMediaStateChanged?.invoke()
                    }
                }

                setOnErrorListener { mp, what, extra ->
                    Log.e("MediaAnnotationHandler", "MediaPlayer Error: what $what, extra $extra")
                    if (mp == this@MediaAnnotationHandler.mediaPlayer) {
                        showPlaybackError("Failed to play media (error $what, $extra)")
                        progressBar?.visibility = View.GONE
                        playButton?.isEnabled = true
                        playButton?.setImageResource(R.drawable.ic_play_arrow)
                        releaseMediaPlayer()
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("MediaAnnotationHandler", "Error setting up media player", e)
            showPlaybackError("Error setting up media player: ${e.message}")
            progressBar?.visibility = View.GONE
            playButton?.isEnabled = true
            playButton?.setImageResource(R.drawable.ic_play_arrow)
            releaseMediaPlayer()
        }
    }

    fun highlightCurrentTranscriptSection(currentPositionMs: Int, vttCues: List<VttCue>, transcriptionText: TextView) {
        if (vttCues.isEmpty() || transcriptionText.text.isEmpty()) return

        val fullText = transcriptionText.text.toString()
        val spannableString = SpannableString(fullText)

        spannableString.getSpans(0, fullText.length, BackgroundColorSpan::class.java).forEach { spannableString.removeSpan(it) }
        spannableString.getSpans(0, fullText.length, StyleSpan::class.java).forEach { spannableString.removeSpan(it) }

        val currentCue = vttCues.find {
            currentPositionMs >= it.startTime && currentPositionMs < it.endTime
        }

        currentCue?.let { cue ->
            var startIndex = -1
            val lines = fullText.split("\n")
            var currentOffset = 0
            for (line in lines) {
                if (line == cue.text) {
                    startIndex = currentOffset
                    break
                }
                currentOffset += line.length + 1
            }

            if (startIndex >= 0) {
                val endIndex = startIndex + cue.text.length
                if (endIndex <= fullText.length) {
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
        }
        transcriptionText.text = spannableString

        currentCue?.let { cue ->
            val layout = transcriptionText.layout ?: return
            var scrollStartIndex = -1
            val lines = transcriptionText.text.toString().split("\n")
            var currentOffset = 0
            for (line in lines) {
                if (line == cue.text) {
                    scrollStartIndex = currentOffset
                    break
                }
                currentOffset += line.length + 1
            }

            if (scrollStartIndex >= 0) {
                val lineStart = layout.getLineForOffset(scrollStartIndex)
                val lineTop = layout.getLineTop(lineStart)
                val parentView = transcriptionText.parent as? View
                val visibleHeight = parentView?.height ?: 0
                val scrollY = lineTop - (visibleHeight / 2) + (transcriptionText.lineHeight / 2) // Center the line
                parentView?.scrollTo(0, scrollY.coerceAtLeast(0))
            }
        }
    }


    private fun updateTimerText(timerText: TextView?, currentPositionMs: Int, durationMs: Int) {
        timerText?.let {
            val currentPosStr = formatTime(currentPositionMs)
            if (durationMs <= 0) {
                it.text = context.getString(R.string.media_player_position_text, currentPosStr)
            } else {
                val durationStr = formatTime(durationMs)
                it.text = context.getString(R.string.media_player_progress_text, currentPosStr, durationStr)
            }
        }
    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60
        return String.format("%d:%02d", minutes, seconds)
    }

    fun releaseMediaPlayer() {
        mediaPlayerUpdateHandler?.removeCallbacks(mediaPlayerUpdateRunnable!!)
        mediaPlayerUpdateHandler = null
        mediaPlayerUpdateRunnable = null

        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                release()
            } catch (e: IllegalStateException) {
                Log.w("MediaAnnotationHandler", "IllegalStateException during media player release: ${e.message}")
            }
        }
        mediaPlayer = null
        val oldPlayingId = currentPlayingAnnotationId
        currentPlayingAnnotationId = null
        currentVttCues = emptyList()

        if (oldPlayingId != null) {
            onMediaStateChanged?.invoke()
        }
    }

    private fun showPlaybackError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun buildFullTranscriptText(vttCues: List<VttCue>): SpannableString {
        val fullText = vttCues.joinToString("\n") { it.text }
        return SpannableString(fullText)
    }

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
                3 -> { // mm:ss.ttt
                    val minutes = parts[0].toLong() * 60 * 1000
                    val seconds = parts[1].toLong() * 1000
                    val millis = parts[2].toLong()
                    minutes + seconds + millis
                }
                4 -> { // hh:mm:ss.ttt
                    val hours = parts[0].toLong() * 60 * 60 * 1000
                    val minutes = parts[1].toLong() * 60 * 1000
                    val seconds = parts[2].toLong() * 1000
                    val millis = parts[3].toLong()
                    hours + minutes + seconds + millis
                }
                else -> 0L
            }
        } catch (e: NumberFormatException) {
            Log.e("MediaAnnotationHandler", "Error parsing VTT time: $timeString", e)
            0L
        }
    }

    fun getCurrentPlayingAnnotationId(): Int? = currentPlayingAnnotationId

    data class VttCue(val startTime: Long, val endTime: Long, val text: String)
}