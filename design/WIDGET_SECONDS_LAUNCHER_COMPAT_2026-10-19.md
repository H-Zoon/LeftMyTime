# 초 위젯 런처 호환성 판정 보완

2026-10-19. 연결된 Galaxy A33 5G(SM-A336N)에서 초 위젯을 게시하면 삼성 홈 앱이 종료되는 문제를 읽기 전용으로 분석하고, 전달 조건을 고쳤다. **빌드·테스트·Lint는 통과했고 기기에서 크래시 재발이 없음을 확인했다. 런처를 전면에 둔 통제 실험·화면 검수는 하지 못했다(아래 ‘검증 결과’).**

> **후속(같은 날):** 미검증 런처에는 정적 위젯 대신 숫자만 초 단위인 Chronometer 대체를 추가했다(정확 알람 허용 시). 아래 ‘정적 위젯 게시’·‘스위치 숨김’은 **정확 알람을 쓸 수 없는 경우**로 한정해 읽는다. [설계·한계](WIDGET_SECONDS_CHRONOMETER_2026-10-19.md).

## 확인한 사실 (기기 로그)

| 항목 | 값 |
| --- | --- |
| 기기 | SM-A336N, Android 16 / API 36, One UI 8.0 (`A336NKSUBGYI3`) |
| 호스트 | `com.sec.android.app.launcher` One UI Home 17.0.05.19 (기본 홈 앱) |
| 앱 | 6.6(23), 문서 버전 6 → V6 제품 문서 |
| 크래시 | `NullPointerException: getPackage(...) must not be null` at `HoneyAppWidgetHostView.updateAppWidgetInternal` (SourceFile:172), 런처 프로세스 종료 5회 |

- 위젯 5(`seconds=false`, 일반 XML)는 같은 런처에서 정상이다.
- 위젯 6(`seconds=true`)을 게시한 직후에만 종료된다. `Widget submitted … seconds=true`와 크래시 시각 차이는 약 0.003~0.2초였다(13:33:50, 13:34:38, 13:47:58).
- 디버그 probe(`remote_epoch`, V6)를 게시한 13:32:54·13:33:07 뒤에도 같은 크래시가 났다(13:32:57, 13:33:07). 앱 위젯 코드가 아니라 **초 위젯 전달 형식(DrawInstructions)** 자체가 원인이다.
- 크래시 사이에 재시작한 런처는 다음 게시가 오기 전까지 종료되지 않았다. 종료는 새 문서가 도착할 때마다 발생했고 반복 종료 루프는 관찰하지 못했다.

**추정(미확정):** `RemoteViews(DrawInstructions)`는 레이아웃/패키지 정보를 가지지 않고(공개 소스의 `mApplication`에 초기값 없음 — 생성자 본문은 이번에 확인하지 못함), 삼성 런처가 `getPackage()`를 null 불가로 읽는다. 삼성 내부 코드는 확인할 수 없으므로 패키지 값을 채우는 우회는 시도하지 않았다.

## 원인과 기존 판정의 빈틈

`WidgetSecondsSupport`는 `DrawInstructions.getSupportedVersion()`으로 판정한다. 이 값은 **앱 프로세스의 프레임워크**가 지원하는 문서 버전이며 홈 앱이 그 문서를 그릴 수 있다는 뜻이 아니다. 10-06 검수는 AOSP Launcher3(문서 6)와 Pixel Launcher(문서 8/9)만 대상이었고, 삼성 런처는 미검증으로 남아 있었는데 문서 버전 6이 곧 제품 지원으로 처리됐다.

## 변경

