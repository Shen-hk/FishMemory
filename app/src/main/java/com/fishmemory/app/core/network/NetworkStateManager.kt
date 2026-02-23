package com.fishmemory.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 网络状态管理器：监听网络变化，提供 WiFi/移动网络/离线判断。
 * 需在 Application.onCreate 中调用 init 初始化。
 */
object NetworkStateManager {

    private var connectivityManager: ConnectivityManager? = null

    enum class NetworkStatus {
        OFFLINE,
        MOBILE,
        WIFI
    }

    private val _status = MutableStateFlow(NetworkStatus.OFFLINE)
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * 在 Application.onCreate 中调用，传入 applicationContext。
     */
    fun init(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        connectivityManager?.let { cm ->
            updateStatus(cm)
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    updateStatus(cm)
                }

                override fun onLost(network: Network) {
                    updateStatus(cm)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    updateStatus(cm)
                }
            }
            cm.registerDefaultNetworkCallback(networkCallback!!)
        }
    }

    private fun updateStatus(cm: ConnectivityManager) {
        val activeNetwork = cm.activeNetwork ?: run {
            _status.value = NetworkStatus.OFFLINE
            return
        }
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: run {
            _status.value = NetworkStatus.OFFLINE
            return
        }
        _status.value = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkStatus.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkStatus.MOBILE
            else -> NetworkStatus.OFFLINE
        }
    }

    fun isWifiConnected(): Boolean = _status.value == NetworkStatus.WIFI

    fun isOnline(): Boolean = _status.value != NetworkStatus.OFFLINE
}
