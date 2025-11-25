package com.son.lecture_project

import android.app.Application
import com.son.lecture_project.data.local.TokenManager

/**
 * 앱의 진입점(Entry Point) 클래스.
 *
 * [역할 및 목적]
 * 1. 앱 실행 시 최초 1회 호출되어 전역 상태를 초기화합니다.
 * 2. SharedPreferences 기반의 TokenManager를 초기화하여,
 *    앱 전체에서 로그인 세션 및 사용자 설정을 접근할 수 있도록 합니다.
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 데이터 관리 모듈(TokenManager) 초기화
        // Context를 주입하여 로컬 저장소 접근 권한 부여
        TokenManager.init(this)
    }
}
