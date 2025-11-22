package com.son.lecture_project.data.api

import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.data.model.ClassScheduleRequest
import retrofit2.Response
import retrofit2.http.*

interface TimetableApiService {

    @GET("api/v1/timetables/user/{userId}")
    suspend fun getUserTimetable(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<List<ClassSchedule>>

    @POST("api/v1/timetables")
    suspend fun createClass(
        @Header("Authorization") token: String,
        @Body request: ClassScheduleRequest
    ): Response<ClassSchedule>

    @PUT("api/v1/timetables/{id}")
    suspend fun updateClass(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: ClassScheduleRequest
    ): Response<ClassSchedule>

    @DELETE("api/v1/timetables/{id}")
    suspend fun deleteClass(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>
}
