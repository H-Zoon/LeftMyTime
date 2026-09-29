# 위젯 초 표시 하위 버전 지원 가능성 조사

2026-09-28. 사용자 요청: 하위 Android까지 확대할 수 있는지 확인.
**소스 조사 결과이며 지원 확대 구현이나 실행 검증 완료가 아니다.** 앱 코드·지원 조건·기본값은 변경하지 않았다.

## 판단

**Android 16의 문서 버전 8 환경은 우선 검증할 가치가 있는 확대 후보다.** 현재 요구하는 `>= 9`가 초 계산에 반드시 필요한 최소 버전이라는 근거는 확인하지 못했다. 전체 문서의 파싱·표시·접근성·시간 경계 실행을 버전 8에서 확인하지 않았으므로, 조건만 낮춰 출시할 근거도 아직 없다.

| 환경 | 확인한 것 | 판단 |
| --- | --- | --- |
| Android 17 / 문서 버전 9 | 9월 26일 실제 위젯 검증 기록 있음 | 현재 선택형 지원 유지 |
| Android 16 QPR2 계열 / 문서 버전 8 | 정수 epoch 초, 현지 초 시계, 초 경계 재그리기 예약 존재 | 현재 렌더러 재사용을 우선 검증할 후보 |
| 더 이른 Android 16 / 다른 문서 버전 | 로컬 SDK 36 소스는 문서 버전 4이고, alpha20에는 별도 WIDGETS_V6 생성기도 있음 | Android 16 전체를 버전 8로 취급하지 않음. 프로파일·헤더별 확인 필요 |
| Android 15 초기 AOSP | 현재 렌더러의 정수 연산·조건부 명령·레이아웃·접근성 명령이 빠짐 | 현재 조건만 낮춰 지원 불가. 다른 업데이트/제조사 환경까지 동일하다고 단정하지 않음 |
| Android 8~14 / API 26~34 | 현재 DrawInstructions 경로의 API 35 하한 미충족 | 기존 XML/Chronometer 등 별도 구현 필요 |

## 코드·라이브러리 근거

- `WidgetSecondsSupport.kt`는 API 35 이상이면서 `DrawInstructions.getSupportedVersion() >= 9`인 경우만 허용한다. 이 값은 시스템의 문서 처리 버전이며 제조사 런처별 적합성 인증은 아니다.
- `WidgetSecondsRenderer.kt`는 고정 의존성 `remote-creation-android:1.0.0-alpha20`의 **WIDGETS_V7**을 사용한다. alpha20 소스에서 해당 프로파일 API는 7이다. 현재 렌더러가 버전 9 전용 프로파일을 사용하는 것은 아니다.
- alpha20의 `Header.apply`는 프로파일 7 이상에서 라이브러리의 프로토콜 상수로 헤더를 쓴다. 로컬 소스의 상수는 1.1이며 `versionToApiLevel`은 1.1을 문서 API 7, 1.2를 8로 대응한다. 생성 프로파일·헤더·호스트 버전을 각각 확인해야 한다. 실제 제품 payload의 버전 8 적용 결과는 미확인이다.
- 현재 시간 계산은 정수 `종료 epoch 초 - 현재 epoch 초`를 먼저 계산하고 짧은 차이만 float로 변환한다. 원시 epoch를 float로 바꾸지 않는다. `TIME_IN_SEC` 의존성으로 초 갱신을 요청하고 연속 애니메이션 시계는 쓰지 않는다.
- 확대 시에도 0초 제한, 숫자·눈금·종료 문구 동기화, 오늘/7일 이내 시간 일정 범위, 집중 제외를 보존한다.

라이브러리 판단은 최신 브랜치가 아니라 로컬에 저장된 **alpha20 sources.jar**를 읽은 결과다. 원본 경로 참고: [RcPlatformProfiles.java](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/remote/remote-creation/src/androidMain/java/androidx/compose/remote/creation/profile/RcPlatformProfiles.java).

## 플랫폼 소스 근거

Android 16 QPR2의 공개 AOSP 소스를 직접 읽었다.

