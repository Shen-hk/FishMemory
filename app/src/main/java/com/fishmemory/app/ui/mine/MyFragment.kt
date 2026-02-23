package com.fishmemory.app.ui.mine

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fishmemory.app.R
import com.fishmemory.app.ui.publish.localarticlelist.LocalArticleListActivity

class MyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_my, container, false)
        
        // 添加点击事件
        setupClickListeners(root)
        
        return root
    }
    
    private fun setupClickListeners(root: View) {
        // 本地文章管理
        root.findViewById<View>(R.id.btnLocalArticleManage)?.setOnClickListener {
            Log.d("MyFragment", "点击了本地文章管理")
            startActivity(LocalArticleListActivity.createIntent(requireContext()))
        }
    }
}