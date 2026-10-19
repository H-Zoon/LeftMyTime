# 미검증 런처용 초 위젯 — Chronometer 하이브리드

2026-10-19. 삼성 One UI Home처럼 draw-instruction 위젯을 받으면 죽는 런처([원인](WIDGET_SECONDS_LAUNCHER_COMPAT_2026-10-19.md))에서도 숫자만 1초 단위로 보이도록 XML `Chronometer` 카운트다운을 추가했다. 사용자 결정: **미검증 런처에서만 사용, 종료는 정확 알람으로 교체, 눈금은 마지막 갱신 기준.** **빌드·단위·Lint·폰 계측 테스트가 통과했고 실기기에서 Chronometer 카운트다운·정확 알람 등록·알람 수신 재렌더·크래시 없음을 확인했다. 종료 시점 교체 전 과정과 `reapply` 수정의 런처 화면 확인은 남아 있다**(아래 ‘검증 결과’).

## 전달 방식 판정

`WidgetSecondsSupport.delivery(context)`가 위젯마다 다음 중 하나를 고른다. 순수 판정 `WidgetSecondsDelivery.resolve`를 단위 테스트한다.

| 조건 | 전달 | 사용자에게 보이는 것 |
| --- | --- | --- |
| API<35, 문서 버전 미지원, 원격 스위치 꺼짐 | `Unavailable` | 정적 위젯, 스위치 없음 |
| 기본 홈 앱이 Launcher3/Pixel Launcher(검증 목록) | `DrawInstructions` | 기존과 같음: 숫자·눈금·상태가 함께 실시간 |
| 그 외 런처 + 정확 알람 허용 | `Chronometer` | 숫자만 매초 감소, 눈금·갱신 시각은 마지막 갱신 기준 |
| 그 외 런처 + 정확 알람 미허용 | `NeedsExactAlarm` | 정적 위젯, 설정 화면에 허용 안내와 설정 열기 버튼 |

**정확 알람이 없으면 Chronometer를 쓰지 않는다.** Chronometer는 0 이후 음수로 계속 진행하므로(`−00:52`, 9월 26일 대조) 종료 순간에 위젯을 바꿀 수단이 없으면 음수가 최대 10분(비정확 알람 창)까지 보일 수 있다. `Chronometer`를 쓰는 조건에서만 `SCHEDULE_EXACT_ALARM`(이미 Manifest에 선언, 집중 타이머가 같은 허용을 사용)이 필요하다.

## 동작

