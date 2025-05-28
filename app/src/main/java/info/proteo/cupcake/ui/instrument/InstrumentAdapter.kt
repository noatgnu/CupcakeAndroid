package info.proteo.cupcake.ui.instrument

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.data.remote.model.instrument.Instrument
import info.proteo.cupcake.databinding.ItemInstrumentBinding

class InstrumentAdapter : ListAdapter<Instrument, InstrumentAdapter.InstrumentViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstrumentViewHolder {
        val binding = ItemInstrumentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return InstrumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InstrumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InstrumentViewHolder(private val binding: ItemInstrumentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(instrument: Instrument) {
            binding.instrumentName.text = instrument.instrumentName
            binding.instrumentDescription.text = instrument.instrumentDescription
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