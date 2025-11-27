package com.son.lecture_project.data.model

import com.google.gson.annotations.SerializedName

data class Ticket(
    // 서버 응답 필드명 매칭을 위해 다양한 케이스 추가
    @SerializedName("ticket_id", alternate = ["id", "ticketId", "_id", "ticket"])
    val ticketId: String? = null,
    
    @SerializedName("user_id", alternate = ["userId", "user"])
    val userId: String? = null,
    
    @SerializedName("created_at", alternate = ["createdAt", "created"])
    val createdAt: String? = null,
    
    @SerializedName("expires_at", alternate = ["expiresAt", "expires"])
    val expiresAt: String? = null,
    
    @SerializedName("is_validated", alternate = ["isValidated", "validated"])
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