- **대상 시각:** 시작 전이면 시작 초, 진행 중이면 종료 초. 오늘은 그날 마지막 초(23:59:59). 계획은 기존 `WidgetSecondsPlan`을 그대로 사용하므로 오늘·유효한 7일 이내 시간 일정만 대상이고 집중·날짜·기간 요약은 제외된다. 종료된 계획은 카운트다운 없이 기존 정적 ‘종료’ 화면이다.
- **Chronometer 기준값:** `elapsedRealtime + 남은 ms + 999`. Chronometer는 0 방향으로 절사하므로 이 보정으로 대상 직전까지 `00:01`, 대상 시각부터 `00:00`, 첫 음수 텍스트는 대상 이후 약 2초 뒤(`−00:01`)가 된다. 이 2초가 정확 알람이 위젯을 바꾸는 여유다. 단위 테스트가 이 경계를 확인한다.
- **정확 알람:** `WidgetBoundarySchedule`에 `ACTION_EXACT`와 별도 저장소(`widget_boundaries_exact`)를 추가했다. 카운트다운 위젯은 대상 시각 **1초 전**(`WidgetCountdown.alarmAtMillis`)을 정확 경계로, 기존 15분 스냅샷 갱신을 비정확 경계로 함께 기록한다. 그 알람으로 깬 렌더가 아직 대상 시각 전(웜 앱)이면 대상 시각 자체를 다시 건다. 정확 경계는 `setExactAndAllowWhileIdle(RTC_WAKEUP)`로 걸며, 권한이 없거나 취소되면 기존 `setWindow`로 내려간다. 삭제된 위젯의 경계는 매번 정리한다. 알람이 울리면 모든 위젯을 다시 그리고 다음 경계를 다시 건다. 기존 비정확 경계·WorkManager 15분 복구·날짜/시간대 변경 방송은 그대로다.
- **눈금:** 마지막 렌더 시점의 값. ‘N 기준’ 갱신 시각 라벨을 그대로 보여 눈금이 실시간이 아님을 알린다. 짧은 일정(수 분)에서는 눈금이 종료까지 거의 움직이지 않는다.
- **레이아웃:** `app_widget_single`·`app_widget_board`에 숨김 그룹(`widgetSecondsGroup`: `Chronometer` + 상태 문구)을 추가했다. 기존 `createSingleWidgetViews`가 제목·눈금·범위·크기 검사를 그대로 수행하고, 숫자가 들어가지 않으면 기존 ‘위젯을 늘려 주세요’ 화면을 쓴다. 상태 문구(‘남음’/‘시작까지’)는 숫자 옆, 너비가 부족하면 아래에 놓는다. 글꼴·색·크기는 기존 `WidgetText.Value/Label`, `WidgetPalette`, `widget_*` dimen을 사용한다.
- **설정 화면:** 스위치는 `Unavailable`이 아니면 보인다. 힌트는 전달 방식별로 다르다(Chronometer: 숫자만 1초 단위). `NeedsExactAlarm`이면 안내와 ‘정확한 알람 허용’ 버튼(시스템 설정)을 보이고, 화면이 다시 열릴 때(onResume) 허용 상태를 다시 읽는다. 허용 변경은 기존 `BootReceiver`가 위젯을 다시 그리는 경로로 반영된다.
- **접근성:** Chronometer 텍스트는 TalkBack이 읽는 시점의 값을 읽는다. 시간이 들어간 고정 설명은 붙이지 않는다. 터치 영역은 기존 48dp 새로고침과 루트 탭을 유지한다.
- **미리보기/핀:** 설정 화면 미리보기는 한 시점으로 고정(Chronometer 정지), 핀 미리보기는 정적이다.

## 바뀌지 않은 것

DB·설정 키(`${id}seconds`)·초 기본값(꺼짐)·alpha20 의존성·출시 버전·검증된 런처의 기존 draw-instruction 경로. 사용자가 저장한 초 선택은 런처를 바꿔도 유지된다.

## 한계와 위험 (미검증 포함)

1. **정확하지 않은 부분이 있다:** 눈금은 마지막 갱신 기준이다. 콜드 스타트 알람 지연이 실측 약 1.8초로 음수 여유(약 2초)에 가까워, 알람을 1초 앞당겨 여유를 약 1.2초로 늘렸다(위 ‘보완’, 실기기 미확인). 삼성 절전·Doze로 지연이 더 길어지면 `−00:01`이 잠깐 보일 수 있고 정확 알람이 있어도 보장은 아니다.
2. **권한을 거둔 경우(실측 확인):** 권한이 회수되면 시스템이 정확 알람을 지우고 앱을 종료하며 위젯은 재렌더되지 않는다. 다음 갱신(15분 복구 작업·경계 알람·새로고침)까지 이미 올라간 Chronometer가 계속 진행하고 음수로 갈 수 있다. 이후 렌더는 정적으로 내려간다.
3. **판정은 기본 홈 앱 기준 추정**(앞선 문서와 동일). 검증 목록 밖의 모든 런처가 Chronometer를 받는다. 삼성 외 제조사 런처에서의 Chronometer 틱·절전 정책은 확인하지 않았다.
4. **이 단말에서의 Chronometer 틱은 사용자가 probe의 `chrono` 모드로 종료 후 음수까지 진행됨을 확인**했다. 제품 위젯(크기별 레이아웃, 다크/보드, 큰 글자, 긴 제목, 한/영)은 확인하지 않았다.
5. API 35 미만(Android 8~14)은 이번 범위가 아니다. `available()`이 거짓이면 정적 위젯이다. Chronometer 자체는 minSdk 26에서 동작하므로 확대는 별도 결정이다.
6. 배터리: 숫자는 호스트가 갱신하고 앱은 정확 알람(경계마다 1회, 화면 꺼짐 시 깨움)만 쓴다. 실기기 전력은 측정하지 않았다.

