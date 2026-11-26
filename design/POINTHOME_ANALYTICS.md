# PointHome 진입 집계 — 2026-10-29

사용자 요청: 플로팅 버튼의 전체·일별 진입 횟수와 FCM 리워드 알림으로 진입한 이용자 수를 Firebase Analytics에서 확인한다. 기존 `AppTelemetry`를 재사용하며 별도 서버·회원 ID·기기 내 누적 카운터를 추가하지 않는다.

## 이벤트 정의

| 이름 | 파라미터 | 허용 값 | 기록 시점 |
| --- | --- | --- | --- |
| `pointhome_open` | `entry_source` | `floating_button` | 플로팅 버튼 builder의 SDK `serviceOpen(BUTTON)` |
| `pointhome_open` | `entry_source` | `reward_push` | 리워드 알림에서 시작한 builder의 SDK `serviceOpen(NATIVE)` |

하나의 진입은 이벤트 하나다. `start()` 호출/반환, 버튼 생성·노출, FCM 수신·알림 표시만으로 기록하지 않는다. 원격 진입 OFF·SDK 실패·일반 알림은 성공 진입으로 기록하지 않는다. 별도의 ‘일일 이벤트’는 만들지 않고 Analytics가 수집한 이벤트 시각과 속성의 보고 시간대를 이용한다. 한국 일별 집계가 필요하면 GA4 속성 시간대를 `Asia/Seoul`로 확인한다.

기존 파라미터 없는 `UsageEvent`는 유지하고 `recordPointHomeOpened(PointHomeEntrySource)`만 닫힌 enum 값을 파라미터로 받는다. 알림 제목·본문·FCM ID·토큰·캠페인 이름·계정 ID·일정 내용·정확한 일정 날짜를 추가하지 않는다. Firebase의 기본 설치 식별 및 수집 정책은 기존과 같다.

SDK 3.7.0 배포 AAR의 공개 API와 bytecode에서 플로팅 `BUTTON`·직접 `start()`의 `NATIVE` 호출 경로 및 listener의 약한 참조 전달을 확인했다. MainActivity가 두 builder를 각각 보유한다. listener는 열린 상태의 중복 콜백을 무시하고, 같은 caller의 닫힘 뒤 다음 열림은 다시 기록한다. 알림 요청 소비와 저장 상태 복원은 기존 `PointHomePushOpen`·MainActivity 흐름을 유지한다. SDK가 보고하는 화면 열림은 광고 시청 완료·보상 지급·웹 콘텐츠 로딩 완료를 뜻하지 않는다.

## Console 설정과 조회

코드 배포 외에 연결된 GA4 속성에서 한 번 설정한다. 이번 작업에서 GA4 Console 설정이나 실제 알림 발송은 수행하지 않았다.

1. GA4 관리의 **맞춤 정의**에서 이벤트 범위 맞춤 측정기준을 만든다. 표시 이름 `PointHome 진입 경로`, 이벤트 매개변수 `entry_source`. 기존 정의가 있으면 재사용한다.
2. **탐색 → 자유 형식**에 측정기준 `날짜`, `이벤트 이름`, `PointHome 진입 경로`와 측정항목 `이벤트 수`, `총 사용자 수`를 추가한다.
3. 필터를 `이벤트 이름 = pointhome_open`으로 설정한다. 행에 날짜, 열에 진입 경로, 값에 이벤트 수·총 사용자 수를 둔다.
4. 전체 집계는 같은 이벤트 필터에서 날짜 행을 빼고 원하는 기간을 설정한다. 플로팅만 보려면 경로를 `floating_button`, 리워드 알림만 보려면 `reward_push`로 필터링한다.

| 원하는 값 | 조회 기준 |
| --- | --- |
| 전체 진입 횟수 | `pointhome_open`의 선택 기간 이벤트 수 |
| 플로팅 전체·일별 진입 횟수 | `entry_source=floating_button`의 기간 합계·날짜별 이벤트 수 |
| 리워드 알림 전체·일별 진입 횟수 | `entry_source=reward_push`의 기간 합계·날짜별 이벤트 수 |
| 리워드 알림으로 진입한 이용자 수 | 같은 경로 필터의 기간 전체 또는 날짜별 총 사용자 수 |

같은 설치에서 하루 3회 진입하면 이벤트 수는 3, 그날 총 사용자 수는 1로 해석한다. 사용자는 Analytics 식별 기준이며 사람·PointHome 회원 단위의 고유 인원은 아니다. 일별 사용자 수나 경로별 사용자 수를 합하면 같은 사용자가 중복될 수 있으므로 기간 전체 인원은 전체 기간 필터로 조회한다. 앱 출시 이전 내역은 소급 생성되지 않는다. ‘전체’는 신규 계측 버전에서 수집된 데이터 중 선택 기간과 보고서 보존 범위의 합계이며 영구 보장 카운터가 아니다.

