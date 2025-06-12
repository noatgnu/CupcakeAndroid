package info.proteo.cupcake.ui.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.data.local.dao.protocol.RecentSessionDao
import info.proteo.cupcake.data.remote.service.CreateAnnotationRequest
import info.proteo.cupcake.data.repository.AnnotationRepository
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class VoiceAnnotationActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var recentSessionDao: RecentSessionDao

    @Inject
    lateinit var annotationRepository: AnnotationRepository

    private var audioFile: File? = null
    private var audioUri: Uri? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var recordingTimer: CountDownTimer? = null

    private lateinit var statusText: TextView
    private lateinit var stopButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var timerText: TextView

    private val RECORD_AUDIO_REQUEST_CODE = 101
    private val MAX_RECORDING_TIME = 60000L



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_annotation)

        // Initialize UI components
        statusText = findViewById(R.id.statusText)
        stopButton = findViewById(R.id.stopButton)
        progressBar = findViewById(R.id.recordingProgress)
        timerText = findViewById(R.id.timerText)

        val noteText = intent.getStringExtra("text")
        val noteName = intent.getStringExtra("name")

        // Set up the stop button listener
        stopButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
                uploadAnnotation()
            }
        }
        if (!noteText.isNullOrBlank()) {
            statusText.text = "Creating text annotation..."
            progressBar.visibility = View.GONE
            timerText.visibility = View.GONE
            stopButton.text = "Close"
            createTextAnnotation(noteText, noteName)
        } else {
            checkPermissionsAndStartRecording()
        }
    }

    private fun checkPermissionsAndStartRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_REQUEST_CODE
            )
        } else {
            retrieveSessionInfoAndStartRecording()
        }
    }

    private fun createTextAnnotation(annotationText: String, annotationName: String?) {
        lifecycleScope.launch {
            try {
                // Get current user
                val currentUser = userRepository.getUserFromActivePreference()
                if (currentUser == null) {
                    showError("No active user found")
                    return@launch
                }

                // Get most recent session
                val recentSession = recentSessionDao.getMostRecentSession(currentUser.id)
                if (recentSession == null) {
                    showError("No recent session found")
                    return@launch
                }

                val sessionId = recentSession.sessionUniqueId
                val stepId = recentSession.stepId ?: return@launch showError("No step ID found")


                // Create annotation request
                val request = CreateAnnotationRequest(
                    annotation = annotationText,
                    annotationType = "text",
                    step = stepId,
                    session = sessionId,
                )

                // Upload annotation
                val result = annotationRepository.createAnnotationInRepository(request, null)
                if (result.isSuccess) {
                    statusText.text = "Text annotation created successfully"
                    if (annotationName != null) {
                        val res = result.getOrNull()
                        if (res != null) {
                            annotationRepository.renameAnnotation(res.id, annotationName)
                        }
                    }

                } else {
                    showError("Failed to create annotation: ${result.exceptionOrNull()?.message}")
                }

                // Close activity after short delay
                Handler().postDelayed({ finish() }, 2000)
            } catch (e: Exception) {
                Log.e("VoiceAnnotation", "Error creating text annotation", e)
                showError("Error: ${e.message}")
            }
        }
    }

    private fun retrieveSessionInfoAndStartRecording() {
        lifecycleScope.launch {
            statusText.text = "Preparing recording..."

            try {
                // Get current user
                val currentUser = userRepository.getUserFromActivePreference()

                if (currentUser == null) {
                    showError("No active user found")
                    return@launch
                }

                // Get most recent session for this user
                val recentSession = recentSessionDao.getMostRecentSession(currentUser.id)

                if (recentSession == null) {
                    showError("No recent session found")
                    return@launch
                }

                val sessionId = recentSession.sessionUniqueId
                val stepId = recentSession.stepId

                if (stepId == null) {
                    showError("No step ID found for recent session")
                    return@launch
                }

                // Start recording
                startRecording(sessionId, stepId)
            } catch (e: Exception) {
                Log.e("VoiceAnnotation", "Error retrieving session info", e)
                showError("Error: ${e.message}")
            }
        }
    }

    private fun startRecording(sessionId: String, stepId: Int) {
        try {
            audioFile = createAudioFile()
            audioUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                audioFile!!
            )

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            statusText.text = "Recording audio..."

            // Start timer for 1 minute
            startRecordingTimer(sessionId, stepId)

        } catch (e: Exception) {
            Log.e("VoiceAnnotation", "Error starting recording", e)
            showError("Failed to start recording: ${e.message}")
        }
    }

    private fun startRecordingTimer(sessionId: String, stepId: Int) {
        progressBar.max = (MAX_RECORDING_TIME / 1000).toInt()
        progressBar.progress = 0

        recordingTimer = object : CountDownTimer(MAX_RECORDING_TIME, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                val secondsElapsed = (MAX_RECORDING_TIME / 1000) - secondsRemaining

                progressBar.progress = secondsElapsed.toInt()
                timerText.text = "Recording: ${secondsElapsed}s / ${MAX_RECORDING_TIME / 1000}s"
            }

            override fun onFinish() {
                stopRecording()
                uploadAnnotation(sessionId, stepId)
            }
        }.start()
    }

    private fun stopRecording() {
        recordingTimer?.cancel()

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            statusText.text = "Recording completed"
            stopButton.text = "Uploading..."
            stopButton.isEnabled = false

        } catch (e: Exception) {
            Log.e("VoiceAnnotation", "Error stopping recording", e)
            showError("Error saving recording: ${e.message}")
        }
    }

    private fun uploadAnnotation(sessionId: String? = null, stepId: Int? = null) {
        lifecycleScope.launch {
            try {
                statusText.text = "Uploading audio annotation..."

                // Get session info if not provided
                var actualSessionId = sessionId
                var actualStepId = stepId

                if (actualSessionId == null || actualStepId == null) {
                    val currentUser = userRepository.getUserFromActivePreference()
                    val recentSession = currentUser?.id?.let { recentSessionDao.getMostRecentSession(it) }

                    if (recentSession == null) {
                        showError("Session information not found")
                        return@launch
                    }

                    actualSessionId = recentSession.sessionUniqueId
                    actualStepId = recentSession.stepId

                    if (actualStepId == null) {
                        showError("Step ID not found")
                        return@launch
                    }
                }

                // Create annotation request
                val request = CreateAnnotationRequest(
                    annotation = "Voice memo recorded via Google Assistant",
                    annotationType = "audio",
                    step = actualStepId,
                    session = actualSessionId
                )

                // Create file part
                val filePart = audioUri?.let { uri ->
                    val inputStream = contentResolver.openInputStream(uri)
                    val byteArray = inputStream?.readBytes()
                    inputStream?.close()

                    if (byteArray != null) {
                        val requestFile = byteArray.toRequestBody("audio/3gpp".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("file", "voice_memo.3gp", requestFile)
                    } else null
                }

                // Upload annotation
                if (filePart != null) {
                    val result = annotationRepository.createAnnotationInRepository(request, filePart)

                    if (result.isSuccess) {
                        statusText.text = "Annotation uploaded successfully"
                    } else {
                        showError("Failed to upload: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    showError("Audio file not available")
                }

            } catch (e: Exception) {
                Log.e("VoiceAnnotation", "Error uploading annotation", e)
                showError("Error: ${e.message}")
            } finally {
                // Close activity after short delay
                Handler().postDelayed({
                    finish()
                }, 2000)
            }
        }
    }

    private fun createAudioFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return File.createTempFile(
            "AUDIO_${timeStamp}_",
            ".3gp",
            storageDir
        )
    }

    private fun showError(message: String) {
        statusText.text = message
        progressBar.visibility = View.GONE
        stopButton.text = "Close"
        stopButton.isEnabled = true
        stopButton.setOnClickListener { finish() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                retrieveSessionInfoAndStartRecording()
            } else {
                showError("Microphone permission denied")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recordingTimer?.cancel()
        if (isRecording) {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                }
                release()
            }
            mediaRecorder = null
        }
    }
}