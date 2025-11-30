package com.son.lecture_project.data.api

import com.son.lecture_project.data.model.MeasureResponse
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Query

interface ModelApiService {

    /**
     * Triggers the people counting process on the model server.
     * @param ticket A one-time ticket obtained from the main API server.
     */
    @POST("/measure")
    suspend fun measure(@Query("ticket") ticket: String): Response<MeasureResponse>
}
