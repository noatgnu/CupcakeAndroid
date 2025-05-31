package info.proteo.cupcake.ui.contact

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HeaderAdapter(private val title: String) :
    RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(title)
    }

    override fun getItemCount(): Int = 1

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<TextView>(android.R.id.text1)

        fun bind(title: String) {
            textView.text = title
            textView.setTypeface(null, Typeface.BOLD)
            textView.setPadding(16, 24, 16, 8)
        }
    }
}