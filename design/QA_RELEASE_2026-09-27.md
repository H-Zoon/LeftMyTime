# 스토어 출시 후보 최종 QA — 2026-09-27

## 판단

**빌드·자동 테스트·확인한 에뮬레이터 기능은 통과했다. 스토어 배포 승인은 보류한다.** 공개 스토어의 데이터 보안 표시와 새 선택형 수집 기능의 대조, 이전 디자인의 스토어 배너 교체, 실제 휴대폰에서의 스플래시·런처 확인이 남았다. 이번 작업에서 배포·스토어 수정·커밋은 하지 않았다.

사용자가 빌드·테스트·실행 검수를 명시적으로 승인했다. `develop`의 `01e9d6a` 이후 아이콘 배경·최종 스플래시 리소스를 포함한 작업 트리를 검증했다. 앱 코드 추가 수정은 없으며, 이 문서와 인수인계 기록으로 결과를 남긴다. 이전 문서의 해당 변경 ‘미빌드’ 상태는 이 문서의 실제 수행 범위로 대체한다.

## 빌드와 자동 검증

실행 명령:

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug \
  testReleaseUnitTest assembleRelease bundleRelease lintRelease \
  assembleDebugAndroidTest -x uploadCrashlyticsMappingFileRelease --console=plain