1. [CoreDocument.java](https://android.googlesource.com/platform/frameworks/base/+/android16-qpr2-release/core/java/com/android/internal/widget/remotecompose/core/CoreDocument.java): 문서 API 8, 프로토콜 1.2.
2. [TimeVariables.java](https://android.googlesource.com/platform/frameworks/base/+/android16-qpr2-release/core/java/com/android/internal/widget/remotecompose/core/TimeVariables.java): 정수 `ID_EPOCH_SECOND`와 `ID_TIME_IN_SEC`, `ID_TIME_IN_HR` 공급.
3. [RemoteComposeState.java](https://android.googlesource.com/platform/frameworks/base/+/android16-qpr2-release/core/java/com/android/internal/widget/remotecompose/core/RemoteComposeState.java): 초 시계 리스너가 있으면 다음 초 경계 근처로 재그리기 예약. 연속 시계와 별도 경로다. 실제 런처의 그리기 빈도·전력 검증을 뜻하지 않는다.

로컬 SDK 36의 `Operations.java`와 alpha20을 정적으로 비교했을 때 현재 렌더러에 관련된 정수 연산·텍스트 조회·조건부 명령·레이아웃·접근성 명령 번호가 일치했다. **번호 일치는 직렬화 형식·프로파일 허용 목록·실행 의미 일치의 증명이 아니다.** SDK 36 소스의 문서 버전은 4로, QPR2 버전 8이나 실제 시험 이미지와 동일한 환경으로 보고하지 않는다. QPR2의 일부 추가 연산 소스 URL은 서버 503으로 읽지 못해 전체 명령 대조는 미완료다.

Android 15 초기 [Operations.java](https://android.googlesource.com/platform/frameworks/base/+/android15-release/core/java/com/android/internal/widget/remotecompose/core/Operations.java)에는 `INTEGER_EXPRESSION`, `TEXT_LOOKUP`, `ID_LIST`, `CONDITIONAL_OPERATIONS`, 현재의 레이아웃·접근성 명령이 없다. [TimeVariables.java](https://android.googlesource.com/platform/frameworks/base/+/android15-release/core/java/com/android/internal/widget/remotecompose/core/TimeVariables.java)도 현지 시계만 공급하며 현재 경로에 필요한 정수 epoch 초를 공급하지 않는다. API 35에 [DrawInstructions](https://developer.android.com/reference/android/widget/RemoteViews.DrawInstructions)가 있다는 것만으로 현 렌더러를 활성화할 수 없다.

## Android 15 이하의 대안

Chronometer는 하위 OS에서도 초 카운트다운을 표시할 수 있다. 그러나 이 앱의 [9월 26일 비교 시험](QA_WIDGET_SECONDS_2026-09-26.md)에서는 종료 후 `−00:52`가 재현됐다. 앱의 경계 갱신이 지연될 때 위젯 자체적으로 0·종료 문구·눈금까지 동기화하는 현재 요구사항은 충족하지 못한다.

매초 앱 실행/위젯 게시 또는 정확 알람 추가는 백그라운드 실행·전력·권한·갱신 지연을 별도로 다뤄야 하는 구현이다. 30분 제한은 `updatePeriodMillis`에 대한 것이며 이벤트 갱신 전반을 금지하는 제한으로 해석하지 않는다. [공식 갱신 문서](https://developer.android.com/develop/ui/views/appwidgets/advanced).

## 다음 구현·검증 단위

1. **문서 버전 8만 첫 확대 후보로 삼는다.** debug 경로에서 현재 제품 렌더러를 버전 8 시스템 플레이어에 적용한다. AndroidX 내장 플레이어의 성공으로 대체하지 않는다. 버전 7/6 및 Android 15 이하는 같은 변경에 묶지 않는다.
2. 기존 플랫폼 시험의 `WidgetSecondsSupport.available()` 조건 때문에 버전 8에서는 동적 문서 시험이 건너뛰어진다. 후보 호스트 검증은 제품 활성화 조건과 분리한다. 정적 대체 시험 통과를 동적 지원 증거로 쓰지 않는다.
3. 실제 런처에서 시작 전→진행→0초 종료, 경계 방송 지연, 앱 프로세스 종료, 화면 껐다 켜기, 분·시간·자정·날짜/시간대 변경을 확인한다. 반복/자동 다음 일정은 지연 시 이전 회차가 0과 종료를 유지해야 한다.
4. 숫자·눈금 동기화, 상세/새로고침 클릭, 접근성 설명, 밝게/어둡게·작은 크기·큰 글자·긴 제목·빈 상태를 실제 구성요소로 검수한다. 초당 수준의 갱신과 매초 앱 IPC/비트맵 생성이 없는지도 확인한다.
5. 통과 후 지원 판정과 설명을 확대하고 미지원 환경의 정적 대체를 유지한다. DB·설정 키·정확 알람 권한 변경은 현재 재사용안에 필요하지 않을 것으로 예상한다. 실패 시 명령/프로파일 단위로 원인을 기록한다.

## 이번 확인 범위

앱 코드, 기존 QA, 고정 라이브러리, SDK 36, Android 15 및 Android 16 QPR2 공개 소스를 읽고 비교했다. 임시 소스는 `/private/tmp/leftmytime-compat/`에 두었으며 제품에 포함하지 않는다.

**빌드·Lint·단위/계측 테스트, 새 APK 설치, 에뮬레이터/실기기 실행·화면·전력 검수는 하지 않았다.** 사용자에게 달라진 앱 동작은 없으며 공통 렌더러 재사용 가능성을 조사한 문서만 추가했다. 실제 지원 확대 확정은 위 실행 검증이 남아 있다.
