package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import info.proteo.cupcake.data.repository.AnnotationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class SketchAnnotationHandler(
    private val context: Context,
    private val annotationRepository: AnnotationRepository,
    private val baseUrl: String
) {

    data class SketchData(
        val width: Int,
        val height: Int,
        val strokes: Any,
        val png: String
    )

    fun displaySketch(
        annotation: Annotation,
        imageView: ImageView,
        transcriptionText: TextView,
        progressContainer: ViewGroup,
        transcriptionContainer: View
    ) {
        val parentView = imageView.parent as? ViewGroup ?: progressContainer

        val progressBar = ProgressBar(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val progressBarHost = if (progressContainer != parentView &&
            parentView.findViewById<ProgressBar>(progressBar.id) == null)
            progressContainer else parentView

        val existingProgressBar = progressBarHost.findViewWithTag<ProgressBar>("sketchLoadProgressBar")
        existingProgressBar?.let { progressBarHost.removeView(it) }
        progressBar.tag = "sketchLoadProgressBar"

        var pBarIndex = if (parentView == progressBarHost) parentView.indexOfChild(imageView) else 0
        if(pBarIndex < 0) pBarIndex = 0

        try {
            progressBarHost.addView(progressBar, pBarIndex)
        } catch (e: IllegalStateException) {
        }

        if (!annotation.transcription.isNullOrBlank()) {
            transcriptionText.text = annotation.transcription
            transcriptionText.visibility = View.VISIBLE
            transcriptionContainer.visibility = View.VISIBLE
        } else {
            transcriptionText.visibility = View.GONE
            transcriptionContainer.visibility = View.GONE
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = annotationRepository.getSignedUrl(annotation.id)

                if (result.isSuccess) {
                    val signedToken = result.getOrNull()?.signedToken
                    if (signedToken != null) {
                        val signedUrl = "${baseUrl}api/annotation/download_signed/?token=$signedToken"
                        val sketchData = downloadSketchData(signedUrl)

                        withContext(Dispatchers.Main) {
                            if (sketchData != null) {
                                val imageData = sketchData.png

                                val imageSource = if (imageData.startsWith("data:image")) {

                                    val base64Data = imageData.substringAfter("base64,")
                                    Base64.decode(base64Data, Base64.DEFAULT)
                                } else {
                                    imageData
                                }

                                Glide.with(imageView)
                                    .load(imageSource)
                                    .listener(object : RequestListener<Drawable> {
                                        override fun onLoadFailed(
                                            e: GlideException?,
                                            model: Any?,
                                            target: Target<Drawable>,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            removeProgressBar(progressBar, progressBarHost)
                                            showSketchLoadError("Failed to load sketch image.", imageView)
                                            return false
                                        }

                                        override fun onResourceReady(
                                            resource: Drawable,
                                            model: Any?,
                                            target: Target<Drawable>,
                                            dataSource: DataSource,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            removeProgressBar(progressBar, progressBarHost)
                                            return false
                                        }
                                    })
                                    .into(imageView)
                            } else {
                                removeProgressBar(progressBar, progressBarHost)
                                showSketchLoadError("Failed to parse sketch data.", imageView)
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            removeProgressBar(progressBar, progressBarHost)
                            showSketchLoadError("Failed to get sketch URL.", imageView)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        removeProgressBar(progressBar, progressBarHost)
                        showSketchLoadError("Error: ${result.exceptionOrNull()?.message}", imageView)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    removeProgressBar(progressBar, progressBarHost)
                    showSketchLoadError("Error: ${e.message}", imageView)
                }
            }
        }
    }

    private fun removeProgressBar(progressBar: ProgressBar, container: ViewGroup) {
        container.removeView(progressBar)
    }

    private fun showSketchLoadError(message: String, imageView: ImageView) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        imageView.setImageResource(R.drawable.ic_broken_image)
    }

    private suspend fun downloadSketchData(urlString: String): SketchData? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Parse JSON
                    val jsonObject = JSONObject(response.toString())
                    SketchData(
                        width = jsonObject.optInt("width", 0),
                        height = jsonObject.optInt("height", 0),
                        strokes = jsonObject.opt("strokes") ?: emptyList<Any>(),
                        png = jsonObject.optString("png", "")
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}