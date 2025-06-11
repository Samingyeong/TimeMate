# 📁 TimeMate 파일 정리 및 리팩토링 계획

## 🚨 현재 문제점

### 1. 중복된 패키지 구조
- `features/` vs `ui/` 패키지 중복 (같은 기능이 두 곳에 존재)
- 유틸리티 클래스들이 루트에 흩어져 있음
- 어댑터 클래스들이 여러 위치에 분산

### 2. 사용되지 않는 파일들
- 오래된 Activity 클래스들
- 중복된 어댑터 클래스들
- 사용되지 않는 서비스 클래스들

## 🎯 정리 계획

### Phase 1: 중복 제거 및 통합
1. **features/ 패키지로 통합** (ui/ 패키지 제거)
2. **유틸리티 클래스 정리** (utils/ 패키지로 통합)
3. **어댑터 클래스 정리** (각 기능별 패키지로 이동)

### Phase 2: 사용되지 않는 파일 제거
1. **오래된 Activity 제거**
2. **중복된 서비스 클래스 제거**
3. **사용되지 않는 모델 클래스 제거**

## 🏠 1. 홈 기능 (features/home/)

### 📁 파일 구조
```
features/home/
├── HomeActivity.java                     # 메인 홈 화면
├── HomePresenter.java                    # 홈 화면 비즈니스 로직
├── service/
│   └── WeatherService.java              # 날씨 정보 서비스
└── adapter/
    └── TodayScheduleAdapter.java         # 오늘 일정 어댑터
```

### 🎯 주요 기능
- ☀️ **날씨 정보**: OpenWeather API 연동
- 📅 **일정 미리보기**: 오늘/내일 일정 카드
- 🔔 **알림 요약**: 중요 알림 표시
- 🚀 **빠른 액션**: 일정 추가 버튼

### 🔗 의존성
- `WeatherService` → `ApiConfig`
- `HomePresenter` → `ScheduleRepository`
- `TodayScheduleAdapter` → `Schedule` 모델

## 📅 2. 일정 관리 기능 (features/schedule/)

### 📁 파일 구조
```
features/schedule/
├── ScheduleAddActivity.java             # 일정 추가 화면
├── ScheduleListActivity.java            # 일정 목록 화면
├── ScheduleDetailActivity.java          # 일정 상세 화면
├── presenter/
│   ├── ScheduleAddPresenter.java        # 일정 추가 로직
│   ├── ScheduleListPresenter.java       # 일정 목록 로직
│   └── ScheduleDetailPresenter.java     # 일정 상세 로직
└── adapter/
    ├── PlaceSuggestAdapter.java         # 장소 자동완성 어댑터
    ├── RouteOptionAdapter.java          # 경로 옵션 어댑터
    └── ScheduleListAdapter.java         # 일정 목록 어댑터
```

### 🎯 주요 기능
- ➕ **일정 CRUD**: 생성, 조회, 수정, 삭제
- 🔍 **실시간 장소 검색**: 네이버 Local Search API
- 🗺️ **최적 경로 추천**: 대중교통/도보/자동차
- 👥 **친구 초대**: 일정 공유 기능
- ⏰ **알림 설정**: 자동 리마인더

### 🔗 의존성
- `ScheduleAddPresenter` → `NaverPlaceSearchRetrofitService`
- `ScheduleAddPresenter` → `NaverOptimalRouteService`
- `ScheduleAddPresenter` → `ScheduleRepository`

## 👥 3. 친구 관리 기능 (features/friend/)

### 📁 파일 구조
```
features/friend/
├── FriendListActivity.java              # 친구 목록 화면
├── FriendAddActivity.java               # 친구 추가 화면
├── presenter/
│   ├── FriendListPresenter.java         # 친구 목록 로직
│   └── FriendAddPresenter.java          # 친구 추가 로직
└── adapter/
    └── FriendListAdapter.java           # 친구 목록 어댑터
```

### 🎯 주요 기능
- 👤 **친구 추가/삭제**: ID 기반 친구 관리
- 📋 **친구 목록**: 친구 상태 표시
- 📅 **일정 공유**: 친구와 일정 공유
- 🔔 **초대 알림**: 친구 요청 알림

## 👤 4. 프로필 기능 (features/profile/)

### 📁 파일 구조
```
features/profile/
├── ProfileActivity.java                 # 프로필 화면
├── SettingsActivity.java                # 설정 화면
├── presenter/
│   ├── ProfilePresenter.java            # 프로필 로직
│   └── SettingsPresenter.java           # 설정 로직
└── adapter/
    └── SettingsAdapter.java             # 설정 목록 어댑터
```

### 🎯 주요 기능
- 👤 **개인정보 관리**: 프로필 조회/수정
- 🔐 **계정 관리**: 로그아웃, 계정 전환
- ⚙️ **앱 설정**: 알림, 테마, 언어 설정
- 📊 **통계**: 일정 완료율, 사용 통계

## 🌐 5. 네트워크 레이어 (network/)

