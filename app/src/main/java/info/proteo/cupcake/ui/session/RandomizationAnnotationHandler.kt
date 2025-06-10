package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import java.text.DecimalFormat

@JsonClass(generateAdapter = true)
data class RandomizationForm(
    val samples: String = "",
    val nCol: Int = 0,
    val nRow: Int = 0,
    val generateNumber: Boolean = false,
    val numberMax: Double = 0.0,
    val numberMin: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class RandomizationData(
    val numberArray: List<List<String>> = emptyList(),
    val sampleArray: List<List<String>> = emptyList(),
    val form: RandomizationForm = RandomizationForm()
)

class RandomizationAnnotationHandler(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val randomizationDataAdapter = moshi.adapter(RandomizationData::class.java).nonNull()
    private val numberDecimalFormat = DecimalFormat("#.####")

    private enum class CellType { DATA, COLUMN_HEADER, ROW_LABEL, ROW_LABEL_HEADER }

    // Define a fixed width for data/header cells in dp
    private val cellWidthDp = 100f
    private val cellWidthPx: Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        cellWidthDp,
        context.resources.displayMetrics
    ).toInt()

    // Define a fixed width for row label cells in dp
    private val rowLabelWidthDp = 70f
    private val rowLabelWidthPx: Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        rowLabelWidthDp,
        context.resources.displayMetrics
    ).toInt()


    fun displayRandomizationData(annotation: Annotation, container: ViewGroup) {
        container.removeAllViews()
        val annotationDataJson = annotation.annotation

        if (annotationDataJson.isNullOrBlank()) {
            val errorText = TextView(context).apply {
                text = "Randomization data is empty or invalid."
                setTextColor(Color.RED)
            }
            container.addView(errorText)
            return
        }

        try {
            val randomizationData = randomizationDataAdapter.fromJson(annotationDataJson)

            if (randomizationData == null) {
                val errorText = TextView(context).apply {
                    text = "Failed to parse randomization data."
                    setTextColor(Color.RED)
                }
                container.addView(errorText)
                return
            }

            var tablesDisplayed = 0

            if (randomizationData.sampleArray.isNotEmpty()) {
                addTableToContainer(
                    "Randomized Sample Table",
                    randomizationData.sampleArray,
                    container
                )
                tablesDisplayed++
            }

            if (randomizationData.numberArray.isNotEmpty()) {
                addTableToContainer(
                    "Randomized Number Table",
                    randomizationData.numberArray,
                    container
                )
                tablesDisplayed++
            }

            if (tablesDisplayed == 0) {
                val noDataText = TextView(context).apply {
                    text = "No randomization data to display."
                }
                container.addView(noDataText)
            }

        } catch (e: Exception) {
            val errorText = TextView(context).apply {
                text = "Error loading randomization data: ${e.message}"
                setTextColor(Color.RED)
            }
            container.addView(errorText)
        }
    }

    private fun addTableToContainer(
        title: String,
        dataArray: List<List<String>>,
        mainContainer: ViewGroup
    ) {
        if (dataArray.isEmpty()) return

        val tableLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 16 }
        }

        val titleView = TextView(context).apply {
            text = title
            setTypeface(null, Typeface.BOLD)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8 }
        }
        tableLayout.addView(titleView)

        val tableGridContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = getTableBorderBackground()
        }

        val headerRowLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        headerRowLayout.addView(createCellTextView("", CellType.ROW_LABEL_HEADER))

        val numCols = dataArray.firstOrNull()?.size ?: 0
        for (j in 0 until numCols) {
            headerRowLayout.addView(createCellTextView("Col $j", CellType.COLUMN_HEADER))
        }
        tableGridContainer.addView(headerRowLayout)

        dataArray.forEachIndexed { rowIndex, rowData ->
            val dataRowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            dataRowLayout.addView(createCellTextView("Row $rowIndex", CellType.ROW_LABEL))

            for (colIndex in 0 until numCols) {
                val cellText = rowData.getOrNull(colIndex) ?: ""
                dataRowLayout.addView(createCellTextView(cellText, CellType.DATA))
            }
            tableGridContainer.addView(dataRowLayout)
        }

        val horizontalScrollView = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(tableGridContainer)
        }
        tableLayout.addView(horizontalScrollView)
        mainContainer.addView(tableLayout)
    }

    private fun createCellTextView(text: String, type: CellType): TextView {
        return TextView(context).apply {
            var displayText = text
            if (type == CellType.DATA) {
                try {
                    val numericValue = text.toDoubleOrNull()
                    if (numericValue != null) {
                        displayText = numberDecimalFormat.format(numericValue)
                    }
                } catch (nfe: NumberFormatException) {
                    // Not a number, use original text
                }
            }
            this.text = displayText
            setPadding(12, 12, 12, 12)
            background = getCellItemBorderBackground()
            maxLines = 1 // Ensure single line for truncation
            ellipsize = TextUtils.TruncateAt.END // Truncate with ellipsis

            val commonMargins = 0

            when (type) {
                CellType.ROW_LABEL_HEADER, CellType.ROW_LABEL -> {
                    layoutParams = LinearLayout.LayoutParams(
                        rowLabelWidthPx, // Fixed width for row labels
                        LinearLayout.LayoutParams.MATCH_PARENT // Match height of data cells in the row
                    ).also {
                        it.setMargins(commonMargins, commonMargins, commonMargins, commonMargins)
                    }
                    if (type == CellType.ROW_LABEL) {
                        setTypeface(null, Typeface.BOLD)
                    }
                }
                CellType.COLUMN_HEADER, CellType.DATA -> {
                    layoutParams = LinearLayout.LayoutParams(
                        cellWidthPx, // Fixed width for data/header cells
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also {
                        it.setMargins(commonMargins, commonMargins, commonMargins, commonMargins)
                    }
                    if (type == CellType.COLUMN_HEADER) {
                        setTypeface(null, Typeface.BOLD)
                    }
                }
            }
        }
    }

    private fun getCellItemBorderBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            setStroke(1, Color.parseColor("#E0E0E0"))
        }
    }

    private fun getTableBorderBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
        }
    }
}