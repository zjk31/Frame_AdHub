package cn.manxinghai.zhuimange.core

/**
 * 漫画图片文件头 XOR 解密器。
 * 只有前 16 字节被混淆，恢复后即可正常解码。
 * 与 Flutter comic_module image_header_restore 完全一致。
 */
object MangaImageDecryptor {

    /** 16 字节 XOR 密钥 */
    private val KEY = byteArrayOf(
        0x5A.toByte(), 0x3F.toByte(), 0x9E.toByte(), 0xC2.toByte(),
        0x71.toByte(), 0xA8.toByte(), 0x4B.toByte(), 0xD6.toByte(),
        0xE0.toByte(), 0xF3.toByte(), 0x1C.toByte(), 0x87.toByte(),
        0x29.toByte(), 0xB5.toByte(), 0x6D.toByte(), 0x02.toByte(),
    )

    /** 只需还原前 N 字节的文件头 */
    private const val HEADER_LEN = 16

    /**
     * 若已为合法图片头则原样返回，否则 XOR 还原前 16 字节。
     */
    fun restoreIfNeeded(data: ByteArray): ByteArray {
        if (data.size < HEADER_LEN) return data
        if (isValidImageHeader(data)) return data
        return restoreHeader(data)
    }

    private fun restoreHeader(data: ByteArray): ByteArray {
        val result = data.copyOf()
        val n = minOf(HEADER_LEN, data.size)
        for (i in 0 until n) {
            result[i] = (data[i].toInt() xor KEY[i % KEY.size].toInt()).toByte()
        }
        return result
    }

    /** 检查是否已是合法的图片文件头 */
    private fun isValidImageHeader(data: ByteArray): Boolean {
        if (data.size < 3) return false
        // JPEG: FF D8 FF
        if (data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() && data[2] == 0xFF.toByte()) return true
        // PNG
        if (data.size >= 8 && data[0] == 0x89.toByte() && data[1] == 0x50.toByte() && data[2] == 0x4E.toByte() && data[3] == 0x47.toByte()) return true
        // GIF
        if (data.size >= 6 && data[0] == 0x47.toByte() && data[1] == 0x49.toByte() && data[2] == 0x46.toByte()) return true
        // BMP
        if (data[0] == 0x42.toByte() && data[1] == 0x4D.toByte()) return true
        // WebP (RIFF)
        if (data.size >= 12 && data[0] == 0x52.toByte() && data[1] == 0x49.toByte() && data[2] == 0x46.toByte() && data[3] == 0x46.toByte()
            && data[8] == 0x57.toByte() && data[9] == 0x45.toByte() && data[10] == 0x42.toByte() && data[11] == 0x50.toByte()) return true
        return false
    }
}
