package com.son.lecture_project.data.api

import com.son.lecture_project.data.model.Ticket
import com.son.lecture_project.data.model.TicketValidationResponse
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface TicketApiService {


    @POST("tickets/issue")
    suspend fun createTicket(
        @Header("Authorization") token: String
    ): Response<Ticket>

    @POST("tickets/validate")
    suspend fun validateTicket(
        @Header("Authorization") token: String,
        @Query("ticket") ticketId: String
    ): Response<TicketValidationResponse>
}
