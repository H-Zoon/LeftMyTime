# 위젯 초 표시 하위 기기 지원 재조사

**2026-10-06 후속: 실행 검증 후 문서6/8을 제품 지원에 포함했다.** [최신 실행 기록·남은 접근성 문제](QA_WIDGET_SECONDS_COMPATIBILITY_2026-10-06.md)가 아래 후보/미실행 설명보다 우선한다. 아래는 조사와 최초 구현 당시 이력이다.

2026-10-05. **조사 및 후보 생성기 구현 완료 / 실행 검증·제품 지원 확대 전.** 후속 ‘진행’ 요청으로 문서 6/8용 생성기 선택과 검증 경로를 구현했다. 일반 사용자에게 노출하는 조건은 기존 문서 9 이상으로 유지한다. 앞선 [갱신 복구 수정](WIDGET_REFRESH_RECOVERY_2026-10-05.md)과 이번 변경 모두 미빌드 상태다.

## 결론

**현재 품질을 유지하는 현실적인 확대 대상은 Android 16 계열의 문서 버전 8뿐 아니라 버전 6까지다.** 버전 8은 기존 WIDGETS_V7 경로, 버전 6은 WIDGETS_V6 생성기와 구형 헤더를 사용하는 후보로 분리한다. 소스 근거가 확보된 것이며, 실제 런처에서 정상 동작한다고 확정한 것은 아니다.

9월 28일 조사는 버전 8만 우선 후보로 남겼다. 이번에는 공개 `android16-release` / `android16-qpr1-release`의 문서 API 6, 정수 epoch 초, 필수 연산·레이아웃·접근성 명령과 alpha20의 V6 생성기를 직접 확인했다. **일괄 `>= 9` → `>= 6` 변경은 올바른 구현이 아니다.** 문서 헤더와 생성기를 함께 선택해야 한다.

| 실제 환경 | 조사 결과 | 제공 판단 |
| --- | --- | --- |
| 기존 Android 17 / 문서 9 검수 환경 | 과거 실제 위젯 검수 기록 있음 | 현행 범위 유지. 다른 제조사까지 검증된 것은 아님 |
| Android 16 QPR2 공개 소스 / 문서 8 | 정수 시계·초 틱·현재 핵심 명령 존재, 프로토콜 1.2 | 기존 V7 렌더러의 우선 검증 후보 |
| Android 16 및 QPR1 공개 소스 / 문서 6 | 정수 시계·초 틱·핵심 명령 존재, 프로토콜 1.0 | **V6 생성기 경로 구현, 실행 검증 대기** |
| 문서 7인 호스트 | alpha20에 V7 생성기 있음. 이번에 문서 7 실기기/해당 플랫폼 소스의 전체 조합은 조사하지 않음 | 별도 호스트 확보 후 확인. 6/8의 결과로 통과 처리하지 않음 |
| 로컬 SDK 36 소스 / 문서 4 | 공개 release 브랜치의 6과 다름 | 현 alpha20 헤더 경로는 6/7+를 대상으로 하므로 일괄 활성화 제외 |
| Android 15 초기 AOSP / 문서 1 | 정수 epoch·정수 연산·조건부/현재 레이아웃·접근성 명령 부족 | 현 제품 렌더러를 그대로 지원 불가 |
| Android 8~14 / API 26~34 | DrawInstructions API 없음 | Chronometer 등 다른 구현이 필요하며 현재 제품 조건을 그대로 충족하지 못함 |

이는 **공개 소스에서 확인한 버전 대응**이다. Android 16이라는 이름만으로 문서 6/8을 보장하지 않으며, 제조사 업데이트가 다를 수 있다. API 35 이상에서 `DrawInstructions.getSupportedVersion()`을 읽고 실제 런처까지 확인해야 한다. 이전에 보고된 Samsung Android 15 기기의 현재 문서 버전은 이번에 읽지 못했다. 연결 기기는 0대다.

## 새로 확보한 근거

### 1. 버전 6에도 필요한 시계가 있다

공개 `android16-release`와 `android16-qpr1-release`의 `CoreDocument`는 문서 API 6 / 프로토콜 1.0이다. `TimeVariables`는 정수 `ID_EPOCH_SECOND`와 현지 `ID_TIME_IN_SEC`, `ID_TIME_IN_HR`을 공급한다. `RemoteComposeState`는 초 변수 리스너에 1,000ms 재그리기를 요청한다. QPR2 / 문서 8은 다음 초 경계에 가깝게 요청하는 방식이다. 따라서 **초 단위 계산 자체가 문서 9 전용이라는 근거는 없다.**