- **`WidgetSecondsLauncher`(신규):** 기본 홈 앱 패키지를 읽고 `com.android.launcher3`, `com.google.android.apps.nexuslauncher`만 검증 목록으로 인정한다. 목록 밖·선택 창(`android`)·읽기 실패는 모두 미검증이다. 삼성 런처가 실패한 근거는 같은 파일의 주석에 남겼다.
- **`WidgetSecondsSupport.delivery(context)`:** `Unavailable`(OS·원격 설정 미지원) / `UnverifiedLauncher` / `Ready`. 기존 `available()`은 프레임워크·원격 설정만 판단하며 계측 테스트·probe가 그대로 쓴다.
- **`AppWidget`:** 초 문서는 `Ready`일 때만 만든다. 미검증 런처에서는 저장된 `showSeconds` 값을 지우지 않고 정적 위젯을 게시하며 `WidgetSeconds` 로그에 런처 패키지를 남긴다. 정적 게시가 시스템에 저장된 이전 초 문서를 대체하므로 기존 설치도 앱 업데이트·다음 갱신에서 복구된다. 경계/WorkManager 일정은 기존 정적 위젯 규칙을 따른다.
- **설정 화면:** 미검증 런처에서는 ‘초 단위 표시’ 스위치를 숨기고, 이미 켜져 있던 경우 ‘현재 홈 앱은 초 단위 표시가 확인되지 않아 마지막 갱신 시각을 함께 보여줍니다.’를 보여준다(한/영 문자열 추가). 미리보기도 같은 판정을 써서 실제 위젯과 같아진다.
- **Manifest:** 기본 홈 앱 조회를 위해 `<queries>`에 `MAIN`/`HOME` intent를 추가했다. 새 권한은 없다.
- **debug probe:** 미검증 런처에서는 문서를 게시하지 않고 정적 안내를 보인다. 호스트 실험은 `--ez force true`로 명시해야 한다. 예) `adb shell am start -W -f 0x10008000 -n com.devidea.timeleft/com.devidea.timeleft.design.WidgetSecondsProbeActivity --es mode candidate --ez force true`
- **테스트 소스:** `WidgetSecondsLauncherTest`(허용 2개, 삼성·미지정·대소문자·선택 창·null 거부). `WidgetSecondsPlatformTest`의 제품 오늘 미리보기 전제를 `canDeliver(context)`로 맞췄다.

재사용: `WidgetSecondsCapability`, `WidgetSecondsPlan`, `WidgetSecondsRenderer`, 설정의 기존 `TimeLeftSwitchRow`·안내 문구 위치. DB·설정 키·출시 버전·alpha20 의존성·초 기본값(꺼짐)은 바꾸지 않았다.

## 남은 문제와 한계

- **판정은 기본 홈 앱 기준 추정이다.** AppWidgetManager는 위젯을 실제로 담은 호스트를 알려주지 않는다. 검증된 런처가 기본인데 다른 호스트가 위젯을 담은 경우는 가려내지 못한다. 위젯 XML의 `widgetCategory`는 `home_screen`뿐이라 잠금 화면 호스트는 대상이 아니다.
- **허용 목록은 정적이다.** 삼성이 런처를 고쳐도 자동 인식하지 않는다. 확인하려면 해당 기기에서 probe를 `--ez force true`로 실행하고 결과를 이 목록과 문서에 반영한다. Samsung 외 제조사 런처(Nova, Lawnchair, Xiaomi, Oppo 등)는 미검증이라 정적 위젯이다.
- **삼성 기기에서는 draw-instruction 초 위젯을 쓸 수 없다**(Chronometer 대체는 숫자만). 사용자가 보는 기능 손실이며, 10-06에 확대한 지원 범위(문서 6/8/9 이상)는 실제로는 Pixel Launcher/Launcher3 계열로 줄었다. 앞선 `QA_WIDGET_SECONDS_COMPATIBILITY_2026-10-06.md`와 `PRODUCT_REVIEW.md`의 ‘지원 확대’ 문장은 이 제한을 적용해 읽는다.
- **미검증:** 런처를 전면에 두고 갱신을 보낸 통제 실험(아래 이유로 불가), 설정 화면의 문구·스위치 숨김 화면, 위젯 실제 화면, 삼성 외 기기, Pixel Launcher/Launcher3 에뮬레이터 회귀.

## 검증 결과 (2026-10-19 14:00~14:07)

