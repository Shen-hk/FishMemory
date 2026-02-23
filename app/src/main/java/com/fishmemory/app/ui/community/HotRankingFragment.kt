package com.fishmemory.app.ui.community

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.fishmemory.app.databinding.FragmentHotRankingBinding
import com.fishmemory.app.data.model.ArticleData
import com.fishmemory.app.data.model.HotRankItem
import com.fishmemory.app.shared.viewmodel.SharedArticleViewModel

class HotRankingFragment : Fragment() {

    private var _binding: FragmentHotRankingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HotRankAdapter
    private lateinit var sharedViewModel: SharedArticleViewModel
    private var isRefreshing = false

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        Log.e("HotFragment", "🧩 onAttach")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("HotFragment", "🧩 onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.e("HotFragment", "🔥 onCreateView")
        _binding = FragmentHotRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.e("HotFragment", "🔥 onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        Log.e("HotFragment", "📦 before ViewModelProvider")

        sharedViewModel =
            ViewModelProvider(requireActivity())[SharedArticleViewModel::class.java]

        Log.e("HotFragment", "📦 ViewModel acquired: $sharedViewModel")

        sharedViewModel.articles.observe(viewLifecycleOwner) { articleList ->
            Log.e(
                "HotFragment",
                "👀 observe triggered, list = $articleList, size=${articleList?.size}"
            )

            if (!articleList.isNullOrEmpty()) {
                val hotRankItems = convertToHotRankItems(articleList)
                Log.e("HotFragment", "🔥 convert done, size=${hotRankItems.size}")
                adapter.dataList = hotRankItems
            }

            binding.swipeRefresh.isRefreshing = false
            isRefreshing = false
        }

        binding.swipeRefresh.setOnRefreshListener {
            Log.e("HotFragment", "🔄 swipe refresh triggered")
            if (!isRefreshing) {
                isRefreshing = true
                sharedViewModel.articles.value?.let { currentList ->
                    Log.e(
                        "HotFragment",
                        "🔄 force setArticles size=${currentList.size}"
                    )
                    sharedViewModel.setArticles(currentList)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        Log.e("HotFragment", "🧱 setupRecyclerView")
        adapter = HotRankAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
    }

    private fun convertToHotRankItems(articles: List<ArticleData>): List<HotRankItem> {
        Log.e("HotFragment", "⚙️ convertToHotRankItems start")
        return articles
            .mapIndexed { _, article ->
                val hotValue = (500..10000).random()
                HotRankItem(
                    id = article.id,
                    title = article.title,
                    hotValue = hotValue,
                    rank = 0
                )
            }
            .sortedByDescending { it.hotValue }
            .mapIndexed { index, item ->
                item.copy(rank = index + 1)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.e("HotFragment", "💀 onDestroyView")
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        Log.e("HotFragment", "💀 onDetach")
    }
}