## 추가·변경한 소스

신규: `WidgetCountdown.kt`(대상·기준값·최대 텍스트), 단위 테스트 `WidgetCountdownTest`(9)·`WidgetSecondsDeliveryTest`(4), 계측 `WidgetCountdownPlatformTest`(3). 변경: `WidgetSecondsSupport`(판정), `WidgetRendering`(`countdown` 매개변수), `AppWidget`(게시·경계 기록·클릭), `WidgetBoundaryReceiver`(정확 알람), `WidgetBoundaryPlanner`(상수 공유), `AppWidgetConfigure`, 두 레이아웃, 한/영 문자열 3개씩. `WidgetSecondsLauncher`의 전달 목록은 그대로다.

## 검증 결과 (2026-10-19 14:30~14:38)

공유 작업 트리를 그대로 복사한 스크래치 사본에서 실행했다(Android Studio 등 다른 Gradle과의 `R.jar` 경합 회피). 사본과 작업 트리의 소스는 같다.

- **단위 테스트:** 150개 실패 0. 이번에 추가한 `WidgetCountdownTest` 9, `WidgetSecondsDeliveryTest` 4와 앞서 추가한 `WidgetSecondsLauncherTest` 3 포함.
- **빌드:** `assembleDebug`·`assembleDebugAndroidTest` 성공.
- **Lint:** `HEAD` 179건(경고 172·힌트 7), 이 변경 179건. 이슈 id·파일·메시지 대조로 **새 경고 0, 사라진 경고 0**, 오류 0. 첫 실행에서 `AppWidgetConfigure`의 `Uri.parse`에 UseKtx 권고가 1건 생겨 `String.toUri`로 고친 뒤 다시 확인했다.
- **계측 `WidgetCountdownPlatformTest` 3개 통과 — SM-A336N 실기기(One UI 8)에서 실행.** Gradle `connected` 작업은 종료 후 앱을 제거해 데이터가 지워질 수 있어 쓰지 않고, 테스트 APK를 설치해 `adb shell am instrument -e class …`로 이 클래스만 실행한 뒤 테스트 패키지만 제거했다. 검증 내용: 단일형·보드형 모두 `Chronometer`가 카운트다운으로 보이고 `percent`가 숨겨짐, 진행 중은 ‘남음’·시작 전은 ‘시작까지’, 시계 형식의 텍스트, 폭이 모자라면 기존 ‘늘려 주세요’ 화면. 폰 화면은 꺼져 있었고 위젯을 홈에 올리는 검사는 아니다.
- **설치:** 같은 사본의 debug APK(SHA-256 `df75b849…`)를 `adb install -r`로 설치(`lastUpdateTime` 14:37:08, `firstInstallTime`·데이터 유지). 설치 뒤 `logcat -b crash`는 5건 그대로(마지막 13:47:58.890)다.

**못 한 확인 — 폰에 위젯이 없다.** 폰의 `firstInstallTime`이 14:16:10이고 내 설치(14:37:08) 전에 이미 새 uid(`u0a1055`)였다. 14:16에 누군가 앱을 제거 후 재설치하면서 홈 화면 위젯(5, 6, probe 7)이 모두 삭제된 것으로, 이 변경의 설치가 원인은 아니다. 그래서 다음은 아직 확인하지 못했다.

- 이 단말에서 `Seconds delivery`가 `Chronometer`/`NeedsExactAlarm` 중 무엇이 되는지(앱의 정확 알람 접근은 `appops`가 ‘default’라 미확정)
- Chronometer 숫자가 매초 줄어드는 실제 위젯 화면, 정확 알람 등록(`dumpsys alarm`의 `REFRESH_BOUNDARY_EXACT`), 종료 시점 교체, 크래시 없음
- 설정 화면의 허용 안내·버튼·복귀 후 전환

### 폰 위젯 확인 (17:32~17:40, 사용자가 위젯 추가·초 표시 켬)

폰이 USB에서 한동안 빠져 있다가(14:38~17:40) 다시 연결돼 확인했다. 위젯 9(제품 `AppWidget`, 삼성 홈 앱 호스트)와 probe 8이 등록돼 있다.

