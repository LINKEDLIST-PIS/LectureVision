plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // compose 플러그인은 XML 기반 프로젝트에서는 필수는 아니지만, 유지합니다.
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.son.lecture_project"
    compileSdk = 35 // 안정성을 위해 34 버전으로 조정 (또는 35 유지 가능)

    defaultConfig {
        applicationId = "com.son.lecture_project"
        minSdk = 24
        targetSdk = 35 // compileSdk와 맞춰줍니다.
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // buildFeatures를 하나로 합쳐서 정리합니다.
    buildFeatures {
        viewBinding = true
        dataBinding = true // DataBinding을 사용하지 않는다면 false로 해도 무방합니다.
        compose = true // Compose를 사용하지 않는다면 false로 해도 무방합니다.
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

// ============== 여기가 합쳐진 의존성 블록입니다 ==============
dependencies {

    // --- 안드로이드 기본 및 UI 라이브러리 ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity) // Activity 관련 기능
    implementation(libs.material) // Material Design 컴포넌트 (CardView, Button 등)
    implementation(libs.androidx.constraintlayout) // ConstraintLayout

    // --- 서버 통신(Retrofit) 라이브러리 ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // --- 테스트 관련 라이브러리 ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // --- Jetpack Compose 관련 라이브러리들 (현재 프로젝트에서는 직접 사용되지 않을 수 있음) ---
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
