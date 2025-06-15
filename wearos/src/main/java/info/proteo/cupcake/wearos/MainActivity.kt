package info.proteo.cupcake.wearos

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import info.proteo.cupcake.shared.model.TimeKeeperData
import info.proteo.cupcake.wearos.databinding.ActivityMainBinding
import info.proteo.cupcake.wearos.viewmodel.TimeKeeperViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TimeKeeperViewModel by viewModels()
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupControls()
    }

    private fun setupObservers() {
        viewModel.timeKeeper.observe(this) { timeKeeper ->
            updateTimekeeperUI(timeKeeper)
        }
    }

    private fun setupControls() {
        binding.buttonStart.setOnClickListener {
            viewModel.sendAction(ACTION_START)
        }

        binding.buttonStop.setOnClickListener {
            viewModel.sendAction(ACTION_STOP)
        }

        binding.buttonReset.setOnClickListener {
            viewModel.sendAction(ACTION_RESET)
        }
    }

    private fun updateTimekeeperUI(timeKeeper: TimeKeeperData?) {
        if (timeKeeper == null) {
            showNoTimekeeperView()
            return
        }

        // Show the TimeKeeper view
        binding.timekeeperContainer.visibility = View.VISIBLE
        binding.noTimekeeperText.visibility = View.GONE

        // Update session and step info
        binding.textSession.text = timeKeeper.sessionName ?: getString(R.string.session_format, timeKeeper.session.toString())
        binding.textStep.text = timeKeeper.stepName ?: getString(R.string.step_format, timeKeeper.step.toString())

        // Update start/stop button states
        updateButtonStates(timeKeeper.started)

        // Calculate remaining time and start countdown if needed
        if (timeKeeper.started) {
            startCountdown(timeKeeper)
        } else {
            stopCountdown()
            // Display the current duration without counting down
            val duration = timeKeeper.currentDuration ?: 0
            binding.textTimer.text = formatTime(duration * 1000L) // Convert to milliseconds
        }
    }

    private fun showNoTimekeeperView() {
        binding.timekeeperContainer.visibility = View.GONE
        binding.noTimekeeperText.visibility = View.VISIBLE
        stopCountdown()
    }

    private fun updateButtonStates(isRunning: Boolean) {
        if (isRunning) {
            binding.buttonStart.visibility = View.GONE
            binding.buttonStop.visibility = View.VISIBLE
        } else {
            binding.buttonStart.visibility = View.VISIBLE
            binding.buttonStop.visibility = View.GONE
        }
    }

    private fun startCountdown(timeKeeper: TimeKeeperData) {
        stopCountdown()

        val currentDurationMs = (timeKeeper.currentDuration ?: 0) * 1000L

        // Calculate elapsed time since the TimeKeeper was started
        val elapsedMs = calculateElapsedTimeMs(timeKeeper.startTime)

        // Calculate the remaining time
        val remainingMs = maxOf(0L, currentDurationMs - elapsedMs)

        // Start the countdown timer
        countDownTimer = object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.textTimer.text = formatTime(millisUntilFinished)
            }

            override fun onFinish() {
                binding.textTimer.text = formatTime(0)
            }
        }.start()
    }

    private fun stopCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun calculateElapsedTimeMs(startTime: String?): Long {
        if (startTime == null) return 0L

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val startDate = dateFormat.parse(startTime)
            val currentDate = Date()

            return currentDate.time - (startDate?.time ?: currentDate.time)
        } catch (e: Exception) {
            return 0L
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_RESET = "reset"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCountdown()
    }
}