맞춤 측정기준과 일반 보고서는 반영 지연이 있다(공식 안내 24~48시간). DebugView로 개발 설치의 이벤트·경로를 먼저 확인하고 실제 보고서 수신은 따로 확인한다. 네트워크·수집 실패 등으로 Analytics 수치가 모든 실제 진입과 완전히 일치한다고 보장하지 않는다.

## 검증 범위

정적 검토: SDK 3.7.0 호출 경로와 callback API, 모든 연동 호출부, 허용 파라미터 두 값, 성공 콜백에만 이벤트를 기록하는 위치, 기존 알림 목적지/동의/원격 설정/소비 동작 보존. 외관·버전·DB·사용자 설정 키는 변경하지 않았다. 개인정보처리방침 원본과 웹 게시본에 진입·경로 계측을 설명한다.

빌드·Lint·단위/계측 테스트·설치·실제 SDK/FCM·GA4 수신은 사용자 명시 요청 전이라 실행하지 않았다. 최종 검증 시 다음을 포함한다.

- 플로팅 진입→닫기→재진입 두 번은 `floating_button` 2회, 버튼 생성·백그라운드 복귀만으로 증가하지 않음.
- 리워드 notification+data·data 알림의 포그라운드/백그라운드/프로세스 미실행 각각 `reward_push` 1회. 일반 알림·수신만으로 0회.
- 빠른 연속 터치, SDK의 중복 open 콜백, 회전·재생성·프로세스 복원, SDK 화면에서 기존 홈 재생성, 닫기 후 재진입, pending 중 일반 알림 수신.
- 원격 false·자격값 누락·SDK 초기화/열기 실패는 0회. Analytics 장애가 진입을 막지 않음.
- DebugView 경로별 값, 자정 전후 GA4 속성 시간대별 일 집계, 여러 진입과 사용자 수 차이, 개인정보 입력값 미포함.

공식 참고: [Firebase Android 이벤트 기록](https://firebase.google.com/docs/analytics/android/events), [맞춤 측정기준](https://support.google.com/analytics/answer/14240153), [이벤트 수·총 사용자 수 정의](https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema), [탐색](https://support.google.com/analytics/answer/7579450).

## 2026-10-29 기기 진단 — 플로팅 진입 전송 확인

사용자의 ‘로그와 이벤트 검색에 보이지 않음’ 제보로 연결된 SM-A336N의 기존 설치 앱을 확인했다. 설치 버전은 **6.6(23) debug**이며 현재 소스/최종 배포 산출물의 6.7(24)와 다르다. 설치 APK에도 `pointhome_open`·`entry_source`와 SDK 성공 콜백 계측이 포함돼 있었다. 이번 진단에서 앱 소스 수정·빌드·Lint·단위 테스트·APK 설치는 하지 않았다.

처음에는 Firebase 상세 로그와 Analytics debug 속성이 비어 있었다. 다음 속성을 켜고 앱 재시작 후 확인했다. 기기 진단 속성은 현재 켜둔 상태이며 원래 값은 Git 제외 `design/qa-pointhome-analytics-2026-10-29/device-diagnostic-properties.json`에 보존했다.

```sh
adb shell setprop log.tag.FA VERBOSE
adb shell setprop log.tag.FA-SVC VERBOSE
adb shell setprop debug.firebase.analytics.app com.devidea.timeleft
adb logcat -v time -s FA FA-SVC
```

**16:35:31 KST**에 Google Play 서비스 프로세스의 `FA-SVC`에서 `Logging event: origin=app,name=pointhome_open`과 `entry_source=floating_button`을 확인했다. 같은 앱의 업로드와 HTTP **204** 성공 응답도 이어졌다. 앱 프로세스/PID만 선택한 Logcat에는 이 서비스 로그가 보이지 않는다. Firebase 수집 활성화·SDK 버튼 진입 콜백과 이벤트 전송까지 확인했으며, 콘솔 DebugView/일반 보고서 표시·GA4 맞춤 측정기준 등록·일별 집계·리워드 푸시 경로·닫힘 후 반복 계측은 이번 확인 범위에 포함하지 않는다. 콘솔에서 즉시 확인할 때는 debug 속성을 켠 기기의 Analytics → DebugView를 사용하고, 일반 보고서의 24~48시간 처리 지연과 구분한다. 선별된 전송 증거는 같은 QA 폴더의 `analytics-delivery.log`에 있다.

진단 중 에이전트의 잘못된 디버거 필드 조회로 앱 프로세스가 종료됐다. 앱을 다시 실행해 복구했으며 이 진단 유발 종료는 기존 계측 누락 재현이나 앱 자체 결함으로 취급하지 않는다. 해당 시각의 Crashlytics 항목이 생기면 진단 유발 종료와 구분한다.

진단 종료 뒤 일반 수집 모드로 돌릴 때는 `adb shell setprop debug.firebase.analytics.app .none.`을 사용한다. 상세 로그도 끄려면 `log.tag.FA`·`log.tag.FA-SVC`를 원래 값(빈 값)으로 복원한다.
