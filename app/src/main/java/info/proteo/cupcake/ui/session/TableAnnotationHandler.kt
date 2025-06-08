package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import org.json.JSONObject

class TableAnnotationHandler(
    private val context: Context,
    private val onTableUpdate: (Annotation, String?, String?) -> Unit
) {
    data class TableData(
        val name: String,
        val nRow: Int,
        val nCol: Int,
        val content: List<List<String>>,
        val tracking: Boolean,
        val trackingMap: Map<String, Boolean>
    )

    fun displayTable(annotation: Annotation, container: ViewGroup) {
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

            val editToggle = SwitchCompat(container.context).apply {
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
                    val contentContainer = mainLayout.findViewWithTag<ViewGroup>("table_content")
                    refreshTableContent(contentContainer, annotation, isChecked, tableData.tracking)
                }
            }

            val trackingToggle = SwitchCompat(container.context).apply {
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

            // Table content container
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
                            layoutParams = LinearLayout.LayoutParams(200, 100).apply {
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
                                    if (tag == null || !tag.toString().startsWith("pending_update")) {
                                        updateTableCellState(annotation, i, j, !isHighlighted, this)
                                    }
                                }
                            }
                        }
                        rowLayout.addView(editText)
                    } else {
                        val cell = TextView(container.context).apply {
                            text = cellContent
                            gravity = android.view.Gravity.CENTER
                            layoutParams = LinearLayout.LayoutParams(200, 100).apply {
                                setMargins(1, 1, 1, 1)
                            }
                            background = getTableCellBackground(isHighlighted)
                            setTextColor(if (isHighlighted) Color.WHITE else Color.BLACK)

                            if (isTrackingMode) {
                                tag = isHighlighted

                                setOnClickListener {
                                    if (tag == null || !tag.toString().startsWith("pending_update")) {
                                        updateTableCellState(annotation, i, j, !isHighlighted, this)
                                    }
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
                    context,
                    "No table data available to update",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val json = JSONObject(annotation.annotation)
            json.put("tracking", isTracking)

            val updatedAnnotation = annotation.copy(annotation = json.toString())
            onTableUpdate(updatedAnnotation, null, null)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error updating table tracking: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateTableCellContent(annotation: Annotation, row: Int, col: Int, newContent: String) {
        try {
            if (annotation.annotation == null || annotation.annotation.isBlank()) {
                Toast.makeText(
                    context,
                    "No table data available to update",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val json = JSONObject(annotation.annotation)
            val contentArray = json.getJSONArray("content")
            val rowArray = contentArray.getJSONArray(row)
            rowArray.put(col, newContent)

            val updatedAnnotation = annotation.copy(annotation = json.toString())
            onTableUpdate(updatedAnnotation, null, null)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error updating table content: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateTableCellState(annotation: Annotation, row: Int, col: Int, highlighted: Boolean, view: android.view.View) {
        try {
            if (annotation.annotation == null || annotation.annotation.isBlank()) {
                Toast.makeText(
                    context,
                    "No table data available to update",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val cellKey = "$row,$col"

            val json = JSONObject(annotation.annotation)
            val trackingMapJson = json.getJSONObject("trackingMap")
            trackingMapJson.put(cellKey, highlighted)

            val updatedAnnotation = annotation.copy(annotation = json.toString())
            onTableUpdate(updatedAnnotation, null, null)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error updating table: ${e.message}",
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

    fun parseTableData(jsonString: String): TableData {
        return try {
            val json = JSONObject(jsonString)
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

            val keys = trackingMapJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                trackingMap[key] = trackingMapJson.getBoolean(key)
            }

            TableData(name, nRow, nCol, content, tracking, trackingMap)
        } catch (e: Exception) {
            throw Exception("Invalid table format: ${e.message}")
        }
    }
}