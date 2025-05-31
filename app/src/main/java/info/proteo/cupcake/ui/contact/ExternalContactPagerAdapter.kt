package info.proteo.cupcake.ui.contact

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ExternalContactPagerAdapter(
    fragment: Fragment,
    private val supportInfoId: Int
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ContactListFragment.newInstance(supportInfoId, "vendor")
            1 -> ContactListFragment.newInstance(supportInfoId, "manufacturer")
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}