# monitoring_desktop

# 사용자의 데스크탑 앱 사용 시간을 기록하고 카테고리별로 분석·관리하는 프로젝트

---

```text
monitoring_desktop/
 ├─ javaServer/        # Spring Boot 백엔드 서버
 │   ├─ src/
 │   ├─ build.gradle
 │   ├─ settings.gradle
 │   └─ ...
 └─ pythonClient/      # 데스크탑 모니터링 Python 클라이언트
     ├─ main.py
     └─ overlookDesktop.py
```

## 주요 기능 목록

### 공통 개념

앱 세션
 - 하나의 앱이 켜졌다가 꺼질 때까지를 하나의 세션으로 저장
 - START 이벤트: 앱 실행 시간 기록 (시작 시각)
 - STOP 이벤트: 종료 시간 기록 및 사용 시간(초 단위) 계산
카테고리
 - CODE, GAME, MUSIC, PRODUCT 등으로 앱을 분류
 - 카테고리별로 총 사용 시간 집계
   
### Python 클라이언트

앱 시간 감시 (psutil 기반)
 - psutil.process_iter()로 현재 실행 중인 프로세스 스캔
 - START, STOP 감지
Spring 서버로 사용 내역 전송
 - requests.post()를 사용하여 http://localhost:8080/api/v1/game/log 로 json 전송
카테고리 조회/수정 (Flask + Spring 연동)
 - Flask 서버(/api/v1/game/category)에서 앱 목록 조회, 추가 가능

### Java 서버

앱 사용 로그 저장 (POST /api/v1/game/log)
 - Python 클라이언트에서 보내는 AppUsageRequest를 수신
 - AppSaveService가 AppSession 엔티티로 DB에 저장
 - START/STOP 이벤트에 따라 세션 생성,종료
앱별 총 사용 시간 조회 (GET /apps/all)
 - DB에 저장된 세션 전체를 불러와 앱 이름별로 실행 구간 중첩을 제거 후 총 사용 시간 계산
지정한 앱 목록만 필터링 조회(ex -> GET /apps/filter?apps=chrome.exe&apps=spotify.exe)
 - 받은 앱 이름 목록만 골라서 사용 시간 반환
카테고리별 총 사용 시간 조회 (GET /categories/all)
 - 각 카테고리별로 세션 구간을 병합 후 총 사용 시간 계산
카테고리별 앱 목록 조회 / 수정 (Flask 연동)
 - GET /api/v1/game/category → Flask GET 호출 후 결과 그대로 반환
 - POST /api/v1/game/category → 요청 JSON을 Flask로 그대로 전달

---

## 상세 설계

### 1단게: 클라이언트(Python) - 파이썬 데스크탑 감시 클라이언트
현재 실행 중인 모든 프로세스 스캔
 - psutil.process_iter(['pid', 'name', 'create_time']) 사용
 - get_app_category()로 감시 대상인지 확인 후 running_instances에 등록
새로 시작된/종료된 프로세스 체크
 - check_start(current_snapshot)
 - check_stop(current_snapshot, pids_to_check)
서버로 이벤트 전송
 - 예시
```
{
  "pid": 1234,
  "appName": "chrome.exe",
  "eventType": "START",
  "eventTime": "2025-11-24T18:30:12.345678",
  "appCategory": "PRODUCT"
}
```

### 2단계: 서버(Spring Boot) - 앱 사용 로그 수집 및 분석 API

(1) 컨트롤러
**AppUsageController**
 - POST /api/v1/game/log
 - AppSaveService.processAppEvent() 호출
**AppLoadController**
 - GET /apps/all
 - GET /apps/filter?apps=앱 이름...
 - GET /categories/all
**CategoryController**
 - GET /api/v1/game/category
 - POST /api/v1/game/category

(2) 서비스
AppSaveService
 - **START 이벤트**
 - 만약 이미 활성화된 같은 pid를 가진 세션이 있다면 종료
 - 없으면 새로운 세션 생성 및 저장

 - **STOP 이벤트**
 - 해당 pid의 미완료 세션을 찾아 종료, duration을 계산 후 저장

AppLoadService
 - **getAggregatedUsageStats()**
 - 앱 이름별로 세션 목록을 그룹화
 - 각 앱에 대해 시간 겹치는 곳을 병합 -> 총 사용 시간 반환
 - **getFilteredAppStats(List<String> apps)**
 - 전체 앱 통계에서 특정 이름 필터(대소문자 무시)
 - **getMergedCategoryStats()**
 - appCategory 기준 그룹화 후 카테고리별 총 사용 시간 계산

## 실행

포트:
 - Spring Boot: 8080
 - Flask(Python): 5000

Python 패키지 목록:
 - psutil
 - flask
 - flask-cors
 - requests

스프링 부트 Dependencies:
- spring-boot-starter-web: REST API 개발
- spring-boot-starter-data-jpa: JPA/Hibernate 기반 DB 연동
- spring-boot-starter-thymeleaf: 간단한 HTML 뷰 렌더링용
- lombok: 보일러플레이트 코드 제거
- h2database: 테스트용 인메모리 DB
- spring-boot-starter-test: JUnit5 통합 테스트 환경