### 📁 파일 구조
```
network/
├── api/
│   ├── NaverApiService.java             # 네이버 API 통합 서비스
│   ├── NaverPlaceSearchRetrofitService.java  # 장소 검색 API
│   ├── NaverOptimalRouteService.java    # 경로 추천 API
│   └── OpenWeatherService.java          # 날씨 API
├── interceptor/
│   └── NaverAuthInterceptor.java        # 네이버 API 인증 인터셉터
└── model/
    ├── PlaceSearchResponse.java         # 장소 검색 응답 모델
    ├── RouteResponse.java               # 경로 응답 모델
    └── WeatherResponse.java             # 날씨 응답 모델
```

### 🎯 주요 기능
- 🔍 **장소 검색**: 네이버 Local Search API
- 🗺️ **경로 계산**: 네이버 Directions API
- ☀️ **날씨 정보**: OpenWeather API
- 🔐 **인증 관리**: 자동 헤더 추가

## 💾 6. 데이터 레이어 (data/)

### 📁 파일 구조
```
data/
├── database/
│   ├── AppDatabase.java                 # Room 데이터베이스
│   ├── DatabaseMigrations.java          # 마이그레이션
│   └── dao/
│       ├── ScheduleDao.java             # 일정 DAO
│       ├── FriendDao.java               # 친구 DAO
│       └── SharedScheduleDao.java       # 공유일정 DAO
├── repository/
│   ├── ScheduleRepository.java          # 일정 리포지토리
│   ├── FriendRepository.java            # 친구 리포지토리
│   └── UserRepository.java              # 사용자 리포지토리
└── model/
    ├── Schedule.java                     # 일정 엔티티
    ├── Friend.java                       # 친구 엔티티
    ├── SharedSchedule.java               # 공유일정 엔티티
    └── User.java                         # 사용자 엔티티
```

### 🎯 주요 기능
- 🗄️ **로컬 데이터베이스**: Room 기반 SQLite
- 📊 **데이터 접근**: Repository 패턴
- 🔄 **데이터 동기화**: 로컬-서버 동기화
- 📈 **마이그레이션**: 데이터베이스 버전 관리

## 🛠️ 7. 핵심 유틸리티 (core/)

### 📁 파일 구조
```
core/
├── config/
│   └── ApiConfig.java                   # API 설정 중앙 관리
├── util/
│   ├── UserSession.java                 # 사용자 세션 관리
│   ├── DateUtils.java                   # 날짜 유틸리티
│   ├── NetworkUtils.java                # 네트워크 유틸리티
│   └── ValidationUtils.java             # 유효성 검사 유틸리티
└── base/
    ├── BaseActivity.java                # 베이스 액티비티
    ├── BasePresenter.java               # 베이스 프레젠터
    └── BaseAdapter.java                 # 베이스 어댑터
```

### 🎯 주요 기능
- 🔑 **API 키 관리**: 중앙 집중식 설정
- 👤 **세션 관리**: 로그인 상태 관리
- 🛠️ **공통 유틸리티**: 날짜, 네트워크, 유효성 검사
- 📱 **베이스 클래스**: 공통 기능 추상화

## 🔔 8. 백그라운드 작업 (worker/)

### 📁 파일 구조
```
worker/
├── DailyScheduleWorker.java             # 일일 스케줄 체크
├── NotificationWorker.java              # 알림 발송
├── RouteCalculationWorker.java          # 경로 계산
└── DataSyncWorker.java                  # 데이터 동기화
```

### 🎯 주요 기능
- ⏰ **자동 스케줄링**: 매일 00:05 일정 체크
- 🔔 **알림 발송**: 푸시 알림 + 홈 위젯
- 🗺️ **경로 계산**: 출발 시간 자동 계산
- 🔄 **데이터 동기화**: 백그라운드 동기화

## 🔗 9. 의존성 관계도

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Features      │    │    Network      │    │     Data        │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │    Home     │ │───▶│ │ WeatherAPI  │ │    │ │ Repository  │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │  Schedule   │ │───▶│ │  NaverAPI   │ │    │ │  Database   │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │   Friend    │ │───▶│ │Interceptor  │ │    │ │    DAO      │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │      Core       │
                    │                 │
                    │ ┌─────────────┐ │
                    │ │   Config    │ │
                    │ └─────────────┘ │
                    │                 │
                    │ ┌─────────────┐ │
                    │ │    Util     │ │
                    │ └─────────────┘ │
                    │                 │
                    │ ┌─────────────┐ │
                    │ │    Base     │ │
                    │ └─────────────┘ │
                    └─────────────────┘
```

## 🎯 10. 장점 및 효과

### ✅ **유지보수성 향상**
- 기능별 독립적인 모듈 구조
- 명확한 책임 분리
- 코드 재사용성 증대

### ✅ **확장성 증대**
- 새로운 기능 추가 용이
- 기존 코드 영향 최소화
- 모듈별 독립적 개발 가능

### ✅ **테스트 용이성**
- 단위 테스트 작성 용이
- Mock 객체 사용 간편
- 통합 테스트 효율성

### ✅ **팀 협업 효율성**
- 기능별 담당자 분배 가능
- 코드 충돌 최소화
- 리뷰 범위 명확화

## 🚀 11. 다음 단계

1. **기존 코드 마이그레이션**: 현재 코드를 새 구조로 이동
2. **베이스 클래스 구현**: 공통 기능 추상화
3. **유닛 테스트 작성**: 각 모듈별 테스트 코드
4. **문서화 완성**: API 문서 및 가이드 작성
5. **CI/CD 파이프라인**: 자동화된 빌드 및 배포
