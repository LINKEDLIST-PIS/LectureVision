package com.son.lecture_project.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val MAIN_API_BASE_URL = "https://cm838.myasustor.com:5445"
    // Note: The model server URL is assumed based on the provided documentation.
    // This might need to be changed to the actual public URL.
    private const val MODEL_SERVER_BASE_URL = "http://cm838.myasustor.com:8000"

    val mainApiService: MainApiService by lazy {
        Retrofit.Builder()
            .baseUrl(MAIN_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MainApiService::class.java)
    }

    val modelApiService: ModelApiService by lazy {
        Retrofit.Builder()
            .baseUrl(MODEL_SERVER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ModelApiService::class.java)
    }
}
