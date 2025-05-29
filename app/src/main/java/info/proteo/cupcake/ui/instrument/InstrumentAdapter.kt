package info.proteo.cupcake.ui.instrument

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.instrument.Instrument
import info.proteo.cupcake.databinding.ItemInstrumentBinding

class InstrumentAdapter(
    private val onItemClicked: (Int) -> Unit // Changed to accept Int (instrumentId)
) : ListAdapter<Instrument, InstrumentAdapter.InstrumentViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstrumentViewHolder {
        val binding = ItemInstrumentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return InstrumentViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: InstrumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InstrumentViewHolder(
        private val binding: ItemInstrumentBinding,
        private val onItemClicked: (Int) -> Unit // Changed to accept Int
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(instrument: Instrument) {
            binding.instrumentName.text = instrument.instrumentName
            binding.instrumentDescription.text = instrument.instrumentDescription

            binding.root.setOnClickListener {
                onItemClicked(instrument.id) // Pass instrument.id
            }

            if (!instrument.image.isNullOrEmpty()) {
                try {
                    val base64Image = instrument.image
                    val base64Content = if (base64Image.startsWith("data:image/png;base64,")) {
                        base64Image.substring("data:image/png;base64,".length)
                    } else {
                        base64Image
                    }

                    val imageBytes = Base64.decode(base64Content, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                    if (bitmap != null) {
                        binding.instrumentImage.setImageBitmap(bitmap)
                        binding.instrumentImage.visibility = View.VISIBLE
                    } else {
                        binding.instrumentImage.setImageResource(R.drawable.ic_error_image)
                        binding.instrumentImage.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    binding.instrumentImage.setImageResource(R.drawable.ic_error_image)
                    binding.instrumentImage.visibility = View.VISIBLE
                }
            } else {
                binding.instrumentImage.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Instrument>() {
            override fun areItemsTheSame(oldItem: Instrument, newItem: Instrument): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Instrument, newItem: Instrument): Boolean {
                return oldItem == newItem
            }
        }
    }
}