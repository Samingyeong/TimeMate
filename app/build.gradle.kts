plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.timemate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.timemate"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig 필드 추가 - 네이버 클라우드 플랫폼 API
        buildConfigField("String", "NAVER_CLOUD_CLIENT_ID", "\"${project.findProperty("NAVER_CLOUD_CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "NAVER_CLOUD_CLIENT_SECRET", "\"${project.findProperty("NAVER_CLOUD_CLIENT_SECRET") ?: ""}\"")

        // BuildConfig 필드 추가 - 네이버 개발자 센터 API
        buildConfigField("String", "NAVER_DEV_CLIENT_ID", "\"${project.findProperty("NAVER_DEV_CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "NAVER_DEV_CLIENT_SECRET", "\"${project.findProperty("NAVER_DEV_CLIENT_SECRET") ?: ""}\"")

        // BuildConfig 필드 추가 - OpenWeather API
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"${project.findProperty("OPENWEATHER_API_KEY") ?: ""}\"")

        // BuildConfig 필드 추가 - API 엔드포인트
        buildConfigField("String", "NAVER_STATIC_MAP_URL", "\"${project.findProperty("NAVER_STATIC_MAP_URL") ?: ""}\"")
        buildConfigField("String", "NAVER_DIRECTIONS_URL", "\"${project.findProperty("NAVER_DIRECTIONS_URL") ?: ""}\"")
        buildConfigField("String", "NAVER_GEOCODING_URL", "\"${project.findProperty("NAVER_GEOCODING_URL") ?: ""}\"")
        buildConfigField("String", "NAVER_REVERSE_GEOCODING_URL", "\"${project.findProperty("NAVER_REVERSE_GEOCODING_URL") ?: ""}\"")
        buildConfigField("String", "NAVER_LOCAL_SEARCH_URL", "\"${project.findProperty("NAVER_LOCAL_SEARCH_URL") ?: ""}\"")

        // 보안을 위해 BuildConfig만 사용하고 manifestPlaceholders는 제거

        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schemas")

            }
        }
    }

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    // ✅ Room 컴파일러 추가 (Java 기준)
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation ("com.kakao.sdk:v2-user:2.19.0")

    // HTTP 요청을 위한 라이브러리
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // 이미지 로딩 라이브러리 (Glide)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // HTML 파싱 라이브러리 (Jsoup) - 이미지 크롤링용
    implementation("org.jsoup:jsoup:1.16.1")

    // 달력 라이브러리 (Android 기본 CalendarView 사용)
    implementation("com.google.code.gson:gson:2.10.1")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime:2.8.1")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.room.common.jvm)
    implementation(libs.room.runtime.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}