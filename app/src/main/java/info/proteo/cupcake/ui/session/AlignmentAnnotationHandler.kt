package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import info.proteo.cupcake.R
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.data.repository.AnnotationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AlignmentAnnotationHandler(
    private val context: Context,
    private val annotationRepository: AnnotationRepository,
    private val baseUrl: String
) {
    companion object {
        private const val TAG_SEGMENT_IMAGE = "segmentAlignmentImage"
    }

    fun displayAlignment(
        annotation: Annotation,
        imageView: ImageView,
        imageContainerView: View
    ) {
        val actualImageContainer = imageContainerView as? ViewGroup

        actualImageContainer?.let { container ->
            for (i in container.childCount - 1 downTo 0) {
                val child = container.getChildAt(i)
                if (child.tag == TAG_SEGMENT_IMAGE) {
                    container.removeViewAt(i)
                }
            }
        }


        imageView.setImageDrawable(null)
        imageView.visibility = View.GONE




        CoroutineScope(Dispatchers.IO).launch {
            var mainBitmap: Bitmap? = null
            val segmentBitmaps = mutableListOf<Bitmap>()
            var overallSuccess = false
            var errorMsg: String? = null

            try {
                val annotationJsonString = annotation.annotation
                if (annotationJsonString.isNullOrBlank()) {
                    errorMsg = "Annotation data is empty."
                } else {
                    val annotationJson = JSONObject(annotationJsonString)

                    val mainImageDataURL: String? = annotationJson.optString("dataURL", null)
                    if (mainImageDataURL?.startsWith("data:image/png;base64,") == true) {
                        try {
                            mainBitmap = decodeDataURL(mainImageDataURL)
                            overallSuccess = true
                        } catch (e: Exception) {
                            errorMsg = "Failed to load main alignment image: ${e.message}"
                        }
                    }

                    val extractedSegments = annotationJson.optJSONArray("extractedSegments")
                    if (extractedSegments != null) {
                        for (i in 0 until extractedSegments.length()) {
                            val segment = extractedSegments.optJSONObject(i)
                            val segmentDataURL = segment?.optString("dataURL", null)
                            if (segmentDataURL?.startsWith("data:image/png;base64,") == true) {
                                try {
                                    val bmp = decodeDataURL(segmentDataURL)
                                    segmentBitmaps.add(bmp)
                                    overallSuccess = true
                                } catch (e: Exception) {
                                    // Individual segment decode error
                                }
                            }
                        }
                    }
                    if (!overallSuccess && errorMsg == null) {
                        errorMsg = "No valid alignment image found in annotation data."
                    }
                }

                withContext(Dispatchers.Main) {


                    if (mainBitmap != null) {
                        imageView.setImageBitmap(mainBitmap)
                        imageView.visibility = View.VISIBLE
                    } else {
                        imageView.visibility = View.GONE
                    }

                    actualImageContainer?.let { container ->
                        segmentBitmaps.forEach { bmp ->
                            val newSegmentImageView = ImageView(context).apply {
                                tag = TAG_SEGMENT_IMAGE
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                ).also { params ->
                                    if (container is LinearLayout) {
                                        (params as LinearLayout.LayoutParams).gravity = Gravity.CENTER_HORIZONTAL
                                    }
                                }
                                setImageBitmap(bmp)
                                adjustViewBounds = true
                                scaleType = ImageView.ScaleType.FIT_CENTER
                            }
                            container.addView(newSegmentImageView)
                        }
                    }

                    if (!overallSuccess) {
                        showAlignmentLoadError(errorMsg ?: "Failed to load alignment images.", imageView)
                    } else if (mainBitmap == null && segmentBitmaps.isEmpty()) {
                        showAlignmentLoadError(errorMsg ?: "No images to display.", imageView)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {

                    showAlignmentLoadError("Error processing alignment: ${e.message}", imageView)
                }
            }
        }
    }

    private fun decodeDataURL(dataURL: String): Bitmap {
        val base64Data = dataURL.substring(dataURL.indexOf(",") + 1)
        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            ?: throw IllegalArgumentException("Failed to decode Base64 image data from dataURL.")
    }

    private fun showAlignmentLoadError(message: String, imageView: ImageView) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        imageView.setImageResource(R.drawable.ic_broken_image)
        imageView.visibility = View.VISIBLE
    }
}