**빌드·테스트·Lint** — 공유 작업 트리에는 다른 세션의 작성 중 코드(`PointHomeIntro.kt`가 아직 없는 `pointhome_intro_*` 문자열을 참조)가 있어 그대로는 `compileDebugKotlin`이 실패했다. 이 변경과 무관하다. 그 작업은 건드리지 않고 `HEAD`(`d62768a`)에 이 변경의 파일 10개만 얹은 격리 사본에서 실행했다.

- `testDebugUnitTest` **126개 실패 0**(신규 `WidgetSecondsLauncherTest` 3개 포함), `assembleDebug` 성공, `lintDebug` 성공.
- Lint: `HEAD` 그대로 경고 171·힌트 7, 이 변경 포함 경고 171·힌트 7. 이슈 id·파일·메시지를 대조해 **새 경고 0건, 사라진 경고 0건**. 오류 0. 공유 트리에서 먼저 돌린 Lint의 174는 다른 세션의 변경이 섞인 값이다.
- 격리 사본 APK는 `ba6b221b1a93…`이며 **기기에 설치하지 않았다.** 계측 테스트(`connectedAndroidTest`)는 실행하지 않았다.

**기기(SM-A336N)** — 다른 세션이 공유 트리에서 14:03:29에 설치한 debug 빌드가 이미 이 변경을 포함하고 있었다(dex에 `WidgetSecondsLauncher`·`Seconds skipped`, 리소스에 새 문자열). 그 세션이 쓰는 빌드를 덮어쓰지 않으려고 격리 APK는 설치하지 않고 이 빌드로 확인했다. 이 빌드에는 다른 세션의 코드도 들어 있어, 검증한 APK가 위 격리 APK와 같은 파일은 아니다.

| 항목 | 변경 전 | 설치 후 |
| --- | --- | --- |
| 위젯 6 게시 | `seconds=true` | `Seconds skipped: launcher=com.sec.android.app.launcher is not verified` → `seconds=false` |
| 런처의 위젯 6 수신 | 게시 직후 0.003~0.2초 안에 `getPackage(...)` NPE로 종료 | 14:03:32.372·.436 `HoneyAppWidgetHostView` `updateAppWidget` 수행 → `prepareView` 완료, 예외 없음 |
| crash 버퍼 | 5건(마지막 13:47:58.890) | 5건 그대로, 이후 신규 없음 |
| 런처 프로세스 | 크래시마다 재시작 | PID 31806이 13:48:01부터 17분 54초 지속(설치·재게시 구간 포함) |

- 위 수신 구간은 런처가 위젯을 받는 중이던 13:59:07~14:03:32.746 안이다. 14:04:22 주기 갱신도 같은 건너뛰기를 기록했다.
- 같은 로그에서 홈 앱 패키지가 `com.sec.android.app.launcher`로 조회됐으므로 `<queries>`를 넣은 상태에서 조회가 동작한다.
- 13:48:02의 `LauncherExceptionParser`의 `getPackage(...)` 줄은 재시작한 런처가 직전 크래시(13:47:58)를 기록한 것이며 새 크래시가 아니다.

**하지 못한 확인:** 폰이 잠금·화면 꺼짐(`Dozing`, 키가드 표시) 상태라 HOME 키가 적용되지 않았고, `APPWIDGET_UPDATE`는 시스템 전용 브로드캐스트라 `adb`로 보낼 수 없다. 그래서 런처를 전면에 두고 갱신을 직접 일으키는 통제 실험은 하지 못했다. 위 결과는 설치 직후 한 번의 수신 구간과 이후 약 4분의 관찰이다. 잠금을 풀고 홈 화면에서 위젯의 새로고침을 눌러 같은 로그를 다시 보면 보강된다.

## 남은 확인

1. 홈 화면에서 위젯 새로고침·화면 켜짐/꺼짐 반복 뒤 `logcat -b crash`에 신규 `FATAL EXCEPTION`이 없는지.
2. 위젯 설정 화면에서 스위치 숨김과 안내 문구(한/영, 큰 글자)를 눈으로 확인.
3. Pixel Launcher/Launcher3 에뮬레이터에서 초 위젯이 기존처럼 게시되는지(회귀). 계측 `WidgetSecondsPlatformTest`도 이 때 함께 실행한다.
