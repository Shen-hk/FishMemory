package com.fishmemory.app.ui.publish.richtext.core.model

import java.util.concurrent.atomic.AtomicLong

/**
 * Block 唯一 ID 生成器。
 * 保证同一进程内 ID 唯一，用于 Adapter stableIds 与查找。
 * 为什么“不需要”的情况多：
 * Android 的 RecyclerView 操作、EditText 输入、Adapter 更新，全部必须在主线程完成。这意味着 nextId() 的调用通常是串行的（一个接一个），不会出现两个线程同时抢着生成 ID 的情况。
 *
 * 为什么“有必要考虑”：
 * 异步初始化/后台加载：如果你在子线程解析一个巨大的 JSON 草稿，并批量生成 Block，此时就有多线程风险。
 *
 * 工具类的普适性：BlockIdGenerator 是一个 object（单例）。作为底层基础组件，你无法预知未来的开发者会不会在协程或子线程里调用它。
 *
 * 内化的“防御性编程”：优秀的架构师会假设“环境是不安全的”。使用 AtomicLong 的成本极低（性能损耗微乎其微），但它能从根源上消除 Bug 隐患。
 */
object BlockIdGenerator {

    // 使用原子长整型，确保多线程下操作的原子性和可见性
    private val counter = AtomicLong(0)

    fun nextId(): String {
        // incrementAndGet() 是原子操作，相当于线程安全的 ++counter
        return "b_${System.nanoTime()}_${counter.incrementAndGet()}"
    }
}

/**
 * 数据层：EditorBlock 列表的增删查。
 * Adapter 不直接修改此列表，由外部（Activity/Coordinator）调用后通知 Adapter 局部刷新。
 */
class EditorBlockList {

    private val blocks = mutableListOf<EditorBlock>()
    //防御性编程封装
    fun getBlocks(): List<EditorBlock> = blocks.toList()

    fun insertBlockAfter(id: String, block: EditorBlock) {
        val index = blocks.indexOfFirst { it.id == id }
        if (index < 0) return
        blocks.add(index + 1, block)
    }

    /** 在指定块之后批量插入，保持顺序。 */
    fun insertBlocksAfter(id: String, newBlocks: List<EditorBlock>) {
        val index = blocks.indexOfFirst { it.id == id }
        if (index < 0) return
        blocks.addAll(index + 1, newBlocks)
    }

    fun removeBlock(id: String): Boolean {
        val index = blocks.indexOfFirst { it.id == id }
        if (index < 0) return false
        blocks.removeAt(index)
        return true
    }

    fun findBlock(id: String): EditorBlock? = blocks.find { it.id == id }

    fun getBlockPosition(id: String): Int = blocks.indexOfFirst { it.id == id }

    /** 在指定位置插入块，用于 setBlocks 初始化或批量插入。 */
    fun insertAt(index: Int, block: EditorBlock) {
        blocks.add(index.coerceIn(0, blocks.size), block)
    }

    /** 清空并替换为给定列表（如 setBlocks 时）。 */
    fun replaceAll(newBlocks: List<EditorBlock>) {
        blocks.clear()
        blocks.addAll(newBlocks)
    }

    fun clear() {
        blocks.clear()
    }

    fun isEmpty(): Boolean = blocks.isEmpty()
}
