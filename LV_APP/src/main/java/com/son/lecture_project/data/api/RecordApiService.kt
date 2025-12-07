package com.son.lecture_project.data.api

import com.son.lecture_project.data.model.Upload
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface RecordApiService {

    // 명세서 기준: GET /uploads?skip=0&limit=50
    @GET("uploads")
    suspend fun getRecords(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100 // Paging 처리 전까지 넉넉하게 100개 로드
    ): Response<List<Upload>>
}
