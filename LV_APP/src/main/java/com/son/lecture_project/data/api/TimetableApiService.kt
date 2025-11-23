package com.son.lecture_project.data.api

import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.data.model.ClassScheduleRequest
import retrofit2.Response
import retrofit2.http.*

interface TimetableApiService {

    // GET은 복수형이 맞을 확률이 높으므로 그대로 유지
    @GET("timetables/user/{userId}")
    suspend fun getUserTimetable(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<List<ClassSchedule>>

    // POST/PUT/DELETE도 RESTful 표준에 따라 복수형 'timetables'를 사용 시도
    // (이전 시도: 'timetable' 단수형 -> 404 에러 발생)
    // (맨 처음: 'api/v1/timetables' -> 404 에러 발생)
    
    @POST("timetables")
    suspend fun createClass(
        @Header("Authorization") token: String,
        @Body request: ClassScheduleRequest
    ): Response<ClassSchedule>

    @PUT("timetables/{id}")
    suspend fun updateClass(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: ClassScheduleRequest
    ): Response<ClassSchedule>

    @DELETE("timetables/{id}")
    suspend fun deleteClass(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>
}
