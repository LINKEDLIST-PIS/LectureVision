package com.son.lecture_project.data.api

import com.son.lecture_project.data.local.TokenManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://cm838.myasustor.com:5445/"
    
    // 기본 모델 서버 주소
    private const val DEFAULT_MODEL_SERVER_URL = "http://cm838.myasustor.com:8000/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 모델 서버용 변수들 (동적 URL 지원을 위해 lazy 대신 커스텀 게터 사용)
    private var currentModelUrl: String? = null
    private var _modelApiService: ModelApiService? = null

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
    
    // 동적으로 URL을 체크하여 ApiService 반환
    val modelApiService: ModelApiService
        get() {
            // 저장된 커스텀 URL 확인
            val savedUrl = TokenManager.getModelServerUrl()
            // 디버그 모드가 켜져있고 커스텀 URL이 있다면 사용, 아니면 기본값 사용
            val targetUrl = if (TokenManager.isDebugMode() && !savedUrl.isNullOrEmpty()) {
                // URL 형식이 올바른지 간단히 체크 (http 포함 여부 등은 Retrofit이 체크하지만 여기선 그냥 사용)
                if (!savedUrl.endsWith("/")) "$savedUrl/" else savedUrl
            } else {
                DEFAULT_MODEL_SERVER_URL
            }

            // URL이 변경되었거나 아직 생성되지 않았다면 재생성
            if (_modelApiService == null || currentModelUrl != targetUrl) {
                synchronized(this) {
                    if (_modelApiService == null || currentModelUrl != targetUrl) {
                        val newRetrofit = Retrofit.Builder()
                            .baseUrl(targetUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                        _modelApiService = newRetrofit.create(ModelApiService::class.java)
                        currentModelUrl = targetUrl
                    }
                }
            }
            return _modelApiService!!
        }
}