현재의 정수 epoch 차이 → 짧은 차이를 float로 변환하는 방법도 버전 6 소스의 정수 뺄셈/결과 등록 경로와 맞는다. 원시 epoch를 float로 바꾸는 대안을 쓸 필요는 없다. 2038 이전·7일 이내·집중 제외 등 기존 `WidgetSecondsPlan`의 범위를 보존한다. 시계의 존재나 재그리기 요청은 실제 런처의 갱신 빈도·배터리 검증 결과가 아니다.

근거: [Android 16 CoreDocument](https://android.googlesource.com/platform/frameworks/base/+/android16-release/core/java/com/android/internal/widget/remotecompose/core/CoreDocument.java), [TimeVariables](https://android.googlesource.com/platform/frameworks/base/+/android16-release/core/java/com/android/internal/widget/remotecompose/core/TimeVariables.java), [RemoteComposeState](https://android.googlesource.com/platform/frameworks/base/+/android16-release/core/java/com/android/internal/widget/remotecompose/core/RemoteComposeState.java), [QPR2 TimeVariables](https://android.googlesource.com/platform/frameworks/base/+/android16-qpr2-release/core/java/com/android/internal/widget/remotecompose/core/TimeVariables.java).

### 2. 가장 중요한 차이는 헤더와 생성기다

고정 의존성 `remote-creation-android:1.0.0-alpha20`의 소스를 직접 읽었다.

- `RcPlatformProfiles.WIDGETS_V6`: API 6, `WidgetsProfileWriterV6` 팩터리. V6에서 쓸 수 없는 부동소수 연산/폰트 등 일부 인수를 추가 검증한다.
- `WIDGETS_V7`: API 7, widgets profile을 쓰는 일반 `RemoteComposeWriterAndroid`.
- `Header.apply()`: API 6이면 **1.0 평면 헤더**, API 7 이상이면 **magic number가 있는 속성 맵 헤더**를 쓴다. 현재 alpha20의 맵 헤더 프로토콜 상수는 1.1이다.
- 버전 6 시스템 플레이어는 최대 프로토콜 1.0을 검사한다. 지원 판정만 낮추고 지금의 V7 문서를 그대로 보내면 호환되지 않는 문서가 된다.

구현 시에는 버전별 **실제 생성기**를 선택해야 한다. V6 프로파일만 일반 생성자에 넘기기보다 V6 팩터리/전용 생성기의 검증도 사용한다. 구형 편의 함수가 추가하는 화면 전체 스케일 변환을 그대로 복사하지 않고, 현재의 실제 픽셀 크기·큰 글자·접근성 터치 영역을 보존한다.

라이브러리를 올리거나 앱에 AndroidX 플레이어를 넣어도 런처의 시스템 플레이어를 교체하지는 못한다. 현재 alpha20은 [공식 릴리스 기록](https://developer.android.com/jetpack/androidx/releases/compose-remote)에도 2026-09-23 버전으로 나와 있다. 이번 확대를 위해 의존성부터 올릴 근거는 없다. 공개 API 하한은 [DrawInstructions API 35](https://developer.android.com/reference/android/widget/RemoteViews.DrawInstructions)다.

### 3. 명령 번호보다 한 단계 깊게 확인했다

alpha20의 작성 코드와 Android 16 문서 6 / QPR2 문서 8의 읽기 코드를 대조했다.

| 영역 | 대조 내용 | 판단 |
| --- | --- | --- |
| 계산 | IntegerExpression, FloatExpression, 정수 뺄셈/ID 32, 정수 결과의 float 참조 | 필요한 데이터 순서·연산 경로 확인 |
| 숫자·문구 | TextFromFloat, TextMerge, TextLookup, DataListIds | 핵심 필드 인코딩과 읽기 순서 확인 |
| 눈금 상태 | ConditionalOperations, 기본 산술/MIN/MAX/FLOOR/MOD | 현재 사용하는 연산은 V6 범위 내 |
| 레이아웃·클릭 | Root/Box/Row/Column/Canvas, CanvasContent, width/height modifier, ClickModifier, HostAction | 핵심 필드 형식 대조 |
| 접근성 | CoreSemantics의 설명·역할·상태·모드·enabled/clickable | 버전 6에도 읽기 경로 존재. 최신 화면 낭독은 실검증 필요 |
| 그리기 | TextData, PaintData, BitmapData, DrawText | V6 핵심 필드 확인. QPR2 TextData/DrawText는 서버 제한으로 추가 조회 미완료 |

QPR2의 일부 추가 조회는 HTTP 429로 읽지 못했다. 이를 전체 명령 대조 완료로 확대하지 않는다. 전체 payload 생성/파싱·실제 프레임 갱신·접근성·제조사 런처·장시간 성능은 미검증이다. V6의 문자열 조회는 최신 라이브러리와 리스너 구현도 다르므로, 문서가 한 번 표시되는 것뿐 아니라 같은 진행 상태에서 숫자가 계속 줄어드는지 반드시 확인한다.

추가 근거: [V6 Operations](https://android.googlesource.com/platform/frameworks/base/+/android16-release/core/java/com/android/internal/widget/remotecompose/core/Operations.java), [V6 IntegerExpression](https://android.googlesource.com/platform/frameworks/base/+/android16-release/core/java/com/android/internal/widget/remotecompose/core/operations/IntegerExpression.java), [V6 CoreSemantics](https://android.googlesource.com/platform/frameworks/base/+/android16-release/core/java/com/android/internal/widget/remotecompose/core/semantics/CoreSemantics.java), [V6 플레이어](https://android.googlesource.com/platform/frameworks/base/+/android16-release/core/java/com/android/internal/widget/remotecompose/player/RemoteComposePlayer.java).

## Android 15 이하의 선택지

**초 숫자를 움직이는 것과, 현재 제품처럼 정확한 상태를 유지하는 것은 별개의 조건이다.**

| 방법 | 가능한 범위 | 남는 문제 |
| --- | --- | --- |
| XML Chronometer | countdown API 24부터. 앱 minSdk 26 전체에서 사용 가능 | 종료 후 음수, 자정 재설정/시작→진행 전환/눈금·문구는 앱 재게시 필요 |
| Android 15의 구형 DrawInstructions | 기본 실수 계산·텍스트 형식·현지 초 시계는 소스에 존재 | 현재 정수 epoch/조건부/레이아웃·접근성 구현 없음. 오늘 전용 단순 표현도 별도 구형 문서·접근성 설계가 필요 |
| 매초 앱에서 RemoteViews 게시 | 앱 프로세스가 실행되는 동안 갱신 가능 | 백그라운드 수명·화면 비가시 상태·전력·IPC 비용을 해결해야 함. 정적 갱신 보완만으로 성립하지 않음 |
| 새 Glance/Compose 라이브러리나 앱 내 플레이어 | 앱 내부 렌더링/위젯 생성 방식 변경 | 기존 런처의 원격 뷰 기능을 교체하지 않음 |

Chronometer의 `setBase()`는 `elapsedRealtime` 기준이다. 날짜·시간대·시각 수동 변경·재부팅 뒤에는 끝점 변환을 다시 해야 한다. 일반 Activity 안에서는 tick listener로 0에서 멈출 수 있지만, 위젯 RemoteViews로 임의의 listener나 custom subclass를 런처에 보낼 수는 없다. `setFormat()`도 계산 결과를 감싸는 문자열 형식이지 종료 시 정지 규칙이 아니다. [Chronometer API](https://developer.android.com/reference/android/widget/Chronometer), [RemoteViews 지원 클래스](https://developer.android.com/reference/android/widget/RemoteViews).

이 저장소에서는 이미 [9월 26일 대조 시험](QA_WIDGET_SECONDS_2026-09-26.md)에서 종료 후 `−00:52`를 확인했다. 직전의 WorkManager/비정확 경계 알람 보완도 매초 렌더링이나 종료 순간의 정지를 보장하지 않으므로 이 한계를 해결한 것으로 취급하지 않는다. 정확 알람에 의존하는 방식은 권한·기기 제약과 실제 전달 검증을 별도로 다뤄야 하며, 현재의 추가 권한 없이 동작하는 초 표시와 같지 않다.

따라서 Android 15 이하에 Chronometer를 넣으려면 **숫자만 초 단위, 눈금은 마지막 갱신 기준, 종료 전환은 지연 가능** 같은 별도의 제품 조건을 먼저 정해야 한다. 현재의 0초 제한·눈금 동기화 기준을 유지하면서 그대로 확대할 수 있다고 권하지 않는다. 현재 Samsung 기기는 실제 지원 문서 버전을 읽기 전에는 이 경로와 최신 문서 경로 중 어느 것을 쓸 수 있는지 확정하지 않는다.

## 구현 순서와 현재 진행

1. `WidgetSecondsSupport`의 단일 boolean을 내부 capability 판정으로 분리한다. 미지원/구형 V6/기존 V7 계열을 구별하되, 실제 통과 전 제품 활성화 조건은 유지한다. API 35 이하를 일괄 켜거나 OS 이름만으로 분기하지 않는다.
2. `WidgetSecondsRenderer`에 생성기 선택만 주입하고 `WidgetSecondsPlan`, 기존 숫자·눈금·팔레트·의미 구성요소는 재사용한다. 현재 문서 9 경로의 표현과 동작도 회귀 확인한다.
3. debug의 제품 probe와 `WidgetSecondsPlatformTest`가 기존에는 `available()` 조건 때문에 문서 6/8에서 제품 문서를 시험하지 못했다. **검증 대상의 허용 범위와 출시 허용 범위를 분리**한다. `remote` 단순 probe 성공을 제품 전체 문서의 성공으로 대체하지 않는다.
4. 문서 8에서 기존 V7 제품 문서, 문서 6에서 V6 제품 문서를 실제 시스템 플레이어와 홈 런처로 확인한다. 소스의 기능 차이를 구분하기 위해 두 환경을 각각 기록한다. 문서 7/4는 그에 해당하는 호스트를 확보해 별도 판단한다.
5. 실행 검증 통과 후에만 각 버전의 초 옵션 노출과 정적 대체 정책을 확대한다. 선택형/기본 꺼짐, 오늘·유효한 7일 이내 시간 일정, 집중·날짜·기간 요약 제외를 유지한다.

후속 구현으로 **1~3번을 코드에 반영**했다. 4~5번은 아직 수행하지 않았다.

### 반영한 코드

- `WidgetSecondsCapability`: OS API와 호스트 문서 버전을 따로 판정. 문서 6 → V6, 문서 8 및 기존 9 이상 → V7. 문서 7/4/알 수 없는 값은 검증 후보에서 제외한다. API 35 미만은 항상 제외한다.
- `WidgetSecondsSupport`: 버전 조회 실패 시 미지원으로 처리하며, `available()`은 계속 제품 지원만 반환한다. 검증용 `validationProfile`과 제품용 `productionProfile`을 구분하므로 후보 probe를 실행해도 일반 위젯의 초 옵션이 켜지지 않는다.
- `WidgetSecondsRenderer`: `WidgetsProfileWriterV6`의 직접 생성자로 V6 헤더와 연산 검증을 사용한다. V7은 기존 생성자를 유지한다. 문서 바이트 생성과 RemoteViews 포장만 분리했으며 시간 계산·숫자·눈금·팔레트·48dp 터치·접근성 구성은 같은 코드다. V6 편의 함수의 전체 화면 축소는 추가하지 않는다.
- debug `WidgetSecondsProbe`: `candidate` 모드에서 실제 제품 렌더러를 사용하며 `source=today/custom`을 선택한다. 화면에 호스트 문서·생성 형식·제품 지원 여부를 표시한다. `production`은 기존 출시 조건을 그대로 시험한다. 단순 probe의 기본값은 초 틱 기반 `remote_epoch`로 변경하고, `remote`는 연속 시계 비교용으로만 남긴다. Chronometer 비교의 종료 시점에도 시작 지연을 반영했다.
- 단위 테스트 소스: SDK/문서 버전 조합, 후보의 제품 노출 방지, 기존 9 이상 유지 조건 추가.
- 계측 테스트 소스: V6/V7 실제 제품 문서의 헤더 검사, 실제 하위 호스트 후보 경로, 오늘 자정·일정 시작/종료·한/영·밝게/어둡게·큰 글자·크기별 최초 그리기 검사 추가. 시스템 플레이어는 헤더를 거절해도 View를 반환할 수 있어, inflate만 보지 않고 그린 결과에 보이는 픽셀이 있는지도 확인하도록 했다. 이 검사는 내용의 정확성·지속적인 초 갱신·TalkBack을 보증하지 않는다.

DB·일반 위젯 설정 키·초 기본값·출시 버전·alpha20 의존성은 이번 하위 지원 작업에서 변경하지 않았다. 새 예시와 검증 모드는 debug 소스에만 있다.

### 다음 실행 검증 방법

아래는 **최종 검증을 요청받은 뒤 실행할 절차**이며 이번에는 실행하지 않았다. 기존 기기에 설치된 앱으로 새 생성기를 검증할 수는 없으므로 변경 반영 debug APK가 필요하다.

1. 기본 검증: `./gradlew testDebugUnitTest assembleDebug lintDebug`. 계측 APK도 생성해 `WidgetSecondsPlatformTest`를 실행한다. API 35 미만, 문서 6, 8, 기존 9 환경의 조건 제외/실행 결과를 구분해 남긴다. 헤더 검사 통과를 해당 호스트의 재생 통과로 취급하지 않는다.
2. 문서 6/8 환경에서 다음으로 실제 제품 probe를 추가한다. 화면/로그의 호스트 버전과 V6/V7 선택을 기록하고, 사용자가 런처의 추가 화면을 완료한다.

   ```sh
   adb shell am start -n com.devidea.timeleft/com.devidea.timeleft.design.WidgetSecondsProbeActivity --es mode candidate --es source custom --ei start_delay 10 --ei seconds 90 --ez pin true
   ```

   처음 추가 후 같은 명령에서 `--ez pin true`를 빼고 다시 실행하면 이미 설치된 probe의 모든 크기를 같은 시작/종료 시점으로 재게시한다. 추가 화면에 걸린 시간 때문에 시작 전 상태를 놓친 경우 이 방법으로 다시 시작한다.

3. 60초 이상 같은 진행 상태의 숫자 감소와 눈금·접근성 설명, 시작 및 종료 전환, 종료 후 0초 유지, 화면 꺼짐/복귀와 앱 프로세스 종료를 확인한다. probe의 클릭은 상세/새로고침/크기 조절 액션 식별 화면을 여는 검사용 동작이므로 실제 일정 상세·경계 알람 검증과 구별한다.
4. `--es source today`로 현지 시계·자정 복귀를 확인한다. `--ez dark true --ez board true --ez long_title true --ef font_scale 2.0 --es locale ko` 등으로 조합을 바꾼다. 긴 제목은 custom fixture에 적용되며 today는 실제 오늘 항목을 사용한다. 다른 `am start`마다 빠진 옵션은 기본값으로 돌아간다.
5. 문서 6/8에서 `--es mode production`은 미지원 안내여야 하고 일반 위젯 설정의 초 옵션도 노출되지 않아야 한다. 기존 문서 9에서는 production/candidate 모두 V7을 사용해야 한다. 미지원 호스트에서 candidate는 Chronometer로 조용히 대체하지 않고 미지원 안내를 표시한다.
6. 아래 실행 검증을 버전별로 통과한 뒤에만 해당 버전을 `productionProfile`에 승격한다. 기기명·OS 빌드·실제 문서 버전·런처 버전·검증한 APK를 함께 기록한다.

## 필요한 실행 검증

- **실시간성**: 고정 미리보기/최초 inflate뿐 아니라 60초 이상 숫자·눈금·현재 접근성 설명이 함께 갱신되는지, 앱 프로세스가 없어도 동작하는지 확인.
- **상태**: 시작 전→진행→0초/종료, 경계 방송 지연 시 0 유지, 지난 링크 클릭, 반복·자동 다음 일정 전환.
- **시계**: 분·시간·자정, 화면 꺼짐/복귀, 날짜/시간대/수동 시각 변경·재부팅. 오늘은 기존 현지 하루 의미를 보존.
- **디자인**: 실제 위젯 렌더러의 밝게/어둡게·작은 크기·큰 글자·긴 한/영 제목·빈/오류 상태와 상세/새로고침 48dp 터치·접근성 설명.
- **성능**: 초 수준 프레임 요청인지, 연속 애니메이션으로 변하지 않는지, 화면이 꺼진 동안의 갱신·CPU/배터리 비용. 이전 API 37 에뮬레이터 기록을 저사양 Samsung 기기의 통과 증거로 사용하지 않음.

조사와 후속 구현까지 수행했으며 코드 대조·XML 파싱·diff 공백 검사를 진행했다. 공식 소스의 임시 사본은 `/private/tmp/timeleft-seconds-20261005/`에 있고 앱에 포함하지 않는다. **빌드·Lint·단위/계측 테스트·새 APK 설치·기기 화면 검수는 수행하지 않았다.** 일반 사용자의 지원 범위는 동일하며, 문서 6/8 후보를 실제 제품 문서로 검증할 수 있는 코드가 이번 결과다. 밝게/어둡게·작은 화면·큰 글자·긴 제목·빈/오류 상태의 시각 검수와 실제 기기 전력 측정도 남아 있다.
