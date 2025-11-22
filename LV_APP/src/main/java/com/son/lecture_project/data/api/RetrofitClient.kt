package com.son.lecture_project.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Base URL 끝에 '/'를 추가하여 상대 경로 매핑 문제 방지
    private const val BASE_URL = "https://cm838.myasustor.com:5445/"
    
    // 모델 서버 URL (가정)
    private const val MODEL_SERVER_URL = "http://cm838.myasustor.com:8000/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val modelRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(MODEL_SERVER_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val instance: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    
    val mainApiService: MainApiService by lazy {
        retrofit.create(MainApiService::class.java)
    }

    val ticketApiService: TicketApiService by lazy {
        retrofit.create(TicketApiService::class.java)
    }

    val noticeApiService: NoticeApiService by lazy {
        retrofit.create(NoticeApiService::class.java)
    }

    val timetableApiService: TimetableApiService by lazy {
        retrofit.create(TimetableApiService::class.java)
    }

    val recordApiService: RecordApiService by lazy {
        retrofit.create(RecordApiService::class.java)
    }
    
    val modelApiService: ModelApiService by lazy {
        modelRetrofit.create(ModelApiService::class.java)
    }
}
