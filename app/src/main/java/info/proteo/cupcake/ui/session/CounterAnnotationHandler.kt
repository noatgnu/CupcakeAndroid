package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import org.json.JSONObject

class CounterAnnotationHandler(
    private val context: Context,
    private val onCounterUpdate: (Annotation, String?, String?) -> Unit
) {
    data class CounterData(
        val name: String,
        val total: Int,
        val current: Int
    )

    fun displayCounter(annotation: Annotation, container: ViewGroup) {
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

            // Title
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

            // Counter controls layout
            val controlsLayout = LinearLayout(container.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER_VERTICAL
            }

            // Decrement button
            val decrementButton = Button(container.context).apply {
                text = "-"
                isEnabled = counterData.current > 0
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    val newValue = (counterData.current - 1).coerceAtLeast(0)
                    updateCounterValue(annotation, newValue, counterData.total)
                }
            }

            // Counter display
            val counterDisplay = TextView(container.context).apply {
                text = "${counterData.current} / ${counterData.total}"
                textSize = 18f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = 16
                    marginEnd = 16
                }
            }

            // Increment button
            val incrementButton = Button(container.context).apply {
                text = "+"
                isEnabled = counterData.current < counterData.total
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    val newValue = (counterData.current + 1).coerceAtMost(counterData.total)
                    updateCounterValue(annotation, newValue, counterData.total)
                }
            }

            // Add views to layout
            controlsLayout.addView(decrementButton)
            controlsLayout.addView(counterDisplay)
            controlsLayout.addView(incrementButton)
            container.addView(controlsLayout)

            // Progress indicator
            val progressLayout = LinearLayout(container.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 8
                    height = 24
                }
            }

            val progressPercent = if (counterData.total > 0) {
                (counterData.current.toFloat() / counterData.total.toFloat()) * 100f
            } else {
                0f
            }

            val progressView = View(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    progressPercent
                )
                setBackgroundColor(Color.parseColor("#0d6efd"))
            }

            val remainingView = View(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    100f - progressPercent
                )
                setBackgroundColor(Color.LTGRAY)
            }

            progressLayout.addView(progressView)
            progressLayout.addView(remainingView)
            container.addView(progressLayout)

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
                    context,
                    "No counter data available to update",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val json = JSONObject(annotation.annotation)
            json.put("current", newValue)
            json.put("total", total)

            val updatedAnnotation = annotation.copy(annotation = json.toString())
            onCounterUpdate(updatedAnnotation, null, null)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error updating counter: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun parseCounterData(jsonString: String): CounterData {
        return try {
            val json = JSONObject(jsonString)
            val name = json.getString("name")
            val total = json.getInt("total")
            val current = json.getInt("current")

            CounterData(name, total, current)
        } catch (e: Exception) {
            throw Exception("Invalid counter format: ${e.message}")
        }
    }
}