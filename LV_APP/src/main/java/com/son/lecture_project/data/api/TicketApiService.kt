package com.son.lecture_project.data.api

import com.son.lecture_project.data.model.Ticket
import com.son.lecture_project.data.model.TicketValidationResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TicketApiService {

    // 수정됨: api/v1 제거
    @POST("tickets/issue")
    suspend fun createTicket(
        @Header("Authorization") token: String
    ): Response<Ticket>

    // 수정됨: api/v1 제거
    @POST("tickets/validate")
    suspend fun validateTicket(
        @Header("Authorization") token: String,
        @Query("ticket") ticketId: String
    ): Response<TicketValidationResponse>

    // 수정됨: api/v1 제거
    @GET("tickets/user/{userId}")
    suspend fun getUserTicket(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<Ticket>
}
