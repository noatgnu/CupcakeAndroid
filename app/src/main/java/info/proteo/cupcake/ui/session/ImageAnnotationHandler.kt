package info.proteo.cupcake.ui.session

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
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

class ImageAnnotationHandler(
    private val context: Context,
    private val annotationRepository: AnnotationRepository,
    private val baseUrl: String
) {

    fun displayImage(annotation: Annotation, imageView: ImageView, progressContainer: ViewGroup) {
        val parentView = imageView.parent as? ViewGroup ?: progressContainer

        val progressBar = ProgressBar(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val progressBarHost = if (progressContainer != parentView && parentView.findViewById<ProgressBar>(progressBar.id) == null) progressContainer else parentView

        val existingProgressBar = progressBarHost.findViewWithTag<ProgressBar>("imageLoadProgressBar")
        existingProgressBar?.let { progressBarHost.removeView(it) }
        progressBar.tag = "imageLoadProgressBar"


        var PBarIndex = if (parentView == progressBarHost) parentView.indexOfChild(imageView) else 0
        if(PBarIndex <0) PBarIndex = 0
        try {
            progressBarHost.addView(progressBar, PBarIndex)
        } catch (e: IllegalStateException){
        }


        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = annotationRepository.getSignedUrl(annotation.id)

                if (result.isSuccess) {
                    val signedToken = result.getOrNull()?.signedToken
                    if (signedToken != null) {
                        val signedUrl = "${baseUrl}/api/annotation/download_signed/?token=$signedToken"

                        withContext(Dispatchers.Main) {
                            Glide.with(imageView)
                                .load(signedUrl)
                                .listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        removeProgressBar(progressBar, progressBarHost)
                                        showImageLoadError("Failed to load image.", imageView)
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
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            removeProgressBar(progressBar, progressBarHost)
                            showImageLoadError("Failed to get image URL.", imageView)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        removeProgressBar(progressBar, progressBarHost)
                        showImageLoadError("Error: ${result.exceptionOrNull()?.message}", imageView)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    removeProgressBar(progressBar, progressBarHost)
                    showImageLoadError("Error: ${e.message}", imageView)
                }
            }
        }
    }

    private fun removeProgressBar(progressBar: ProgressBar, container: ViewGroup) {
        container.removeView(progressBar)
    }

    private fun showImageLoadError(message: String, imageView: ImageView) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        imageView.setImageResource(R.drawable.ic_broken_image)
    }
}