package info.proteo.cupcake.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.databinding.ItemSettingsCategoryBinding

class SettingsCategoryAdapter(
    private val categories: List<SettingsCategory>,
    private val onCategoryClick: (SettingsCategory) -> Unit
) : RecyclerView.Adapter<SettingsCategoryAdapter.ViewHolder>() {

    data class SettingsCategory(val id: Int, val title: String, val icon: Int)

    class ViewHolder(val binding: ItemSettingsCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSettingsCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.binding.apply {
            categoryTitle.text = category.title
            categoryIcon.setImageResource(category.icon)
            root.setOnClickListener { onCategoryClick(category) }
        }
    }

    override fun getItemCount() = categories.size
}