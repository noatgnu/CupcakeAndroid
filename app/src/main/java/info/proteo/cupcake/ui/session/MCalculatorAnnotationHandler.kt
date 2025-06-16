package info.proteo.cupcake.ui.session

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import info.proteo.cupcake.R
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class DataLogEntry(
    val data: JSONObject,
    val operationType: String,
    val result: Double
)

class MCalculatorAnnotationHandler(
    private val context: Context,
    private val onAnnotationUpdate: (Annotation, String?, String?) -> Unit
) {
    private val dataLog = mutableListOf<DataLogEntry>()
    private var selectedForm: String = "dynamic"

    fun displayMolarityCalculator(annotation: Annotation, container: ViewGroup) {
        container.removeAllViews()
        initializeFromString(annotation.annotation)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.molarity_calculator_layout, container, false)
        container.addView(view)

        val formSelector = view.findViewById<Spinner>(R.id.formSelector)
        val formTypes = listOf(
            "dynamic",
            "massFromVolumeAndConcentration",
            "volumeFromMassAndConcentration",
            "concentrationFromMassAndVolume",
            "volumeFromStockVolumeAndConcentration"
        )
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, formTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        formSelector.adapter = adapter
        formSelector.setSelection(formTypes.indexOf(selectedForm))
        formSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedForm = formTypes[position]
                updateFormVisibility(view?.rootView ?: container)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Set up calculate and save buttons
        view.findViewById<Button>(R.id.btnCalculate)?.setOnClickListener {
            calculate(view)
            updateAnnotation(annotation)
            refreshLog(view)

        }

        // Initial form visibility and log
        updateFormVisibility(view)
        refreshLog(view)
    }

    private fun initializeFromString(json: String?) {
        dataLog.clear()
        if (json.isNullOrBlank()) return
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val data = obj.getJSONObject("data")
                val type = obj.getString("operationType")
                val result = obj.getDouble("result")
                dataLog += DataLogEntry(data, type, result)
            }
        } catch (_: JSONException) {
            dataLog.clear()
        }
    }

    private fun serializeToString(): String {
        val arr = JSONArray()
        dataLog.forEach {
            val obj = JSONObject().apply {
                put("data", it.data)
                put("operationType", it.operationType)
                put("result", it.result)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun updateAnnotation(annotation: Annotation) {
        val payload = serializeToString()
        val updatedAnnotation = annotation.copy(
            annotation = payload,
        )
        onAnnotationUpdate(updatedAnnotation, null, null)
    }

    private fun calculate(root: View) {
        when (selectedForm) {
            "massFromVolumeAndConcentration" -> calculateMassFromVolumeAndConcentration(root)
            "volumeFromMassAndConcentration" -> calculateVolumeFromMassAndConcentration(root)
            "concentrationFromMassAndVolume" -> calculateConcentrationFromMassAndVolume(root)
            "volumeFromStockVolumeAndConcentration" -> calculateVolumeFromStockVolumeAndConcentration(root)
            "dynamic" -> calculateDynamic(root)
        }
    }

    // --- Calculation logic for each form ---

    private fun calculateMassFromVolumeAndConcentration(root: View) {
        val concentration = root.getDouble(R.id.inputConcentration1)
        val volume = root.getDouble(R.id.inputVolume1)
        val molecularWeight = root.getDouble(R.id.inputMolecularWeight1)
        val concentrationUnit = root.getString(R.id.inputConcentrationUnit1)
        val volumeUnit = root.getString(R.id.inputVolumeUnit1)
        val weightUnit = root.getString(R.id.inputWeightUnit1)
        if (concentration != null && volume != null && molecularWeight != null &&
            concentrationUnit != null && volumeUnit != null && weightUnit != null) {
            val concentrationInMolar = convertMolarity(concentration, concentrationUnit, "M")
            val volumeInLiters = convertVolume(volume, volumeUnit, "L")
            val mass = concentrationInMolar * volumeInLiters * molecularWeight
            val finalWeight = convertMass(mass, "g", weightUnit)
            val data = JSONObject().apply {
                put("concentration", concentration)
                put("volume", volume)
                put("molecularWeight", molecularWeight)
                put("concentrationUnit", concentrationUnit)
                put("volumeUnit", volumeUnit)
                put("weightUnit", weightUnit)
            }
            dataLog += DataLogEntry(data, "massFromVolumeAndConcentration", finalWeight)
        }
    }

    private fun calculateVolumeFromMassAndConcentration(root: View) {
        val weight = root.getDouble(R.id.inputWeight2)
        val concentration = root.getDouble(R.id.inputConcentration2)
        val molecularWeight = root.getDouble(R.id.inputMolecularWeight2)
        val weightUnit = root.getString(R.id.inputWeightUnit2)
        val concentrationUnit = root.getString(R.id.inputConcentrationUnit2)
        val volumeUnit = root.getString(R.id.inputVolumeUnit2)
        if (weight != null && concentration != null && molecularWeight != null &&
            weightUnit != null && concentrationUnit != null && volumeUnit != null) {
            val weightInGrams = convertMass(weight, weightUnit, "g")
            val concentrationInMolar = convertMolarity(concentration, concentrationUnit, "M")
            val volume = weightInGrams / (concentrationInMolar * molecularWeight)
            val finalVolume = convertVolume(volume, "L", volumeUnit)
            val data = JSONObject().apply {
                put("weight", weight)
                put("concentration", concentration)
                put("molecularWeight", molecularWeight)
                put("weightUnit", weightUnit)
                put("concentrationUnit", concentrationUnit)
                put("volumeUnit", volumeUnit)
            }
            dataLog += DataLogEntry(data, "volumeFromMassAndConcentration", finalVolume)
        }
    }

    private fun calculateConcentrationFromMassAndVolume(root: View) {
        val weight = root.getDouble(R.id.inputWeight3)
        val volume = root.getDouble(R.id.inputVolume3)
        val molecularWeight = root.getDouble(R.id.inputMolecularWeight3)
        val weightUnit = root.getString(R.id.inputWeightUnit3)
        val volumeUnit = root.getString(R.id.inputVolumeUnit3)
        val concentrationUnit = root.getString(R.id.inputConcentrationUnit3)
        if (weight != null && volume != null && molecularWeight != null &&
            weightUnit != null && volumeUnit != null && concentrationUnit != null) {
            val weightInGrams = convertMass(weight, weightUnit, "g")
            val volumeInLiters = convertVolume(volume, volumeUnit, "L")
            val concentration = weightInGrams / (volumeInLiters * molecularWeight)
            val finalConcentration = convertMolarity(concentration, "M", concentrationUnit)
            val data = JSONObject().apply {
                put("weight", weight)
                put("volume", volume)
                put("molecularWeight", molecularWeight)
                put("weightUnit", weightUnit)
                put("volumeUnit", volumeUnit)
                put("concentrationUnit", concentrationUnit)
            }
            dataLog += DataLogEntry(data, "concentrationFromMassAndVolume", finalConcentration)
        }
    }

    private fun calculateVolumeFromStockVolumeAndConcentration(root: View) {
        val volumeUnit = root.getString(R.id.inputVolumeUnit4)
        val stockConcentration = root.getDouble(R.id.inputStockConcentration4)
        val stockConcentrationUnit = root.getString(R.id.inputStockConcentrationUnit4)
        val targetConcentration = root.getDouble(R.id.inputTargetConcentration4)
        val targetConcentrationUnit = root.getString(R.id.inputTargetConcentrationUnit4)
        val stockVolume = root.getDouble(R.id.inputStockVolume4)
        val stockVolumeUnit = root.getString(R.id.inputStockVolumeUnit4)
        if (volumeUnit != null && stockVolume != null && stockVolumeUnit != null &&
            stockConcentration != null && stockConcentrationUnit != null &&
            targetConcentration != null && targetConcentrationUnit != null) {
            val stockConcentrationInMolar = convertMolarity(stockConcentration, stockConcentrationUnit, "M")
            val targetConcentrationInMolar = convertMolarity(targetConcentration, targetConcentrationUnit, "M")
            val stockVolumeInLiters = convertVolume(stockVolume, stockVolumeUnit, "L")
            val volume = stockVolumeInLiters / stockConcentrationInMolar * targetConcentrationInMolar
            val finalVolume = convertVolume(volume, "L", volumeUnit)
            val data = JSONObject().apply {
                put("volumeUnit", volumeUnit)
                put("stockConcentration", stockConcentration)
                put("stockConcentrationUnit", stockConcentrationUnit)
                put("targetConcentration", targetConcentration)
                put("targetConcentrationUnit", targetConcentrationUnit)
                put("stockVolume", stockVolume)
                put("stockVolumeUnit", stockVolumeUnit)
            }
            dataLog += DataLogEntry(data, "volumeFromStockVolumeAndConcentration", finalVolume)
        }
    }

    private fun calculateDynamic(root: View) {
        val concentration = root.getDouble(R.id.inputConcentrationDyn)
        val volume = root.getDouble(R.id.inputVolumeDyn)
        val molecularWeight = root.getDouble(R.id.inputMolecularWeightDyn)
        val weight = root.getDouble(R.id.inputWeightDyn)
        val concentrationUnit = root.getString(R.id.inputConcentrationUnitDyn)
        val volumeUnit = root.getString(R.id.inputVolumeUnitDyn)
        val weightUnit = root.getString(R.id.inputWeightUnitDyn)
        if (concentrationUnit != null && volumeUnit != null && weightUnit != null) {
            val data = JSONObject()
            if (concentration != null && volume != null && molecularWeight != null) {
                val concentrationInMolar = convertMolarity(concentration, concentrationUnit, "M")
                val volumeInLiters = convertVolume(volume, volumeUnit, "L")
                val mass = concentrationInMolar * volumeInLiters * molecularWeight
                val finalWeight = convertMass(mass, "g", weightUnit)
                data.put("concentration", concentration)
                data.put("volume", volume)
                data.put("molecularWeight", molecularWeight)
                data.put("concentrationUnit", concentrationUnit)
                data.put("volumeUnit", volumeUnit)
                data.put("weightUnit", weightUnit)
                dataLog += DataLogEntry(data, "dynamic", finalWeight)
            } else if (weight != null && volume != null && molecularWeight != null) {
                val weightInGrams = convertMass(weight, weightUnit, "g")
                val concentrationVal = weightInGrams / (volume * molecularWeight)
                val finalConcentration = convertMolarity(concentrationVal, "M", concentrationUnit)
                data.put("weight", weight)
                data.put("volume", volume)
                data.put("molecularWeight", molecularWeight)
                data.put("weightUnit", weightUnit)
                data.put("volumeUnit", volumeUnit)
                data.put("concentrationUnit", concentrationUnit)
                dataLog += DataLogEntry(data, "dynamic", finalConcentration)
            } else if (weight != null && concentration != null && molecularWeight != null) {
                val weightInGrams = convertMass(weight, weightUnit, "g")
                val concentrationInMolar = convertMolarity(concentration, concentrationUnit, "M")
                val volumeVal = weightInGrams / (concentrationInMolar * molecularWeight)
                val finalVolume = convertVolume(volumeVal, "L", volumeUnit)
                data.put("weight", weight)
                data.put("concentration", concentration)
                data.put("molecularWeight", molecularWeight)
                data.put("weightUnit", weightUnit)
                data.put("concentrationUnit", concentrationUnit)
                data.put("volumeUnit", volumeUnit)
                dataLog += DataLogEntry(data, "dynamic", finalVolume)
            } else if (weight != null && volume != null && concentration != null) {
                val weightInGrams = convertMass(weight, weightUnit, "g")
                val volumeInLiters = convertVolume(volume, volumeUnit, "L")
                val concentrationInMolar = convertMolarity(concentration, concentrationUnit, "M")
                val molecularWeightVal = weightInGrams / (volumeInLiters * concentrationInMolar)
                data.put("weight", weight)
                data.put("volume", volume)
                data.put("concentration", concentration)
                data.put("weightUnit", weightUnit)
                data.put("volumeUnit", volumeUnit)
                data.put("concentrationUnit", concentrationUnit)
                dataLog += DataLogEntry(data, "dynamic", molecularWeightVal)
            }
        }
    }

    // --- Helpers for extracting values from EditTexts/Spinners ---

    private fun View.getDouble(id: Int): Double? =
        (findViewById<EditText>(id)?.text?.toString()?.toDoubleOrNull())

    private fun View.getString(id: Int): String? =
        (findViewById<Spinner>(id)?.selectedItem as? String)
            ?: findViewById<EditText>(id)?.text?.toString()

    // --- Unit conversion helpers (implement as needed) ---

    private fun convertMolarity(value: Double, fromUnit: String, toUnit: String): Double {
        // Example: "uM" to "M"
        return when (fromUnit to toUnit) {
            "uM" to "M" -> value * 1e-6
            "mM" to "M" -> value * 1e-3
            "M" to "M" -> value
            "M" to "uM" -> value * 1e6
            "M" to "mM" -> value * 1e3
            else -> value
        }
    }

    private fun convertVolume(value: Double, fromUnit: String, toUnit: String): Double {
        return when (fromUnit to toUnit) {
            "mL" to "L" -> value * 1e-3
            "L" to "L" -> value
            "L" to "mL" -> value * 1e3
            else -> value
        }
    }

    private fun convertMass(value: Double, fromUnit: String, toUnit: String): Double {
        return when (fromUnit to toUnit) {
            "mg" to "g" -> value * 1e-3
            "g" to "g" -> value
            "g" to "mg" -> value * 1e3
            else -> value
        }
    }

    // --- UI helpers ---

    private fun updateFormVisibility(root: View) {
        // Show/hide form layouts based on selectedForm
        val forms = listOf(
            R.id.formDynamicLayout to "dynamic",
            R.id.form1Layout to "massFromVolumeAndConcentration",
            R.id.form2Layout to "volumeFromMassAndConcentration",
            R.id.form3Layout to "concentrationFromMassAndVolume",
            R.id.form4Layout to "volumeFromStockVolumeAndConcentration"
        )
        for ((id, type) in forms) {
            root.findViewById<View>(id)?.visibility =
                if (selectedForm == type) View.VISIBLE else View.GONE
        }
    }

    private fun refreshLog(root: View) {
        val logView = root.findViewById<TextView>(R.id.dataLogView)
        val sb = StringBuilder()
        dataLog.forEachIndexed { idx, entry ->
            val d = entry.data
            val r = entry.result
            val type = entry.operationType
            val line = when (type) {
                "massFromVolumeAndConcentration" -> {
                    "${d.opt("concentration")} ${d.opt("concentrationUnit")} x ${d.opt("volume")} ${d.opt("volumeUnit")} x ${d.opt("molecularWeight")} = $r ${d.opt("weightUnit")}"
                }
                "volumeFromMassAndConcentration" -> {
                    "${d.opt("weight")} ${d.opt("weightUnit")} / (${d.opt("concentration")} ${d.opt("concentrationUnit")} x ${d.opt("molecularWeight")}) = $r ${d.opt("volumeUnit")}"
                }
                "concentrationFromMassAndVolume" -> {
                    "${d.opt("weight")} ${d.opt("weightUnit")} / (${d.opt("volume")} ${d.opt("volumeUnit")} x ${d.opt("molecularWeight")}) = $r ${d.opt("concentrationUnit")}"
                }
                "volumeFromStockVolumeAndConcentration" -> {
                    "${d.opt("stockVolume")} ${d.opt("stockVolumeUnit")} x ${d.opt("targetConcentration")} ${d.opt("targetConcentrationUnit")} / ${d.opt("stockConcentration")} ${d.opt("stockConcentrationUnit")} = $r ${d.opt("volumeUnit")}"
                }
                "dynamic" -> {
                    when {
                        d.has("volume") && d.has("concentration") && d.has("molecularWeight") -> {
                            "${d.opt("volume")} ${d.opt("volumeUnit")} x ${d.opt("concentration")} ${d.opt("concentrationUnit")} x ${d.opt("molecularWeight")} g/mol = $r ${d.opt("weightUnit")}"
                        }
                        d.has("weight") && d.has("volume") && d.has("concentration") -> {
                            "${d.opt("weight")} ${d.opt("weightUnit")} / (${d.opt("volume")} ${d.opt("volumeUnit")} x ${d.opt("concentration")} ${d.opt("concentrationUnit")}) = $r g/mol"
                        }
                        d.has("weight") && d.has("volume") && d.has("molecularWeight") -> {
                            "${d.opt("weight")} ${d.opt("weightUnit")} / (${d.opt("volume")} ${d.opt("volumeUnit")} x ${d.opt("molecularWeight")} g/mol) = $r ${d.opt("concentrationUnit")}"
                        }
                        d.has("weight") && d.has("concentration") && d.has("molecularWeight") -> {
                            "${d.opt("weight")} ${d.opt("weightUnit")} / (${d.opt("concentration")} ${d.opt("concentrationUnit")} x ${d.opt("molecularWeight")} g/mol) = $r ${d.opt("volumeUnit")}"
                        }
                        else -> "[dynamic] result: $r"
                    }
                }
                else -> "[${type}] result: $r"
            }
            sb.append("${idx + 1}. $line\n")
        }
        logView?.text = sb.toString()
    }
}