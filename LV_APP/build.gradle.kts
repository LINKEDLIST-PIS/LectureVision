plugins {
    // 앱 모듈에 적용할 플러그인들입니다.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // toml 파일에 정의된 이름인 'kotlin-compose'로 수정합니다.
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.son.lecture_project"
    // 가장 안정적인 최신 SDK 버전인 34를 사용합니다.
    compileSdk = 35

    defaultConfig {
        applicationId = "com.son.lecture_project"
        minSdk = 24
        // targetSdk는 compileSdk와 맞춰줍니다.
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // buildFeatures를 하나로 합쳐서 정리합니다.
    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
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

    // composeOptions를 추가하여 컴파일러 버전을 명시해주는 것이 안정적입니다.
    composeOptions {
        // 이 버전은 프로젝트의 코틀린 버전에 따라 달라질 수 있습니다.
        // 만약 경고가 뜨면 스튜디오가 추천하는 버전으로 수정하세요.
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    // 기존에 사용하시던 의존성 목록입니다.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

