package cn.maga.lingdongmanhua.data.repository

import android.content.Context
import android.net.Uri
import cn.maga.lingdongmanhua.data.api.FileUploadApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

/**
 * 文件上传 Repository
 */
class FileUploadRepository(
    private val api: FileUploadApi,
    private val context: Context
) {
    /**
     * 上传头像
     * @param imageUri 图片 URI
     * @return 上传后的头像 URL
     */
    suspend fun uploadAvatar(imageUri: Uri): Result<String> = runCatching {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: return Result.failure(Exception("无法读取图片"))

        val byteArray = inputStream.use { it.readBytes() }
        val mediaType = context.contentResolver.getType(imageUri)?.toMediaTypeOrNull()
            ?: "image/jpeg".toMediaTypeOrNull()

        val requestBody = byteArray.toRequestBody(mediaType)
        val multipartBody = MultipartBody.Part.createFormData("file", "avatar.jpg", requestBody)

        val resp = api.uploadAvatar(multipartBody)
        check(resp.isSuccess) { resp.message ?: "上传失败" }
        resp.data ?: throw Exception("上传返回空")
    }
}
