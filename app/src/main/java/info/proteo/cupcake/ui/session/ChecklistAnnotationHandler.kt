package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import org.json.JSONArray
import org.json.JSONObject

class ChecklistAnnotationHandler(
    private val context: Context,
    private val onChecklistUpdate: (Annotation, String?, String?) -> Unit
) {
    data class ChecklistData(
        val name: String,
        val checkList: List<ChecklistItem>
    )

    data class ChecklistItem(
        var checked: Boolean,
        val content: String
    )

    fun displayChecklist(annotation: Annotation, container: ViewGroup) {
        container.removeAllViews()

        try {
            if (annotation.annotation == null || annotation.annotation.isBlank()) {
                val errorText = TextView(container.context).apply {
                    text = "No checklist data available"
                    setTextColor(Color.RED)
                }
                container.addView(errorText)
                return
            }

            val checklistData = parseChecklistData(annotation.annotation)

            // Create title
            val titleText = TextView(container.context).apply {
                text = checklistData.name
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8
                }
            }
            container.addView(titleText)

            // Create checkbox items
            checklistData.checkList.forEachIndexed { index, item ->
                val checkBoxView = CheckBox(container.context).apply {
                    text = item.content
                    isChecked = item.checked

                    setOnCheckedChangeListener { _, isChecked ->
                        updateChecklistItemState(annotation, index, isChecked)
                    }
                }
                container.addView(checkBoxView)
            }
        } catch (e: Exception) {
            val errorText = TextView(container.context).apply {
                text = "Error loading checklist: ${e.message}"
                setTextColor(Color.RED)
            }
            container.addView(errorText)
        }
    }

    private fun updateChecklistItemState(annotation: Annotation, itemIndex: Int, isChecked: Boolean) {
        try {
            if (annotation.annotation == null || annotation.annotation.isBlank()) {
                Toast.makeText(
                    context,
                    "No checklist data available to update",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val checklistData = parseChecklistData(annotation.annotation)
            val updatedList = checklistData.checkList.toMutableList()

            if (itemIndex < updatedList.size) {
                updatedList[itemIndex] = updatedList[itemIndex].copy(checked = isChecked)

                val jsonObject = JSONObject().apply {
                    put("name", checklistData.name)
                    put("checkList", JSONArray().apply {
                        updatedList.forEach { item ->
                            put(JSONObject().apply {
                                put("checked", item.checked)
                                put("content", item.content)
                            })
                        }
                    })
                }

                val updatedAnnotation = annotation.copy(annotation = jsonObject.toString())
                onChecklistUpdate(updatedAnnotation, null, null)
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error updating checklist: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun parseChecklistData(jsonString: String): ChecklistData {
        return try {
            val json = JSONObject(jsonString)
            val name = json.getString("name")
            val checkListArray = json.getJSONArray("checkList")

            val checkList = mutableListOf<ChecklistItem>()
            for (i in 0 until checkListArray.length()) {
                val item = checkListArray.getJSONObject(i)
                checkList.add(
                    ChecklistItem(
                        checked = item.getBoolean("checked"),
                        content = item.getString("content")
                    )
                )
            }

            ChecklistData(name, checkList)
        } catch (e: Exception) {
            throw Exception("Invalid checklist format: ${e.message}")
        }
    }
}