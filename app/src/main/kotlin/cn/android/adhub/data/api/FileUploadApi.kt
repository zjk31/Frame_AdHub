package cn.android.adhub.data.api

import cn.android.adhub.data.dto.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * 文件上传 API
 */
interface FileUploadApi {

    @Multipart
    @POST("api/user/uploadAvatar")
    suspend fun uploadAvatar(
        @Part file: MultipartBody.Part
    ): ApiResponse<String>
}
