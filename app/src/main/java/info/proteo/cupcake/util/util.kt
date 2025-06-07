package info.proteo.cupcake.util

import android.content.res.Configuration
import android.content.res.Resources
import info.proteo.cupcake.data.remote.model.protocol.ProtocolStep

object ProtocolHtmlRenderer {

    fun ProtocolStep.renderAsHtml(resources: Resources): String {
        val nightModeFlags = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        val cssStyle = if (isDarkMode) {
            "body{color:#FFFFFF;background-color:#121212;font-family:sans-serif;font-size:14px;} " +
                "table{width:100%;border-collapse:collapse;} " +
                "th,td{border:1px solid #444;padding:8px;} " +
                "img{max-width:100%;height:auto;} " +
                "a{color:#BB86FC;}"
        } else {
            "body{color:#000000;background-color:#FFFFFF;font-family:sans-serif;font-size:14px;} " +
                "table{width:100%;border-collapse:collapse;} " +
                "th,td{border:1px solid #ddd;padding:8px;} " +
                "img{max-width:100%;height:auto;} " +
                "a{color:#6200EE;}"
        }

        return "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<style>$cssStyle</style>" +
            "</head><body>${this.descriptionStringPreprocess()}</body></html>"
    }

    fun ProtocolStep.htmlToPlainText(defaultValue: String = ""): String {
        if (this.stepDescription.isNullOrEmpty()) {
            return defaultValue
        }

        return android.text.Html.fromHtml(this.descriptionStringPreprocess(), android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }

    fun ProtocolStep.descriptionStringPreprocess(): String {
        val reagents = this.reagents ?: return this.stepDescription ?: ""
        var description = this.stepDescription ?: ""

        for (reagent in reagents) {
            description = description.replace("%${reagent.id}.name%", reagent.reagent.name)
            description = description.replace("%${reagent.id}.quantity%", reagent.quantity.toString())
            description = description.replace("%${reagent.id}.unit%", reagent.reagent.unit)
            if (reagent.scalable && reagent.scalableFactor != null) {
                val scaledQuantity = (reagent.quantity * reagent.scalableFactor)
                description = description.replace(
                    "%${reagent.id}.scaled_quantity%",
                    "%.2f".format(scaledQuantity)
                )
            }

        }

        return description
    }
}

