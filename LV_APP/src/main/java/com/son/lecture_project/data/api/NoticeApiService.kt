package com.son.lecture_project.data.api

import com.son.lecture_project.data.model.Notice
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface NoticeApiService {

    @GET("notices")
    suspend fun getNotices(
        @Header("Authorization") token: String
    ): Response<List<Notice>>
}