```

| 검사 | 결과 |
| --- | --- |
| 전체 빌드 | 성공, 2분 4초. 170 tasks: 104 executed, 66 up-to-date |
| Debug 단위 테스트 | 100개, 실패·오류·제외 0 |
| Release 단위 테스트 | 동일한 100개를 release 구성에서 실행, 실패·오류·제외 0. 서로 다른 200개 테스트라는 의미는 아님 |
| Debug Lint | 오류 0, 경고 159, 힌트 7 |
| Release Lint | 오류 0, 경고 156, 힌트 5 |
| 이전 Debug Lint 대비 | 검사 ID·메시지·위치 파일 기준 추가 0 / 제거 0. 행 번호 이동 제외. 기존 경고 유지 |
| Release Lint 비교 한계 | 이번 실행 수치는 확보했으나 이전 release 보고서와의 비교 기준은 없음 |
| Android 17 전체 계측 | 17개 보고, 16개 실행 통과, 환경 조건으로 1개 제외. 3.437초 |
| 앱 크래시 버퍼 | 이번 release 실행 검수의 API31/API37에서 각각 0바이트. 모든 ANR·장기 안정성을 보증하는 수치는 아님 |

계측은 `AndroidJUnitRunner`로 실제 에뮬레이터에서 실행했다. 제외된 항목은 미지원 위젯 호스트의 정적 대체 시험(`unavailableHostKeepsTheExistingSnapshotEvenWithASavedSecondsPreference`)이며, API37 지원 환경이므로 assumption으로 건너뛴 결과다. 실패가 아니다. API36 미지원 분기의 과거 실행 기록은 [위젯 초 검수](QA_WIDGET_SECONDS_2026-09-26.md)를 따르며 이번 재실행으로 세지 않는다.

자동 검증에는 날짜·반복·시간대/DST 계산, 시간 숫자 진입 보정, 위젯 초 표시/경계 계획, 백업 코덱, Room 이행, 집중 완료 outbox, 플랫폼 위젯 문서·만료 링크가 포함된다. 실제 자정·DST를 기다리거나 모든 OS의 장기 절전 동작을 실행한 것은 아니다.

## 출시 산출물과 패키지 검사

- 앱 ID `com.devidea.timeleft`, `versionName=6.1`, `versionCode=18`, minSdk26 / targetSdk36. 이번 QA에서 버전 변경 없음.
- R8 축소·리소스 축소를 적용한 서명 APK와 AAB 생성. 개인 키·비밀번호는 출력하지 않았다.
- APK `apksigner verify` 통과(V2 서명). AAB `jarsigner -verify`의 `jar verified` 확인. 자체 서명 인증서의 신뢰 체인·타임스탬프 경고는 미서명 오류와 구별한다.
- APK 16KB zip 정렬 통과. AAB 안의 4개 ABI / 네이티브 라이브러리 8개에서 ELF PT_LOAD 정렬 16384 확인. Android17 에뮬레이터의 실제 페이지 크기도 16384이며 서명 APK 설치·실행 성공.
- release manifest에 `debuggable`, `testOnly`, debug 갤러리·위젯 probe 등록 없음. 광고 ID·AdServices 권한 없음. Analytics/Crashlytics 자동 수집 기본 꺼짐 설정 확인.
- Crashlytics 매핑 업로드 작업은 명시적으로 제외했다. Play Console 업로드, Firebase 콘솔 이벤트 도착, Play 배포용 APK 분할 설치는 수행하지 않았다.
- 로컬 서명 성공은 Play Console에 등록된 업로드 인증서 일치 또는 versionCode18 재사용 가능 여부의 증명이 아니다. 실제 트랙에서 별도 확인한다.

| 파일 | 바이트 | SHA-256 |
| --- | ---: | --- |
| `app/build/outputs/apk/release/app-release.apk` | 5,617,984 | `429463bac9cc9d45429ba0c8228358f0dfbf01ef4c5caeecd3a0a894fedd96e5` |
| `app/build/outputs/bundle/release/app-release.aab` | 7,967,259 | `e2657312c0bee22cac9d5ab3c95e04e50c9f866328bdecd81aec6d45f91ce189` |

향후 앱 코드·리소스·버전 변경 시 위 파일과 해시는 이전 후보가 된다. 새 후보를 생성하고 영향 범위를 재검증해야 한다.

## 실행 환경

| 환경 | 설치 앱 | 실제 확인 범위 |
| --- | --- | --- |
| 기존 Pixel9Pro 에뮬레이터, Android17/API37, Pixel Launcher, 1280×2856, 16KB | 최신 debug APK를 기존 설치 위에 갱신 | 전체 계측·실제 공통 화면을 사용하는 디자인 갤러리·기존 DB 보존 |
| 별도 Pixel5 에뮬레이터, Android12/API31, 1080×2340 | 서명 release APK 신규 설치 | 일반 앱 흐름·집중 완료 알림·백업·분 표시 위젯·밝고 어두운 스플래시 |
| 별도 Pixel9Pro 에뮬레이터, Android17/API37, 1280×2856, 16KB | 같은 서명 release APK 신규 설치 | 실시간 초 위젯·4×1 요약·백업 복구·테마 적용·스플래시 |

**물리 휴대폰은 연결되지 않았다.** 에뮬레이터 결과를 Samsung/One UI 등 제조사 런처, 실기기 배터리·발열·프레임 성능 또는 Android8/API26 실행 검수로 확대하지 않는다.

## 앱 기능 확인

| 항목 | 수행 및 결과 |
| --- | --- |
| 빈 홈 | 오늘·월·년의 실제 남은 값과 일정/집중 추가 동선 표시 |
| 상세 | 오늘의 초 숫자 감소, 소수점 경과율, 총 초 전환, 눈금 가로 탐색과 ‘미리보기’ 구별 확인 |
| 상세 높이 | 큰 글자·좁은 화면의 공통 시트와 실제 today 시트에서 상한·내부 스크롤 확인. 회전/분할 창 전수는 미검수 |
| 이야기 | 출처·저장 버튼 표시, 저장 후 보관함에서 같은 이야기 확인. 외부 원문 전수 재검증은 미수행 |
| 집중 | 1분 세션 시작→48초에서 일시정지→표시 유지→재개→완료. 홈/다른 화면 이동 중 종료 알림 기록과 Completed 상태 확인 |
| 날짜 일정 | `QA_Release_Date` 생성, 홈에 1일 남음·날짜 눈금 표시, 앱 프로세스 재시작 후 유지 |
| 편집 이탈 | 제목 변경 후 뒤로→버리기 확인창→Discard. 원래 제목 유지 |
| 전체 일정 | 목록↔그리드, 행 펼침, 상세/편집/삭제 등의 동선 노출 확인. 이번 실행에서 모든 정렬·검색 조합은 전수 검수하지 않음 |
| 테마 | 시간 보드·다크 미리보기 후 Cancel 시 A/Clay/시스템 유지. 별도 release에서 Dark 적용 시 설정과 설치 요약 위젯에 반영 |
| 파일 백업 | Android12 시스템 파일 선택기로 실제 JSON 저장. 같은 파일을 복구하면 2개 Unchanged / 변경 0개 |
| 새 설치 복구 | 위 파일을 Android17 새 release 설치에서 선택, Add2 미리보기→복구 2개 완료→날짜 일정의 홈 표시 확인 |
| 권한·개인정보 | 신규 Android17의 알림 꺼짐 안내와 선택형 통계·오류 공유 기본 꺼짐 확인. 수집 동의를 켜거나 외부 이벤트 전송 검증은 하지 않음 |

백업 이동은 로컬 파일 복구 검수이며, 사용자가 제외한 기기 간 동기화 기능을 추가하거나 검증한 것이 아니다.

## 시각 확인

`DesignGalleryActivity`에서 운영 `HomeScreen`, 목록, 편집, 설정, 상세 및 실제 위젯 렌더러를 재사용했다. 고정 예시 데이터와 화면별 강제 테마는 debug에만 존재한다. 갤러리 캡처를 실제 사용자 DB 또는 설치 위젯 검수로 대신하지 않는다.

- 320dp 환경의 A 홈 밝게/어둡게, 긴 한국어 제목·글자2배 홈, 글자2배 목록/설정/시간 입력/상세를 확인했다.
- 시간 보드 기본 폭, 요약 위젯 미리보기, 좁은 위젯 설정·큰 글자를 확인했다.
- 큰 글자에서 숫자·단위·제목의 줄바꿈과 스크롤 배치를 확인했다. 긴 화면의 모든 하단 항목·모든 팔레트·TalkBack 음성 출력은 전수 검수하지 않았다.
- 재사용 구성요소: `TimeLeftTheme`, `Spacing`/`LayoutTokens`, `TimeDetailSheet`, `RemainingTimeText`, 공통 눈금·글로우와 앱/위젯 렌더러. 이번 QA에서 별도 모형 UI나 임의 스타일을 추가하지 않았다.

증거: `04-home-light`~`13-widget-small`, `16-release-theme-preview`, 실제 release `03-release-api31-today-detail`, `14-release-focus-paused`, `32-release-restored`.

## 설치 위젯

- API31: 앱 상세→홈 추가→시스템의 4×2 확인→실제 런처에 배치. 분 단위 숫자, 남음, 눈금 양 끝 시작/종료 시각이 잘리지 않는다.
- API37 release: Today의 실험적 초 옵션 저장, 실제 4×2에서 `10:51:53→10:51:49` 감소 확인. `am kill` 후 앱 PID가 없는 상태에서도 유지됐다. 사용자의 강제 중지와 일반 프로세스 종료는 다르다.
- API37 release: 런처 선택기의 **Period overview 4×1**을 직접 추가해 오늘/월/년 모두 표시 확인. 실제 bounds1162×314px(약387×105dp). 밝게/어둡게 확인. 4×1은 런처 셀 이름이며 모든 런처의 픽셀 크기가 같지 않다.
- 기존 일반 provider를 내용만 Overview로 바꾸면 해당 provider의 최소 크기 때문에 4×2가 유지됐다. 처음부터 4×1은 전용 Period overview 항목으로 추가한다.
- 시험 중 API37 Pixel Launcher에 같은 Today가 두 위치에 나타나는 현상을 관찰했다. AppWidget 서비스에는 해당 provider의 ID3 하나만 존재했고 두 위치가 같은 설정을 공유했다. 런처 재시작 뒤에도 두 위치가 남았다. 앱 결함/호스트 결함으로 단정하지 않으며 제조사 실기기의 pin 추가 재현 항목으로 남긴다. API31에서는 동일 중복을 관찰하지 않았다.
- 시스템 밝기 변경 직후 API31 위젯은 이전 스냅샷을 유지했고 앱 재진입 후 갱신됐다. 앱 내부 테마 적용은 설치 위젯에 반영됐다. 모든 시스템 설정 변경의 즉시 반영을 보장하지 않는다.

증거: `19-release-api31-widget-dark`(파일명과 달리 변경 직후의 라이트 스냅샷), `21-api31-widget-after-reopen`, `26/27-release-live-widget`, `30-release-summary-4x1`, `35-release-widget-final`, AppWidget 서비스 덤프. `18`은 홈 전환 중 촬영되어 위젯 렌더링 통과 근거로 사용하지 않는다.

## 아이콘·스플래시

- 최신 아이콘이 신규 설치의 런처에 표시되며, 밝고 어두운 시작 배경과 최종 심볼·앱 진입을 실제 release에서 확인했다. 시스템 모션 배율0에서도 정상 진입했다.
- ADB 직접 실행(API31)은 아이콘 없는 시작 화면이 나타났으므로 런처 아이콘 탭으로 다시 녹화했다. 실제 사용자 진입과 다른 실행 경로의 결과를 구별한다.
- **바깥 선이 왼쪽부터 그려지고 두 짧은 선이 늘어나는 중간 프레임은 확보하지 못했다.** API31/37 녹화에서는 완성된 심볼과 진입 전환이 주로 보였다. 리소스와 최종 APK에서 AVD 대상·애니메이터·560ms 연결이 보존된 것은 확인했으나 이것만으로 세부 모션을 시각 통과 처리하지 않는다.
- 에뮬레이터 초기화·녹화의 프레임 누락과 시스템 스플래시 시작 시점의 영향을 배제하지 못했다. 임의로 진입 대기를 추가하거나 모션 파라미터를 바꾸지 않았다. 물리 기기의 콜드 런치 고속 녹화로 실제 감김/선 등장과 탭 건너뛰기·빠른 백그라운드 전환을 확인한다.
- 관련 플랫폼 기준: [Android SplashScreen](https://developer.android.com/develop/ui/views/launch/splash-screen), [AndroidX SplashScreen](https://developer.android.com/reference/androidx/core/splashscreen/SplashScreen). 애니메이션 duration 지정 자체는 스플래시 표시 시간을 보장하지 않으며 API31 이전 AVD 지원도 별개다.

실제 녹화: `17-release-splash-launcher.mp4`, `20-release-splash-dark.mp4`, `22-release-splash-motion-off.mp4`, `25-api37-splash-launcher.mp4`, `31-api37-splash-dock.mp4`. 정적 XML 기반 GIF는 이번 실제 실행 증거에 포함하지 않는다.

## 출시 전 남은 확인

| 우선순위 | 항목 | 완료 조건 |
| --- | --- | --- |
| 출시 전 필수 | 공개 데이터 보안·개인정보 안내 대조 | [현재 Play 등록 페이지](https://play.google.com/store/apps/details?id=com.devidea.timeleft)의 ‘No data collected’와 새 선택형 Analytics/Crashlytics 기능을 대조해 Console 신고 및 공개 정책을 일치시킨다. 기본 꺼짐만으로 신고 검토가 끝난 것으로 보지 않음 |
| 출시 전 필수 | 공개 정책과 운영 수집 확인 | `docs/privacy.html`의 최신 내용이 스토어에 연결된 공개 정책에도 반영되는지 확인. 공개 Google Sites 링크는 이번 웹 도구에서 읽지 못했으므로 잘못된 페이지/접속 불가로 단정하지 않음. Firebase 실제 동의/철회·도착 여부는 미확인 |
| 출시 전 권장 수정 | 스토어 대표 배너 | `store-assets/store-listing/feature-graphic-1024x500.jpg`는 보라색·이전 UI의 배너. 현재 A/Clay 디자인과 맞는 이미지로 교체. 새 아이콘512px은 최신 배경이 반영됨 |
| 시각 최종 확인 | 스플래시 모션 | 실제 휴대폰에서 감김·선 등장 중간 과정과 모션 끄기 확인. 현재 세부 모션은 미확인 |
| 기기 확인 | 실제 런처·운영 조건 | One UI 등에서 4×2/4×1, pin 중복 여부, 큰 글자, 시스템 테마 전환, 절전/알림/장기 배터리 확인 |
| 업로드 직전 | Console 출시 후보 | versionCode18 사용 가능 여부, 업로드 인증서, 데이터 보안/권한 선언, 릴리스 노트·스크린샷, 사전 출시 보고서 확인. 콘솔 권한이 없는 로컬 QA로 대신하지 않음 |

추가 미검수: 최소 OS/API26 런타임, 기존 스토어 설치에서 새 release로의 서명 업데이트, 실제 기기 재부팅/Doze/권한 재허용 후 반복 알림, 캘린더 계정·반복 예외 전수, 접근성 음성·실물 햅틱, 장기 이야기 중복 감소. 관련 계산·이행의 단위/계측 통과와 이 실행 조건들을 구별한다.

## 증거·환경 정리

- 로그·PNG·MP4·UI XML·임시 QA 코드·백업은 Git 제외 `design/qa-release-2026-09-27/`에 보관했다. 민감할 수 있는 기존 DB 스냅샷은 로컬 검수용이며 배포/커밋하지 않는다.
- 기존 에뮬레이터 DB를 검수 전 스냅샷과 행 단위로 비교해 **동일** 확인. 일정0개 상태 유지. 원래부터 앱 위젯 등록은 없었으며 Google 검색 위젯ID4만 있던 상태가 유지됐다. 과거 위젯 검수의 Today ID6 기록을 이번 시작 상태로 재사용하지 않는다.
- 기존 에뮬레이터의 원래 A/Clay/시스템, 글자1배,1280×2856을 유지하고 실제 앱 홈으로 복귀했다. 이름이 고정된 계측 전용 DB8종과 보조 파일만 제거했다.
- 새로 만든 Android12/17 임시 AVD 두 개는 종료·삭제했다. 기존 에뮬레이터는 보존했다.
- 마지막 `git diff --check` 통과, 이번 갱신 문서의 로컬 링크 대상 존재 확인. 테스트/캡처가 있다는 이유만으로 미검수 조건을 통과 처리하지 않는다.
