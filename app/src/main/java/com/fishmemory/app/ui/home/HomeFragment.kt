package com.fishmemory.app.ui.home

/**
 * 首页 Fragment：搜索栏、分类 Tab、ViewPager 承载各分类下的文章列表。
 * 职责：搜索/分类 UI 与联动、键盘与滚动动画；列表数据由各 CategoryFragment + SharedArticleViewModel 负责。
 */
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.fishmemory.app.R
import com.fishmemory.app.databinding.FragmentHomeBinding
import com.fishmemory.app.ui.home.category.CategoryPageFragment
import com.fishmemory.app.ui.home.category.CategoryPagerAdapter
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var searchBarHeight = 0

    private val categories = listOf(
        "全部",
        "本地",
        "收藏",
        "安卓开发",
        "Java开发",
        "运维"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupCategoryBar()
        bindViewPagerAndCategory()

        setupSearchView()
        setupSearchBarAnimation()
        setupActionButtons()
    }

    // ================= ViewPager =================

    private fun setupViewPager() {
        binding.viewPager.adapter = CategoryPagerAdapter(this, categories)
        binding.viewPager.offscreenPageLimit = 1
    }

    // ================= 搜索栏动画=================

    private fun setupSearchBarAnimation() {
        binding.searchContainer.post {
            searchBarHeight = binding.searchContainer.height
        }

        binding.appBarLayout.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->

                if (searchBarHeight == 0) return@OnOffsetChangedListener

                val offset = abs(verticalOffset)
                val progress =
                    (offset / searchBarHeight.toFloat()).coerceIn(0f, 1f)

                // 缩放
                val scale = 1f - 0.15f * progress
                binding.searchContainer.scaleX = scale
                binding.searchContainer.scaleY = scale

                // 上移 + 渐隐
                binding.searchContainer.translationY = -offset * 0.6f
                binding.searchContainer.alpha = 1f - progress

                val buttonSize = binding.listenButton.width.toFloat()

                // 听按钮左移
                binding.listenButton.translationX = -progress * buttonSize

                // 折叠搜索按钮
                binding.miniSearchButton.apply {
                    translationX = (1f - progress) * buttonSize
                    alpha = progress
                    isClickable = progress > 0.9f
                }
            }
        )
    }

    private fun setupActionButtons() {
        binding.miniSearchButton.setOnClickListener {
            expandSearch()
        }
    }

    // ================= 搜索（转发给当前页） =================

    private fun setupSearchView() {
        val searchEditText = binding.searchEditText

        binding.searchButton.setOnClickListener {
            dispatchSearch(searchEditText.text.toString())
            hideKeyboard()
        }

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                dispatchSearch(searchEditText.text.toString())
                hideKeyboard()
                true
            } else false
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                dispatchSearch(s?.toString().orEmpty())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun dispatchSearch(query: String) {
        currentPage()?.onSearch(query)
    }

    private fun expandSearch() {
        binding.appBarLayout.setExpanded(true, true)
        binding.searchEditText.post {
            binding.searchEditText.requestFocus()
            showKeyboard()
        }
    }

    private fun showKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }

    // ================= 分类栏 =================

    private fun setupCategoryBar() {
        binding.categoryContainer.removeAllViews()

        categories.forEachIndexed { index, title ->
            val tv = TextView(requireContext()).apply {
                text = title
                textSize = 16f
                setPadding(32, 16, 32, 8)
                paint.isFakeBoldText = index == 0
                setTextColor(
                    if (index == 0)
                        ContextCompat.getColor(requireContext(), R.color.app_primary)
                    else
                        ContextCompat.getColor(requireContext(), R.color.gray)
                )
                setOnClickListener {
                    binding.viewPager.setCurrentItem(index, true)
                }
            }

            val indicator = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    6
                )
                setBackgroundResource(R.drawable.category_indicator)
                visibility = if (index == 0) View.VISIBLE else View.INVISIBLE
            }

            val wrapper = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(tv)
                addView(indicator)
            }

            binding.categoryContainer.addView(wrapper)
        }
    }

    private fun bindViewPagerAndCategory() {
        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateCategoryUI(position)
                }
            }
        )
    }

    private fun updateCategoryUI(selectedIndex: Int) {
        for (i in 0 until binding.categoryContainer.childCount) {

            val wrapper = binding.categoryContainer.getChildAt(i) as LinearLayout
            val tv = wrapper.getChildAt(0) as TextView
            val indicator = wrapper.getChildAt(1)

            val selected = i == selectedIndex

            tv.setTextColor(
                if (selected)
                    ContextCompat.getColor(requireContext(), R.color.app_primary)
                else
                    ContextCompat.getColor(requireContext(), R.color.gray)
            )
            tv.paint.isFakeBoldText = selected
            indicator.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        }

        scrollCategoryToCenter(selectedIndex)
    }

    private fun scrollCategoryToCenter(index: Int) {
        val wrapper = binding.categoryContainer.getChildAt(index)
        val scrollView = binding.categoryContainerWrapper

        val scrollX =
            wrapper.left - (scrollView.width - wrapper.width) / 2

        scrollView.smoothScrollTo(scrollX, 0)
    }

    // ================= 当前页（用于搜索转发） =================

    private fun currentPage(): CategoryPageFragment? {
        val index = binding.viewPager.currentItem
        return childFragmentManager.findFragmentByTag("f$index")
                as? CategoryPageFragment
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