| 항목 | 결과 |
| --- | --- |
| 전달 판정 | `Seconds delivery=NeedsExactAlarm launcher=com.sec.android.app.launcher` (17:32:40, 17:38:14). 이 앱은 정확 알람 접근이 ‘default’(미허용)라 설계대로 정적 위젯(`seconds=false`)으로 게시 |
| 화면 | ‘오늘의 · 10. 19. 오후 5:38 기준 · 6시간 21분 남음’ + 눈금(정적). 표시 값이 23:59:59까지의 남은 시간과 일치 |
| 정확 알람 | `REFRESH_BOUNDARY_EXACT` 등록 0건(정적이라 정상). 비정확 15분 경계(`REFRESH_BOUNDARY`, 17:47:37 창 10분)만 1건 |
| 크래시 | crash 버퍼 5건 그대로(마지막 13:47:58). 런처 PID 31806이 13:48:01부터 3시간 52분 유지, 위젯 9 게시 후 8분 이상 안정 |

즉 **삼성 홈 앱 + 정확 알람 미허용 경로는 크래시 없이 정적으로 동작함을 실기기에서 확인**했다. **Chronometer 경로(숫자 매초 감소, 정확 알람 등록, 종료 교체)는 정확 알람 접근이 허용되지 않아 아직 실행되지 않았다.** 설정 화면의 허용 안내·버튼 표시는 눈으로 확인하지 못했다.

### Chronometer 실기기 확인 (17:42~17:52, 사용자 승인으로 `appops`에서 `SCHEDULE_EXACT_ALARM` 임시 허용 후 default로 복원)

| 항목 | 결과 |
| --- | --- |
| 허용 직후 | `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`로 위젯이 다시 그려져 `Seconds delivery=Chronometer`, `seconds=true`(17:42:05) |
| 화면 | ‘오늘의 · 5:42 기준 · **6:17:39 남음**’. 17:42:20에 `6:17:39`, 17:43:04에 `6:16:55`, 17:43:13에 `6:16:46`로 23:59:59와 정확히 일치하고 매초 감소. 눈금은 ‘N 기준’ 스냅샷 |
| 앱 프로세스 없이 | `am kill`로 앱 프로세스 제거(PID 없음) 뒤에도 숫자가 계속 감소, 정확 알람 유지 |
| 정확 알람 등록 | `REFRESH_BOUNDARY_EXACT`: `RTC_WAKEUP`, `window=0`, `exactAllowReason=permission`, 오늘 **23:59:59.000** (계산한 대상 시각과 동일) + 비정확 15분 경계 1건 |
| 크래시 | crash 버퍼 5건 그대로, 런처 PID 31806 유지 |
| 알람 → 수신기 → 재렌더 | 임시 계측 프로브(저장소 미포함)로 위젯 9에 20초 뒤 정확 경계를 걸고 프로세스가 죽은 상태에서 대기: 두 번 모두 알람이 울려 `Boundary refresh received: exact=true` 후 `Widget submitted` 확인 |
| 권한 회수(`appops default`) | 시스템이 정확 알람을 제거하고 앱 프로세스를 종료. **위젯은 재렌더되지 않아 Chronometer가 계속 진행**(다음 갱신 전까지) |

참고: 18:24 시점에 crash 버퍼가 5건에서 0건으로 비어 있었다(누군가 로그를 지운 것으로 추정). 런처 PID 31806은 13:48:00부터 그대로라 그동안 런처 재시작은 없었다.

**측정한 알람 지연(콜드 스타트, 두 번):** 예정 시각 대비 수신기 +1.13초·+1.10초, 위젯 게시 +1.69초·+1.77초. 런처 반영까지 포함하면 약 +1.8초라, **첫 음수(`−00:01`)가 나타나는 +1.999초와 여유가 0.2초 안팎**이다. 앱 프로세스가 살아 있는 웜 상태 지연은 측정하지 못했다(위젯 새로고침이 임시 알람을 덮어써 측정이 무효가 됨). 따라서 실제 종료 시점에는 가끔 `−00:01`이 잠깐 보일 수 있다.

