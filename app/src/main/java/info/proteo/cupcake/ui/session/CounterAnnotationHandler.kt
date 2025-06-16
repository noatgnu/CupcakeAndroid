package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import info.proteo.cupcake.shared.data.model.annotation.Annotation
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
            if (annotation.annotation == null || annotation.annotation!!.isBlank()) {
                val errorText = TextView(container.context).apply {
                    text = "No counter data available"
                    setTextColor(Color.RED)
                }
                container.addView(errorText)
                return
            }

            val counterData = parseCounterData(annotation.annotation!!)

            val mainLayout = LinearLayout(container.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            container.addView(mainLayout)

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
            mainLayout.addView(titleView)

            // Counter display row
            val counterRow = LinearLayout(container.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8
                }
            }
            mainLayout.addView(counterRow)

            // Decrement button
            val decrementButton = Button(container.context).apply {
                text = "-"
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnClickListener {
                    updateCounter(annotation, counterData.current - 1, counterData.total)
                }
                isEnabled = counterData.current > 0
            }
            counterRow.addView(decrementButton)

            // Counter display
            val counterDisplay = TextView(container.context).apply {
                text = "${counterData.current}/${counterData.total}"
                setTypeface(null, Typeface.BOLD)
                textSize = 18f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    2f
                )
            }
            counterRow.addView(counterDisplay)

            // Increment button
            val incrementButton = Button(container.context).apply {
                text = "+"
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnClickListener {
                    updateCounter(annotation, counterData.current + 1, counterData.total)
                }
                isEnabled = counterData.current < counterData.total
            }
            counterRow.addView(incrementButton)

            // Progress bar
            val progressLayout = LinearLayout(container.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    20 // Fixed height for progress bar
                )
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    setColor(Color.LTGRAY)
                    cornerRadius = 5f
                }
            }
            mainLayout.addView(progressLayout)

            if (counterData.total > 0) {
                val progress = LinearLayout(container.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val progressWidth = (counterData.current.toFloat() / counterData.total.toFloat() * 100).toInt()
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        progressWidth.toFloat()
                    )
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        setColor(Color.parseColor("#0d6efd"))
                        cornerRadius = 5f
                    }
                }
                progressLayout.addView(progress)

                // Add empty space for remaining progress
                val remaining = LinearLayout(container.context).apply {
                    val remainingWidth = 100 - (counterData.current.toFloat() / counterData.total.toFloat() * 100).toInt()
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        remainingWidth.toFloat()
                    )
                }
                progressLayout.addView(remaining)
            }

        } catch (e: Exception) {
            val errorText = TextView(container.context).apply {
                text = "Error loading counter: ${e.message}"
                setTextColor(Color.RED)
            }
            container.addView(errorText)
        }
    }

    private fun updateCounter(annotation: Annotation, newCurrent: Int, total: Int) {
        try {
            if (annotation.annotation == null || annotation.annotation!!.isBlank()) {
                Toast.makeText(
                    context,
                    "No counter data available to update",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Ensure the new current value is within bounds
            val validatedCurrent = newCurrent.coerceIn(0, total)

            val json = JSONObject(annotation.annotation)
            json.put("current", validatedCurrent)

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