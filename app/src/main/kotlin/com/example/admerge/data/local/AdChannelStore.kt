package com.example.admerge.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.admerge.domain.model.AdChannel
import com.example.admerge.domain.repository.AdChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [AdChannelRepository] 的 SharedPreferences 实现。
 */
class AdChannelStore(context: Context) : AdChannelRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ad_channel", Context.MODE_PRIVATE)

    private val _channelFlow = MutableStateFlow(loadFromPrefs())
    override val channelFlow: Flow<AdChannel> = _channelFlow.asStateFlow()

    override val cachedChannel: AdChannel get() = _channelFlow.value

    override suspend fun updateChannel(channel: AdChannel) {
        prefs.edit().putInt(KEY_AD_CHANNEL, channel.code).apply()
        _channelFlow.value = channel
    }

    private fun loadFromPrefs(): AdChannel {
        val code = prefs.getInt(KEY_AD_CHANNEL, AdChannel.Umeng.code)
        return AdChannel.fromCode(code)
    }

    companion object {
        private const val KEY_AD_CHANNEL = "ad_channel"
    }
}
