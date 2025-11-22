package com.son.lecture_project.data.model

import com.google.gson.annotations.SerializedName

data class Ticket(
    @SerializedName("ticket_id")
    val ticketId: String? = null,
    @SerializedName("user_id")
    val userId: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("expires_at")
    val expiresAt: String? = null,
    @SerializedName("is_validated")
    val isValidated: Boolean? = null
)

data class TicketRequest(
    @SerializedName("user_id")
    val userId: String
)

data class TicketValidationRequest(
    @SerializedName("ticket_id")
    val ticketId: String
)

data class TicketValidationResponse(
    @SerializedName("is_valid")
    val isValid: Boolean,
    @SerializedName("message")
    val message: String
)