**발견해 고친 버그:** 정확 알람 없이 정적 렌더(`NeedsExactAlarm`)로 바뀐 뒤에도 **옛 Chronometer가 계속 보이고 줄어들었다**(17:48:35 `6:11:24` → 6초 뒤 `6:11:18`). 삼성 런처가 새 RemoteViews를 기존 뷰 트리에 `reapply`하는데 정적 경로가 카운트다운 그룹의 숨김과 `percent` 표시를 되돌리지 않았기 때문이다. 종료 시점 교체도 같은 이유로 실패했을 것이다. 정적 경로가 그룹을 숨기고 `percent`를 다시 보이며 숨은 Chronometer를 정지하도록 고쳤다(`WidgetRendering`). 회귀 테스트 `aStaticRenderReappliedOverACountdownHidesTheOldChronometer`를 추가했고, 폰에서 계측 4개(`WidgetCountdownPlatformTest`)가 통과했다. 수정 빌드(SHA-256 `990ceb5c…`)는 17:50:39에 설치됐다. **런처 화면에서 옛 Chronometer가 사라지는지는 눈으로 확인하지 못했다**: 그 시각에 전경 앱이 다른 앱이라 홈을 볼 수 없었고, 다음 위젯 갱신(15~25분 안, 또는 새로고침 탭)이 있어야 반영된다.

**보완(구현, 같은 날):** 정확 알람을 대상 시각 1초 전에 걸고(`ALARM_LEAD_MILLIS`), 그 갱신에서 아직 진행 중이면 대상 시각에 다시 건다. 실측 지연 기준으로 콜드 스타트는 +0.8초 부근에서 종료 화면으로 바뀌어 여유가 약 1.2초(전 0.2초), 웜은 +0.5초 부근에서 바뀌어 약 1.5초다. 알람만 일찍 거는 것이라 종료 화면이 대상 시각 전에 뜨지 않는다. 단위 테스트 4개(`WidgetCountdownTest`: 알람 시각 경계, 콜드·웜 시뮬레이션, 보완 전 여유 0.2초 문서화)가 통과했다. **실기기 확인(18:24, 사용자 승인으로 `appops` 임시 허용 후 default 복원, 화면은 건드리지 않음):** 새 빌드(SHA-256 `65a5c57f…`)를 설치하고 허용하자 `Seconds delivery=Chronometer`, `seconds=true`로 다시 그려졌고 `REFRESH_BOUNDARY_EXACT`가 `RTC_WAKEUP`, `window=0`, `exactAllowReason=permission`, **`origWhen=2026-10-19 23:59:58.000`**으로 등록됐다(보완 전 23:59:59보다 1초 앞). 복원 후 시스템이 알람을 지웠다. 아직 확인하지 못한 것: 실제 23:59:58 이후의 알람 수신·종료 화면 교체(음수 없음)와 웜 상태 동작. 이 확인은 정확 알람 허용을 오늘 자정까지 유지해야 하며, 화면은 폰 잠금 상태라 보지 못했다.

## 남은 확인 (정확 알람 허용 후)

1. `./gradlew testDebugUnitTest assembleDebug lintDebug`, 계측 `WidgetCountdownPlatformTest`·`WidgetSecondsPlatformTest`. 기존 경고와 새 문제를 구분한다.
2. SM-A336N: 초 위젯 설정 후 `Seconds delivery=Chronometer`, 숫자 매초 감소, 종료 시점에 정적 ‘종료’ 화면으로 교체(음수 없음), 화면 꺼짐/켜짐·프로세스 종료·재부팅 뒤 지속, `logcat -b crash`에 신규 크래시 없음.
3. 정확 알람 허용을 끈 상태: `NeedsExactAlarm` 안내와 정적 위젯, 허용 후 복귀 시 Chronometer 전환.
4. 오늘(자정 직전·직후), 시작 전→진행→종료 전환, 밝게/어둡게·보드·한/영·큰 글자·긴 제목·작은 크기.
5. Pixel Launcher/Launcher3 에뮬레이터에서 기존 draw-instruction 초 위젯 회귀 없음.
