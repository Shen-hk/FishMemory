package com.fishmemory.app.ui.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.fishmemory.app.databinding.FragmentMessageBinding
import com.fishmemory.app.shared.viewmodel.MessageViewModel


class MessageFragment : Fragment() {

    private var _binding:FragmentMessageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MessageViewModel by viewModels()

    private lateinit var messageAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter { item ->
            // 这里先留空：以后点进聊天 / 系统通知
            // navigateToDetail(item)
        }

        binding.rvMessage.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = messageAdapter
            itemAnimator = null
        }
    }

    private fun observeViewModel() {
        viewModel.messageList.observe(viewLifecycleOwner) { list ->
            val isEmpty = list.isNullOrEmpty()

            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvMessage.visibility = if (isEmpty) View.GONE else View.VISIBLE

            messageAdapter.submitList(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
