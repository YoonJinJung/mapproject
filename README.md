# 지구로 슝: 소행성 모니터링

NASA 공개 API를 활용한 근지구 천체(NEO) 위협 관제 안드로이드 대시보드 앱

---

## ⚠️ 서버 구성

### Lambda 함수 코드 위치

```
personalproject/
└── lambda/
    └── index.js      ← ✅ AWS Lambda에 실제 배포된 서버 코드 (CommonJS)
    
```


---

### 서버 구성 상세

| 항목 | 값 |
|---|---|
| 서비스 | AWS Lambda + Amazon API Gateway |
| 런타임 | Node.js 24.x (CommonJS) |
| 리전 | ap-southeast-2 (Sydney) |
| 엔드포인트 | `https://ek00ftzg73.execute-api.ap-southeast-2.amazonaws.com/default/space-threat-bff` |
| 함수명 | `space-threat-bff` |

#### 환경변수 설정 (Lambda Console → Configuration → Environment variables)

| 키 | 값 |
|---|---|
| `NASA_API_KEY` | NASA Open APIs에서 발급받은 API 키 |

> API 키 발급: https://api.nasa.gov

#### API 라우팅 (단일 엔드포인트, 쿼리 파라미터로 분기)

```
GET /space-threat-bff?start_date=YYYY-MM-DD&end_date=YYYY-MM-DD
    → NASA NeoWs API 호출 후 소행성 목록 반환

GET /space-threat-bff?type=apod
    → NASA APOD API 호출 후 오늘의 우주 이미지 정보 반환
```

#### Android 연결 설정

`app/src/main/java/edu/skku/map/personalproject/data/remote/NetworkConfig.kt`

```
BASE_URL      = "https://ek00ftzg73.execute-api.ap-southeast-2.amazonaws.com/default"
ENDPOINT_FEED = "/space-threat-bff"
```

---

## 프로젝트 구조

```
personalproject/
│
├── lambda/                              # AWS Lambda 서버 코드
│   ├── index.js                         # ✅ 배포 코드 (CommonJS)
│   ├── index.mjs                        # ES Module 버전 (참고용)
│   └── deploy.sh                        # 배포 스크립트
│
└── app/src/main/
    ├── AndroidManifest.xml
    │
    ├── java/edu/skku/map/personalproject/
    │   ├── data/
    │   │   ├── model/
    │   │   │   ├── Asteroid.kt              # 소행성 데이터 클래스
    │   │   │   ├── FeedResponse.kt          # NeoWs API 응답 모델
    │   │   │   └── ApodResponse.kt          # APOD API 응답 모델
    │   │   ├── local/
    │   │   │   ├── AppDatabase.kt           # Room DB 싱글턴
    │   │   │   ├── AsteroidDao.kt           # DAO (Flow 반환)
    │   │   │   └── WatchlistEntity.kt       # Room Entity
    │   │   ├── remote/
    │   │   │   ├── ApiService.kt            # OkHttp3 네트워크 호출
    │   │   │   └── NetworkConfig.kt         # 서버 URL 상수
    │   │   └── repository/
    │   │       └── AsteroidRepository.kt    # 네트워크·DB 통합 접근층
    │   │
    │   └── ui/
    │       ├── dashboard/
    │       │   └── DashboardActivity.kt     # 메인 (NeoWs + APOD 병렬 호출)
    │       ├── list/
    │       │   ├── AsteroidListActivity.kt  # 날짜 선택 + 목록
    │       │   └── AsteroidAdapter.kt
    │       ├── detail/
    │       │   └── AsteroidDetailActivity.kt # 상세 정보 + 관심 목록
    │       └── watchlist/
    │           ├── WatchlistActivity.kt     # Room Flow 실시간 구독
    │           └── WatchlistAdapter.kt
    │
    └── res/
        ├── layout/
        │   ├── activity_dashboard.xml
        │   ├── activity_asteroid_list.xml
        │   ├── activity_asteroid_detail.xml
        │   ├── activity_watchlist.xml
        │   ├── item_asteroid.xml
        │   └── item_watchlist.xml
        └── values/
            ├── colors.xml     # 다크 스페이스 테마 색상
            ├── strings.xml    # 한국어 문자열
            └── themes.xml     # Theme.Material3.Dark.NoActionBar
```

---

## 사용 오픈소스 라이브러리

| 라이브러리 | 버전 | 라이선스 | 링크 |
|---|---|---|---|
| OkHttp3 | 4.12.0 | Apache 2.0 | https://github.com/square/okhttp |
| Gson | 2.10.1 | Apache 2.0 | https://github.com/google/gson |
| Room | 2.7.0 | Apache 2.0 | https://developer.android.com/jetpack/androidx/releases/room |
| Kotlin Coroutines | 1.8.0 | Apache 2.0 | https://github.com/Kotlin/kotlinx.coroutines |
| Lifecycle (AndroidX) | 2.7.0 | Apache 2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle |

---

## 프로젝트 요약

**앱명**: 지구로 슝: 소행성 모니터링 / **언어**: Kotlin 100% / **Activity**: 4개

### 핵심 기능

| 기능 | 구현 방식 |
|---|---|
| 소행성 현황 대시보드 | Lambda → NeoWs API, 총 수·위험 수 요약 카드 |
| 날짜별 소행성 목록 | DatePickerDialog + RecyclerView(ListAdapter/DiffUtil) |
| 소행성 상세 정보 | Gson 직렬화로 Activity 간 객체 전달 |
| 관심 목록 저장·삭제 | Room DB, DAO Flow로 UI 자동 갱신 |
| 오늘의 우주 이미지 | Lambda → APOD API, 탭 시 `ACTION_VIEW`로 브라우저 실행 |

### 아키텍처 요점

- **AWS Lambda BFF**: NASA API 파싱·경량화를 서버에서 처리, API 키 앱 코드 미노출
- **계층형 구조**: UI → Repository → Remote/Local 명확히 분리
- **병렬 코루틴**: 대시보드에서 NeoWs + APOD를 `async { }` 패턴으로 동시 요청
- **Implicit Intent**: APOD 이미지 탭 → 기기 기본 브라우저로 NASA 원본 이미지 오픈
