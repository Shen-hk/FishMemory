package com.fishmemory.app.ui.publish.richtext.business.link

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder


/**
 * 链接元数据：OGP 抓取结果
 */
data class LinkMeta(
    val title: String,
    val description: String,
    val imageUrl: String?
)

/**
 * 轻量 OGP 抓取器：
 * - 不引入第三方 HTML 解析库（避免依赖膨胀）
 * - 仅解析常见的 og:title/og:description/og:image
 * - 失败时返回空字段，由 UI 做兜底展示
 */
object LinkMetaFetcher {
    private const val TAG = "LinkMetaFetcher"

    /**
     * 标准化 URL：自动添加协议前缀并修复 Punycode 编码
     */
    private fun normalizeUrl(url: String): String {
        // 去除首尾空格
        val trimmed = url.trim()

        // 尝试解码 Punycode（如 .xn--）
        val decodedUrl = try {
            URLDecoder.decode(trimmed, "UTF-8")
        } catch (e: Exception) {
            trimmed
        }

        return when {
            // 已有协议前缀
            decodedUrl.startsWith("http://", ignoreCase = true) ||
            decodedUrl.startsWith("https://", ignoreCase = true) -> decodedUrl

            // www. 开头 → https://
            decodedUrl.startsWith("www.", ignoreCase = true) -> "https://$decodedUrl"

            // 其他 → 默认 https://
            else -> "https://$decodedUrl"
        }
    }

    /**
     * 抓取链接元数据（IO 线程执行）
     */
    suspend fun fetch(url: String): LinkMeta = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedUrl = normalizeUrl(url)
            Log.d(TAG, "🔗 开始解析链接：$normalizedUrl")

            val conn = URL(normalizedUrl).openConnection() as HttpURLConnection
            try {
                conn.apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    instanceFollowRedirects = true
                    //伪装成浏览器
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    setRequestProperty("Connection", "close")
                }

                val responseCode = conn.responseCode
                Log.d(TAG, "📡 响应码：$responseCode")

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "❌ HTTP 错误：$responseCode")
                    return@withContext LinkMeta(title = "", description = "", imageUrl = null)
                }

                // 获取输入流（失败时用 errorStream）
                val inputStream = try {
                    conn.inputStream
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 无法获取输入流：${e.message}")
                    conn.errorStream
                }

                // 读取 HTML（限制 128KB，避免超大页面拖垮解析）
                val html = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { br ->
                    val sb = StringBuilder()
                    var line: String?
                    var total = 0
                    while (br.readLine().also { line = it } != null && total < 128 * 1024) {
                        val l = line ?: break
                        sb.append(l).append('\n')
                        total += l.length
                    }
                    Log.d(TAG, "📄 读取 HTML 大小：${total / 1024}KB")
                    sb.toString()
                }

                val meta = parseOg(html)
                Log.d(TAG, "✅ 解析成功：title=${meta.title}, desc=${meta.description}, image=${meta.imageUrl}")
                meta
            } finally {
                conn.disconnect()
            }
        }.getOrElse { e ->
            Log.e(TAG, "❌ 解析失败：${e.message}", e)
            LinkMeta(title = "", description = "", imageUrl = null)
        }
    }

    /**
     * 解析 OGP 元数据
     */
    private fun parseOg(html: String): LinkMeta {
        // 查找指定 property 的 meta 标签
        fun findMeta(property: String): String {
            val regex = Regex(
                "<meta[^>]+property=[\"']$property[\"'][^>]+content=[\"']([^\"']+)[\"'][^>]*>",
                setOf(RegexOption.IGNORE_CASE)
            )
            return regex.find(html)?.groupValues?.getOrNull(1).orEmpty()
        }

        // 优先 og:title，退化到 <title>
        val title = findMeta("og:title").ifBlank {
            Regex("<title>([\\s\\S]*?)</title>", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        }
        val desc = findMeta("og:description")
        val image = findMeta("og:image").ifBlank { null }
        return LinkMeta(title = title, description = desc, imageUrl = image)
    }
}
