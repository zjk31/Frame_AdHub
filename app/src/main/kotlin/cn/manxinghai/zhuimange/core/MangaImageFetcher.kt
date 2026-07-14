package cn.manxinghai.zhuimange.core

import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.decode.DataSource
import coil3.request.Options
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.FileSystem
import java.util.concurrent.TimeUnit

/**
 * Coil 3 自定义 Fetcher — 拦截漫画图片 URL，下载加密数据并 XOR 解密后返回。
 */
class MangaImageFetcher(
    private val url: String,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful || response.body == null) {
            throw Exception("Failed to fetch: ${response.code}")
        }

        val encrypted = response.body!!.bytes()
        val decrypted = MangaImageDecryptor.restoreIfNeeded(encrypted)

        val source = Buffer().write(decrypted)
        val imageSource = coil3.decode.ImageSource(
            source = source,
            fileSystem = FileSystem.SYSTEM
        )
        return SourceFetchResult(
            source = imageSource,
            mimeType = "image/webp",
            dataSource = DataSource.NETWORK
        )
    }

    class Factory : Fetcher.Factory< String> {
        override fun create(
            data: String,
            options: Options,
            imageLoader: coil3.ImageLoader
        ): Fetcher? {
            if (data.contains("pan.manxinghai.cn") && data.endsWith(".webp")) {
                return MangaImageFetcher(data, options)
            }
            return null
        }
    }

    companion object {
        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

