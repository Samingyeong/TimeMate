# 네이버 Directions5 API 설정 가이드

## 1. 네이버 클라우드 플랫폼 계정 생성

1. [네이버 클라우드 플랫폼](https://www.ncloud.com/) 접속
2. 회원가입 및 로그인
3. 콘솔 접속

## 2. API 키 발급

1. **AI·Application Service** → **Maps** → **Directions5** 선택
2. **이용 신청** 클릭
3. 서비스 약관 동의 후 신청
4. **인증키 관리** → **신규 인증키 생성**
5. 다음 정보 입력:
   - 서비스명: TimeMate
   - 서비스 URL: 앱 패키지명 또는 도메인
   - 서비스 환경: Mobile App

## 3. API 키 설정

발급받은 API 키를 앱에 적용:

### NaverDirectionsService.java 수정

```java
// 네이버 클라우드 플랫폼에서 발급받은 API 키
private static final String CLIENT_ID = "발급받은_CLIENT_ID";
private static final String CLIENT_SECRET = "발급받은_CLIENT_SECRET";
```

### 보안을 위한 권장사항

1. **gradle.properties**에 API 키 저장:
```properties
NAVER_CLIENT_ID="your_client_id"
NAVER_CLIENT_SECRET="your_client_secret"
```

2. **build.gradle (app)**에서 BuildConfig로 설정:
```gradle
android {
    defaultConfig {
        buildConfigField "String", "NAVER_CLIENT_ID", "\"${NAVER_CLIENT_ID}\""
        buildConfigField "String", "NAVER_CLIENT_SECRET", "\"${NAVER_CLIENT_SECRET}\""
    }
}
```

3. **NaverDirectionsService.java**에서 사용:
```java
private static final String CLIENT_ID = BuildConfig.NAVER_CLIENT_ID;
private static final String CLIENT_SECRET = BuildConfig.NAVER_CLIENT_SECRET;
```

## 4. 테스트

API 키 설정 후 앱에서 다음 기능 테스트:

1. 일정 추가 화면에서 출발지/도착지 입력
2. "🗺️ 길찾기" 버튼 클릭
3. 경로 정보 (거리, 시간, 통행료, 연료비) 확인

## 5. 주의사항

- API 호출량에 따라 과금될 수 있습니다
- 무료 할당량: 월 100,000건
- 상용 서비스 시 적절한 요금제 선택 필요

## 6. 대안 방법

API 키가 없는 경우 다음 기능들이 작동합니다:

1. **더미 데이터**: 현재 구현된 기본 경로 정보 표시
2. **외부 지도 앱 연동**: 
   - 네이버 지도 앱
   - 구글 지도 웹/앱
3. **기본 길찾기**: 주소 기반 간단한 경로 안내

## 7. 문제 해결

### 자주 발생하는 오류

1. **401 Unauthorized**: API 키 확인
2. **403 Forbidden**: 서비스 URL 설정 확인
3. **Network Error**: 인터넷 연결 및 방화벽 확인

### 로그 확인

```bash
adb logcat | grep NaverDirections
```

API 호출 로그를 통해 문제점 파악 가능합니다.
