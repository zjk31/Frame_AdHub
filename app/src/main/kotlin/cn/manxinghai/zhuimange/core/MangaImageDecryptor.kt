package cn.manxinghai.zhuimange.core

import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * 漫画章节图片 XOR 解密器。
 * 图片在 CDN 上以 8 字节 XOR 加密存储，key 通过网络抓包反推得到。
 */
object MangaImageDecryptor {

    /** 12 字节 XOR 密钥 */
    private val KEY = byteArrayOf(
        0x5A.toByte(), 0x3F.toByte(), 0x9E.toByte(), 0xC2.toByte(),
        0x71.toByte(), 0xA8.toByte(), 0x4B.toByte(), 0xD6.toByte(),
        0xE0.toByte(), 0xF3.toByte(), 0x1C.toByte(), 0x87.toByte()
    )

    fun decrypt(data: ByteArray): ByteArray {
        for (i in data.indices) {
            data[i] = (data[i].toInt() xor KEY[i % KEY.size].toInt()).toByte()
        }
        return data
    }

    /**
     * 解密后返回 InputStream。
     */
    fun decryptToStream(data: ByteArray): InputStream {
        return ByteArrayInputStream(decrypt(data))
    }
}
