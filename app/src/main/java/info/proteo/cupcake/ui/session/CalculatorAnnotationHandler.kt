package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import org.json.JSONObject
import java.lang.reflect.Type
import java.text.DecimalFormat
import kotlin.text.get
import kotlin.toString

data class CalculatorLogEntry(
    val inputPromptFirstValue: Double,
    val inputPromptSecondValue: Double,
    val operation: String, // e.g., "+", "-", "*", "/"
    val result: Double,
    var scratch: Boolean = false
)

// Represents the state of a single calculator instance
data class CalculatorState(
    val input: StringBuilder = StringBuilder("0"),
    var firstOperand: Double? = null,
    var currentOperation: String? = null,
    var resetInputOnNextDigit: Boolean = true,
    val logEntries: MutableList<CalculatorLogEntry> = mutableListOf()
)

class CalculatorAnnotationHandler(
    private val context: Context,
    private val onCalculatorUpdate: (Annotation, String?, String?) -> Unit
) {
    // Map to store state for each annotation's calculator
    private val activeStates = mutableMapOf<Int, CalculatorState>()
    private val activeLogAdapters = mutableMapOf<Int, CalculatorLogAdapter>()

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    private val listCalculatorLogEntryType: Type by lazy {
        Types.newParameterizedType(List::class.java, CalculatorLogEntry::class.java)
    }
    private val calculatorLogListAdapter: com.squareup.moshi.JsonAdapter<List<CalculatorLogEntry>> by lazy {
        moshi.adapter(listCalculatorLogEntryType)
    }

    companion object {
        private const val MAX_INPUT_LENGTH = 15 // Maximum number of digits for input
        private const val TAG = "CalculatorHandler"
    }

    private fun formatNumber(number: Double): String {
        val df = DecimalFormat("#.##########") // Adjust formatting as needed
        return df.format(number)
    }

    private fun getStateForAnnotation(annotation: Annotation): CalculatorState {
        return activeStates.getOrPut(annotation.id) {
            val newState = CalculatorState()
            val parsedLog = parseCalculatorData(annotation.annotation)
            newState.logEntries.addAll(parsedLog)
            newState
        }
    }

    fun displayCalculator(annotation: Annotation, container: ViewGroup) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(context)
        val calcView = inflater.inflate(R.layout.calculator_annotation_layout, container, true)

        val state = getStateForAnnotation(annotation)

        val logRecyclerView = calcView.findViewById<RecyclerView>(R.id.calculator_log_recyclerview)
        val logAdapter = CalculatorLogAdapter(
            state.logEntries,
            { entry, position -> toggleScratchEntry(annotation, calcView, position) },
            this::formatNumber
        )
        activeLogAdapters[annotation.id] = logAdapter
        logRecyclerView.layoutManager = LinearLayoutManager(context)
        logRecyclerView.adapter = logAdapter

        setupCalculatorButtons(calcView, annotation)
        updateDisplay(annotation, calcView)
        Log.d(TAG, "Displayed calculator for annotation ID: ${annotation.id}, current display: '${state.input}'")
    }

    private fun parseCalculatorData(jsonString: String?): List<CalculatorLogEntry> {
        return if (jsonString != null) {
            try {
                calculatorLogListAdapter.fromJson(jsonString) ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing calculator data", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun serializeCalculatorData(log: List<CalculatorLogEntry>): String {
        return calculatorLogListAdapter.toJson(log)
    }

    private fun setupCalculatorButtons(calcView: View, annotation: Annotation) {
        val digitButtonIds = mapOf(
            R.id.button_0 to "0", R.id.button_1 to "1", R.id.button_2 to "2", R.id.button_3 to "3",
            R.id.button_4 to "4", R.id.button_5 to "5", R.id.button_6 to "6", R.id.button_7 to "7",
            R.id.button_8 to "8", R.id.button_9 to "9"
        )
        digitButtonIds.forEach { (id, digit) ->
            calcView.findViewById<Button>(id)?.setOnClickListener { appendDigit(digit, annotation, calcView) }
        }

        calcView.findViewById<Button>(R.id.button_add)?.setOnClickListener { setOperation("+", annotation, calcView) }
        calcView.findViewById<Button>(R.id.button_subtract)?.setOnClickListener { setOperation("-", annotation, calcView) }
        calcView.findViewById<Button>(R.id.button_multiply)?.setOnClickListener { setOperation("*", annotation, calcView) }
        calcView.findViewById<Button>(R.id.button_divide)?.setOnClickListener { setOperation("/", annotation, calcView) }
        calcView.findViewById<Button>(R.id.button_equals)?.setOnClickListener { calculateResult(annotation, calcView) }
        calcView.findViewById<Button>(R.id.button_decimal)?.setOnClickListener { appendDecimal(annotation, calcView) }
        calcView.findViewById<Button>(R.id.button_clear)?.setOnClickListener { clearInput(annotation, calcView) }
        calcView.findViewById<Button>(R.id.button_backspace)?.setOnClickListener { handleBackspace(annotation, calcView) }
        calcView.findViewById<Button>(R.id.button_percent)?.setOnClickListener { handlePercent(annotation, calcView) }
    }

    private fun appendDigit(digit: String, annotation: Annotation, calcView: View) {
        val state = activeStates[annotation.id] ?: return
        Log.d(TAG, "Appending digit: $digit to A_ID: ${annotation.id}, currentInput: '${state.input}', resetInput: ${state.resetInputOnNextDigit}")

        if (state.resetInputOnNextDigit) {
            state.input.clear()
            state.resetInputOnNextDigit = false
        }
        if (state.input.toString() == "0" && digit != ".") {
            state.input.clear()
        }
        if (state.input.length < MAX_INPUT_LENGTH) {
            state.input.append(digit)
        } else {
            Toast.makeText(context, "Max input length reached", Toast.LENGTH_SHORT).show()
        }
        updateDisplay(annotation, calcView)
    }

    private fun appendDecimal(annotation: Annotation, calcView: View) {
        val state = activeStates[annotation.id] ?: return
        Log.d(TAG, "Appending decimal to A_ID: ${annotation.id}, currentInput: '${state.input}', resetInput: ${state.resetInputOnNextDigit}")
        if (state.resetInputOnNextDigit) {
            state.input.clear().append("0") // Start with "0." if reset
            state.resetInputOnNextDigit = false
        }
        if (!state.input.contains(".")) {
            if (state.input.length < MAX_INPUT_LENGTH) {
                state.input.append(".")
            } else {
                Toast.makeText(context, "Max input length reached", Toast.LENGTH_SHORT).show()
            }
        }
        updateDisplay(annotation, calcView)
    }

    private fun setOperation(op: String, annotation: Annotation, calcView: View) {
        val state = activeStates[annotation.id] ?: return
        val currentInputValueString = state.input.toString()
        val currentInputValue = currentInputValueString.toDoubleOrNull()

        if (currentInputValueString == "Error") {
            if (state.firstOperand == null) {
                Log.d(TAG, "Operator pressed while display is Error and no firstOperand. Ignoring.")

                return
            }
        }

        if (currentInputValue != null) {
            if (state.firstOperand != null && state.currentOperation != null) {
                calculateResult(annotation, calcView, addToLog = true, updateDisplayAfterCalc = false)
            } else {
                state.firstOperand = currentInputValue

            }
        } else {
            if (state.firstOperand == null) {
                state.firstOperand = 0.0
                state.input.clear().append("0")
            }
        }

        state.currentOperation = op
        state.resetInputOnNextDigit = true
        if (state.firstOperand != null) {
            state.input.clear().append(formatNumber(state.firstOperand!!))
        } else {
            // This case should ideally be covered by the defaulting logic above.
            // As a fallback, ensure display is "0" if firstOperand is somehow still null.
            state.input.clear().append("0")
        }

        Log.d(TAG, "Operation set for A_ID ${annotation.id}: $op. First operand: ${state.firstOperand}. Display will show: '${state.input}'")
        updateDisplay(annotation, calcView)
    }

    private fun calculateResult(annotation: Annotation, calcView: View, addToLog: Boolean = true, updateDisplayAfterCalc: Boolean = true) {
        val state = activeStates[annotation.id] ?: return
        val secondOperandString = state.input.toString()
        val secondOperand = secondOperandString.toDoubleOrNull()

        if (state.currentOperation == null) {
            if (secondOperand != null) {
                state.input.clear().append(formatNumber(secondOperand))
                state.firstOperand = secondOperand
            }
            state.resetInputOnNextDigit = true
            if (updateDisplayAfterCalc) updateDisplay(annotation, calcView)
            return
        }

        if (state.firstOperand == null || secondOperand == null) {
            if (secondOperandString == "Error") {
            } else {
                state.input.clear().append("Error")
                state.firstOperand = null
            }
            state.currentOperation = null
            state.resetInputOnNextDigit = true
            if (updateDisplayAfterCalc) updateDisplay(annotation, calcView)
            saveAnnotation(annotation)
            return
        }

        val firstOperandValue = state.firstOperand!!
        val operation = state.currentOperation!!
        var resultValue: Double? = null

        try {
            resultValue = when (operation) {
                "+" -> firstOperandValue + secondOperand
                "-" -> firstOperandValue - secondOperand
                "*" -> firstOperandValue * secondOperand
                "/" -> {
                    if (secondOperand == 0.0) throw ArithmeticException("Division by zero")
                    firstOperandValue / secondOperand
                }
                else -> null
            }
        } catch (e: ArithmeticException) {
            Log.e(TAG, "ArithmeticException in calculation: ${e.message}")
            state.input.clear().append("Error")
            state.firstOperand = null
        } catch (e: Exception) {
            Log.e(TAG, "Exception in calculation: ${e.message}")
            state.input.clear().append("Error")
            state.firstOperand = null
        }

        if (resultValue != null) {
            val formattedResult = formatNumber(resultValue)
            state.input.clear().append(formattedResult)
            state.firstOperand = resultValue
            if (addToLog) {
                val logEntry = CalculatorLogEntry(firstOperandValue, secondOperand, operation, resultValue)
                state.logEntries.add(0, logEntry)
                activeLogAdapters[annotation.id]?.notifyItemInserted(0)
            }
        } else {
            if (state.input.toString() != "Error") {
                state.input.clear().append("Error")
            }
            if (state.firstOperand != null) {
                state.firstOperand = null
            }
        }

        state.currentOperation = null
        state.resetInputOnNextDigit = true

        if (updateDisplayAfterCalc) {
            updateDisplay(annotation, calcView)
        }
        saveAnnotation(annotation)
        Log.d(TAG, "Calculated for A_ID ${annotation.id}: $firstOperandValue $operation $secondOperand = ${state.input}. FirstOp after calc: ${state.firstOperand}, ResetNext: ${state.resetInputOnNextDigit}")
    }

    private fun clearInput(annotation: Annotation, calcView: View) {
        val state = activeStates[annotation.id] ?: return
        state.input.clear().append("0")
        state.firstOperand = null
        state.currentOperation = null
        state.resetInputOnNextDigit = true
        updateDisplay(annotation, calcView)
        Log.d(TAG, "Cleared input for A_ID ${annotation.id}")
    }

    private fun handleBackspace(annotation: Annotation, calcView: View) {
        val state = activeStates[annotation.id] ?: return
        if (state.resetInputOnNextDigit || state.input.toString() == "Error") {
            // If reset is pending or display shows error, backspace acts like clear for current input
            state.input.clear().append("0")
            state.resetInputOnNextDigit = true // Keep it true if it was, or set to true after clearing error
        } else if (state.input.isNotEmpty()) {
            state.input.deleteCharAt(state.input.length - 1)
            if (state.input.isEmpty()) {
                state.input.append("0")
                state.resetInputOnNextDigit = true // Back to initial state
            }
        }
        updateDisplay(annotation, calcView)
    }

    private fun handlePercent(annotation: Annotation, calcView: View) {
        val state = activeStates[annotation.id] ?: return
        val inputValue = state.input.toString().toDoubleOrNull()
        if (inputValue == null) {
            state.input.clear().append("Error")
            updateDisplay(annotation, calcView)
            return
        }

        val result: Double
        if (state.firstOperand != null && state.currentOperation != null) {
            // Calculate percentage of the first operand: e.g., 100 + 10% => 100 + (100 * 0.10)
            result = state.firstOperand!! * (inputValue / 100.0)
        } else {
            // Simple percentage: e.g., 10% => 0.10
            result = inputValue / 100.0
        }

        state.input.clear().append(formatNumber(result))
        state.resetInputOnNextDigit = true
        updateDisplay(annotation, calcView)
    }

    private fun updateDisplay(annotation: Annotation, calcView: View) {
        val state = activeStates[annotation.id] ?: return
        val displayTextView = calcView.findViewById<TextView>(R.id.calculator_display)
        val stateDebugTextView = calcView.findViewById<TextView>(R.id.calculator_state_debug)

        var textToShow = state.input.toString()
        if (textToShow.isEmpty() && !state.resetInputOnNextDigit) {
            textToShow = "0"
            state.input.append("0")
            state.resetInputOnNextDigit = true
        } else if (textToShow.isEmpty() && state.resetInputOnNextDigit) {
            textToShow = "0"
        }

        if (textToShow.length > MAX_INPUT_LENGTH && textToShow != "Error") {
            textToShow = "Error"
            state.input.clear().append("Error")
        }
        Log.d(TAG, "Updating display for A_ID: ${annotation.id} with: '$textToShow'")
        displayTextView.text = textToShow

        val firstOpStr = state.firstOperand?.let { formatNumber(it) } ?: "null"
        val currentOpStr = state.currentOperation ?: "null"
        stateDebugTextView.text = "1st: $firstOpStr, Op: $currentOpStr"
    }

    private fun toggleScratchEntry(annotation: Annotation, calcView: View, position: Int) {
        val state = activeStates[annotation.id] ?: return
        if (position < 0 || position >= state.logEntries.size) return

        val entry = state.logEntries[position]
        entry.scratch = !entry.scratch
        activeLogAdapters[annotation.id]?.notifyItemChanged(position)
        saveAnnotation(annotation)
    }

    private fun saveAnnotation(annotation: Annotation) {
        val state = activeStates[annotation.id] ?: return
        val serializedLog = serializeCalculatorData(state.logEntries)
        val dataPayload = JSONObject()
        dataPayload.put("calculator_log", serializedLog)
        val updatedAnnotation = annotation.copy(
            annotation = dataPayload.toString()
        )
        onCalculatorUpdate(updatedAnnotation, null, null)
        Log.d(TAG, "Saved annotation for A_ID ${annotation.id}")
    }
}

class CalculatorLogAdapter(
    private val logEntries: MutableList<CalculatorLogEntry>,
    private val onScratchClick: (CalculatorLogEntry, Int) -> Unit,
    private val numberFormatter: (Double) -> String
) : RecyclerView.Adapter<CalculatorLogAdapter.LogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calculator_log, parent, false)
        return LogViewHolder(view, numberFormatter)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val entry = logEntries[position]
        holder.bind(entry, position, onScratchClick)
    }

    override fun getItemCount(): Int = logEntries.size

    class LogViewHolder(
        itemView: View,
        private val numberFormatter: (Double) -> String
    ) : RecyclerView.ViewHolder(itemView) {
        private val logTextView: TextView = itemView.findViewById(R.id.log_entry_text)
        private val scratchButton: ImageButton = itemView.findViewById(R.id.button_scratch_log)

        fun bind(entry: CalculatorLogEntry, position: Int, onScratchClick: (CalculatorLogEntry, Int) -> Unit) {
            logTextView.text = "${numberFormatter(entry.inputPromptFirstValue)} ${entry.operation} ${numberFormatter(entry.inputPromptSecondValue)} = ${numberFormatter(entry.result)}"
            if (entry.scratch) {
                logTextView.paintFlags = logTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                scratchButton.alpha = 0.5f
            } else {
                logTextView.paintFlags = logTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                scratchButton.alpha = 1.0f
            }
            scratchButton.setOnClickListener { onScratchClick(entry, position) }
        }
    }
}