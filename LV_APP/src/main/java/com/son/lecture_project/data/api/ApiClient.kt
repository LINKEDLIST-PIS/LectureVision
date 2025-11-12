package com.son.lecture_project.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // ⚠️ 포트 포함 주소 반드시 유지
    private const val BASE_URL = "https://cm838.myasustor.com:5445/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)   // 연결 타임아웃
        .readTimeout(30, TimeUnit.SECONDS)      // 읽기 타임아웃
        .writeTimeout(30, TimeUnit.SECONDS)     // 쓰기 타임아웃
        .retryOnConnectionFailure(true)         // 연결 실패 시 재시도
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
