package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import info.proteo.cupcake.shared.data.model.annotation.Annotation
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
            if (annotation.annotation.isNullOrBlank()) {
                val errorText = TextView(container.context).apply {
                    text = "Table data is empty or invalid."
                    setTextColor(Color.RED)
                }
                container.addView(errorText)
                return
            }

            val tableData = parseTableData(annotation.annotation!!)
            var isEditMode = false // Keep track of edit mode state

            val mainLayout = LinearLayout(container.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            container.addView(mainLayout)

            val headerLayout = LinearLayout(container.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 0, 0, 8) // Add some padding
            }

            val titleView = TextView(container.context).apply {
                text = tableData.name
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f // Weight to push toggles to the right
                )
            }

            val editToggle = SwitchCompat(container.context).apply {
                text = "Edit"
                isChecked = isEditMode
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnCheckedChangeListener { _, isChecked ->
                    isEditMode = isChecked
                    // Refresh table content to apply edit mode
                    val tableContentContainer = mainLayout.findViewWithTag<LinearLayout>("table_content")
                    tableContentContainer?.let {
                        refreshTableContent(it, annotation, isEditMode, tableData.tracking)
                    }
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
                    // No need to refresh full table, just update internal state if needed
                    // Or, if tracking changes cell appearance, then refresh:
                    // val tableContentContainer = mainLayout.findViewWithTag<LinearLayout>("table_content")
                    // tableContentContainer?.let {
                    //     refreshTableContent(it, annotation, isEditMode, isChecked)
                    // }
                }
            }

            headerLayout.addView(titleView)
            headerLayout.addView(editToggle)
            headerLayout.addView(trackingToggle)
            mainLayout.addView(headerLayout)

            val tableContainer = LinearLayout(container.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                tag = "table_content" // Tag to find this container later
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
            if (annotation.annotation.isNullOrBlank()) {
                // Handle empty or invalid annotation case
                return
            }
            val tableData = parseTableData(annotation.annotation!!)

            for (i in 0 until tableData.nRow) {
                val rowLayout = LinearLayout(container.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                for (j in 0 until tableData.nCol) {
                    val cellContent = tableData.content.getOrNull(i)?.getOrNull(j) ?: ""
                    val cellKey = "$i,$j"
                    val isHighlighted = tableData.trackingMap[cellKey] ?: false

                    val cellView = TextView(container.context).apply {
                        text = cellContent
                        setPadding(8, 8, 8, 8)
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1.0f
                        ).also {
                            it.setMargins(1, 1, 1, 1)
                        }
                        background = getTableCellBackground(isHighlighted && isTrackingMode)
                        setTextColor(if (isHighlighted && isTrackingMode) Color.WHITE else Color.BLACK)


                        setOnClickListener {
                            if (isEditMode) {
                                showEditCellDialog(annotation, i, j, cellContent)
                            } else if (isTrackingMode) {
                                updateTableCellState(annotation, i, j, !isHighlighted, this)
                            }
                        }
                    }
                    rowLayout.addView(cellView)
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

    private fun showEditCellDialog(annotation: Annotation, row: Int, col: Int, currentContent: String) {
        val dialogView = LayoutInflater.from(context).inflate(android.R.layout.select_dialog_item, null) // Using a simple layout
        val editText = EditText(context).apply {
            setText(currentContent)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setSingleLine(false)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24) // Add some padding to the dialog content
            addView(editText)
        }

        AlertDialog.Builder(context)
            .setTitle("Edit Cell (${row + 1}, ${col + 1})")
            .setView(container)
            .setPositiveButton("Save") { dialog, _ ->
                val newContent = editText.text.toString()
                if (newContent != currentContent) {
                    updateTableCellContent(annotation, row, col, newContent)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }


    private fun updateTableTracking(annotation: Annotation, isTracking: Boolean) {
        try {
            if (annotation.annotation.isNullOrBlank()) {
                Toast.makeText(context, "Cannot update tracking: Table data is missing.", Toast.LENGTH_SHORT).show()
                return
            }
            val json = JSONObject(annotation.annotation)
            json.put("tracking", isTracking)
            val updatedAnnotation = annotation.copy(annotation = json.toString())
            onTableUpdate(updatedAnnotation, null, null)
        } catch (e: Exception) {
            Toast.makeText(context, "Error updating table tracking: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTableCellContent(annotation: Annotation, row: Int, col: Int, newContent: String) {
        try {
            if (annotation.annotation.isNullOrBlank()) {
                Toast.makeText(context, "Cannot update content: Table data is missing.", Toast.LENGTH_SHORT).show()
                return
            }
            val json = JSONObject(annotation.annotation)
            val contentArray = json.getJSONArray("content")
            if (row < contentArray.length()) {
                val rowArray = contentArray.getJSONArray(row)
                if (col < rowArray.length()) {
                    rowArray.put(col, newContent)
                    val updatedAnnotation = annotation.copy(annotation = json.toString())
                    onTableUpdate(updatedAnnotation, null, null)
                } else {
                    Toast.makeText(context, "Error: Column index out of bounds.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Error: Row index out of bounds.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error updating table content: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTableCellState(annotation: Annotation, row: Int, col: Int, highlighted: Boolean, view: android.view.View) {
        try {
            if (annotation.annotation.isNullOrBlank()) {
                Toast.makeText(context, "Cannot update state: Table data is missing.", Toast.LENGTH_SHORT).show()
                return
            }
            val cellKey = "$row,$col"
            val json = JSONObject(annotation.annotation)
            val trackingMapJson = json.optJSONObject("trackingMap") ?: JSONObject() // Ensure trackingMap exists
            trackingMapJson.put(cellKey, highlighted)
            json.put("trackingMap", trackingMapJson) // Put it back if it was newly created

            val updatedAnnotation = annotation.copy(annotation = json.toString())
            onTableUpdate(updatedAnnotation, null, null)

            // Visually update the cell directly if view is passed and valid
            // This provides immediate feedback before the adapter potentially rebinds
            if (view is TextView) {
                view.background = getTableCellBackground(highlighted)
                view.setTextColor(if (highlighted) Color.WHITE else Color.BLACK)
            }

        } catch (e: Exception) {
            Toast.makeText(context, "Error updating table cell state: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTableCellBackground(isHighlighted: Boolean): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(if (isHighlighted) Color.parseColor("#0d6efd") else Color.TRANSPARENT) // Use transparent for non-highlighted
            setStroke(1, Color.LTGRAY) // Use a lighter gray for borders
        }
    }

    fun parseTableData(jsonString: String): TableData {
        return try {
            val json = JSONObject(jsonString)
            val name = json.getString("name")
            val nRow = json.getInt("nRow")
            val nCol = json.getInt("nCol")
            val tracking = json.optBoolean("tracking", false) // Default to false if not present

            val contentArray = json.getJSONArray("content")
            val content = mutableListOf<List<String>>()
            for (i in 0 until nRow) {
                val row = mutableListOf<String>()
                val jsonRow = contentArray.optJSONArray(i) // Use optJSONArray for safety
                if (jsonRow != null) {
                    for (j in 0 until nCol) {
                        row.add(jsonRow.optString(j, "")) // Default to empty string
                    }
                } else { // if a row is missing, fill with empty strings for nCol
                    for (j in 0 until nCol) {
                        row.add("")
                    }
                }
                content.add(row)
            }

            val trackingMapJson = json.optJSONObject("trackingMap") ?: JSONObject() // Default to empty if not present
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