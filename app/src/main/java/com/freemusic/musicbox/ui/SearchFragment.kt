package com.freemusic.musicbox.ui

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.freemusic.musicbox.R
import com.google.android.material.tabs.TabLayout


class SearchFragment : Fragment(), FragmentActions {

    private lateinit var searchView: SearchView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager

    private val onPageChangeListener = object: ViewPager.OnPageChangeListener {
        override fun onPageSelected(position: Int) {
            currentSearchTargetFragment.onPageChange(searchView.query.toString())
        }

        override fun onPageScrollStateChanged(state: Int) {}
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    }

    private val onQueryTextListener = object: SearchView.OnQueryTextListener {
        override fun onQueryTextChange(newText: String?): Boolean {
            if (newText != null)
                currentSearchTargetFragment.onQueryTextChange(newText)
            return true
        }

        override fun onQueryTextSubmit(query: String?): Boolean {
            val imm = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(view?.windowToken, 0)
            searchView.clearFocus()

            if (query != null)
                currentSearchTargetFragment.onQueryTextSubmit(query)
            return true
        }
    }

    inner class ViewPagerAdapter(fm: FragmentManager): FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int {
            return 2
        }

        override fun getItem(position: Int): Fragment {
            return when(position) {
                0 -> SearchCatalogFragment()
                1 -> SearchYoutubeFragment()
                else -> throw RuntimeException()
            }
        }

        override fun getPageTitle(position: Int): CharSequence? {
            val stringId = when (position) {
                0 -> R.string.label_search_catalog
                1 -> R.string.label_search_youtube
                else -> null
            }
            return if (stringId != null) resources.getString(stringId)
            else null
        }
    }

    private val currentSearchTargetFragment: SearchTargetFragment
        get() {
            val fragment = childFragmentManager.findFragmentByTag(
                "android:switcher:" + R.id.fragment_search_vp_targets + ":" + viewPager.currentItem)
            return fragment as SearchTargetFragment
        }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchView = view.findViewById(R.id.fragment_search_sv_search)
        searchView.isIconified = true

        viewPager = view.findViewById(R.id.fragment_search_vp_targets)
        viewPager.adapter = ViewPagerAdapter(childFragmentManager)
        viewPager.addOnPageChangeListener(onPageChangeListener)

        tabLayout = view.findViewById(R.id.fragment_search_tl_targets)
        tabLayout.setupWithViewPager(viewPager)

        searchView.setOnQueryTextListener(onQueryTextListener)

        Handler(context?.mainLooper).post {
            val queryText = searchView.query.toString()
            currentSearchTargetFragment.onPageChange(queryText)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager.removeOnPageChangeListener(onPageChangeListener)
    }


    override fun onBackPressed(): Boolean {
        val currentChildFragment = currentSearchTargetFragment
        return if (currentChildFragment is FragmentActions)
            currentChildFragment.onBackPressed()
        else
            super.onBackPressed()
    }

}


abstract class SearchTargetFragment: Fragment() {
    abstract fun onQueryTextChange(query: String)

    abstract fun onQueryTextSubmit(query: String)

    abstract fun onPageChange(query: String)
}
