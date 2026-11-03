# 6.8 (25) 최종 검증·서명 빌드 — 2026-10-30

사용자의 ‘최종 검증·서명 빌드 진행’ 요청으로 리워드 광고 동의·직접 수신거부 개선을 포함한 **6.8(25)** AAB/APK를 생성했다. 기준 브랜치는 `feature/reward-notifications`, HEAD는 `87a380a`이며 미커밋 변경을 포함한다. 커밋·push·Play 업로드/심사 제출·Firebase 캠페인 게시·Hosting 배포는 수행하지 않았다.

## 포함 범위와 서명

인트로·설정에 선택적 광고 수신 동의를 명시하고, 알림 본문에서 로그인 없이 리워드 수신설정을 직접 연다. 앱이 게시하는 알림에는 수신거부 액션과 한국시간 야간 표시 검사를 추가했다. 기존 토픽·수신 선택·인트로 시점은 유지하며 새 명시적 선택에만 기기 내 시각/고지 버전을 기록한다. `SettingsScaffold`, `TimeLeftTopAppBar`, 기존 스위치·상태·재시도·시스템 설정 행과 디자인 토큰을 재사용한다. 상세한 동작과 Console 자동 표시의 한계는 [변경 기록](REWARD_ADS_2026-10-30.md)을 따른다.

기존 `key/androidkey.keystore`와 Git 제외 `keystore.properties`로 서명했다. 비밀번호는 로그/문서에 기록하지 않았다. 최종 AAB/APK 인증서 SHA-256은 모두 아래 값으로 이전 6.6/6.7과 일치한다. Play Console의 등록 인증서와 versionCode25 사용 여부는 직접 조회하지 않았다.

`65da43af6724aef8c7faae98adfd93be415eb6f26bc5b0a9b630227148c37456`

`key/`, `keyStore/`, `keystore.properties`와 빌드 산출물은 Git 제외이며 키 파일은 추적되지 않는다.

## 빌드와 자동 검사

JBR21·기존 Gradle 캐시, 실행별 JVM 힙4GB/worker2를 사용했다. 전역 메모리 설정은 바꾸지 않았다.

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew '-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8' \
  --max-workers=2 \
  testDebugUnitTest assembleDebug lintDebug \
  testReleaseUnitTest assembleRelease bundleRelease lintRelease \
  assembleDebugAndroidTest \
  -x uploadCrashlyticsMappingFileRelease --console=plain
```

첫 실행에서 `SettingsActivity`의 중복 companion object 컴파일 오류를 발견해 기존 시간 포매터를 하나의 companion으로 합친 뒤 전체 명령을 재실행했다. **BUILD SUCCESSFUL, 4분58초, 170 tasks(94 executed, 76 up-to-date).**

| 항목 | 결과 |
| --- | --- |
| Debug 단위 테스트 | 27 suites / 153개, 실패·오류·제외0 |
| Release 단위 테스트 | 27 suites / 153개, 실패·오류·제외0 |
| Debug Lint | 오류0 / 경고169 / 힌트10 |
| Release Lint | 오류0 / 경고166 / 힌트7 |
| 리워드 계측 테스트 | Android17/API37 에뮬레이터에서 3개 통과 |

Lint 개수는 이전 검사와 같다. ID·메시지·위치 파일 비교에서 각 구성의 추가1/제거1은 `SharedPreferences.edit`의 기존 `UseKtx` 제안이 `RewardMessaging.kt`에서 새 `RewardAdConsent.kt`로 이동한 것이다. 기존 광고 SDK 보안/사용 API 등의 경고가 모두 해결됐다는 의미는 아니다.

R8의 기존 Billing/Pangle 클래스 누락 경고22종과 계측 APK의 Jetifier 혼합 AndroidX/옛 support 참조 경고가 남는다. 빌드·축소·패키징은 성공했지만 모든 광고 네트워크 경로의 실행을 확인한 것은 아니다. Crashlytics 매핑 직접 업로드는 제외했다.

## 최종 산출물 검증

| 파일 (`app/build/outputs/release-6.8-25/`) | 바이트 | SHA-256 |
| --- | ---: | --- |
| `TimeLeft-6.8-25.aab` | 30,963,914 | `90c406b083667c720c93ccafafb55b4d99accce09304eb969d2d9a22072df796` |
| `TimeLeft-6.8-25.apk` | 28,709,148 | `be01676e4626e75cd7b88ae2b5ebedcaa542925686fba7f2a59f707265215a2c` |
| `mapping.txt` | 74,122,444 | `c5d0eead7fd070b2be6065d7caf4b5eb0ab3b5e2d980f0b9dbfc8a2797251e7e` |

전달용 사본과 Gradle 원본의 해시가 일치하며 같은 폴더의 `SHA256SUMS.txt`에 기록했다. 이전 6.7 전달 폴더는 보존했다.

- Bundletool1.18.3 `validate` 통과. 앱 ID `com.devidea.timeleft`, 버전6.8/25, debug 비활성 확인.
- release manifest에 검수용 갤러리/제거한 문장 입력 Activity가 없고 `SettingsActivity`는 exported=false다.
- APK `apksigner verify` V2, AAB `jarsigner -verify` 통과. 자체 서명/신뢰 체인·타임스탬프 관련 안내는 서명 실패와 구별한다.
- APK `zipalign -c -P 16 -v 4` 통과, AAB `PAGE_ALIGNMENT_16K` 확인.
- 포함 네이티브 라이브러리30개 모두 ELF LOAD 정렬이16,384바이트 이상이며 APK/AAB 라이브러리 바이트가 일치한다.
- AAB 내 `proguard.map`과 별도 `mapping.txt` 해시 일치. 서명 설정/비밀번호 파일이 패키지에 포함되지 않았음을 확인했다.

## 실행 검증과 한계

기존 Pixel_10_Pro_XL AVD를 `-read-only -no-snapshot -no-window`로 실행했다. Android17/API37·arm64, 실제 페이지 크기16,384바이트이며 원래 AVD 사용자 데이터는 저장하지 않는다. 연결된 실기기는 변경하지 않았다.

`RewardAdConsentTest` 3개는 과거 수신 선택 보존/고지 기록 비소급, 새 동의·철회 시각/버전 저장과 다른 설정 보존, Console 목적지 1회 소비 및 직접 수신거부 Intent의 구별을 확인했다. 고유한 테스트 전용 설정 파일을 사용하며 실제 토픽 알림은 발송하지 않았다.

실제 `RewardNotificationSettingsScreen`을 재사용하는 debug 갤러리에서 320dp·밝게/어둡게·한/영·글자1.5배, 수신 켜짐/꺼짐·시스템 차단·동기화 실패·설정 불러오기 상태를 확인했다. 긴 영문도 세로 스크롤로 하단 시스템 설정·리워드 보기까지 접근했고, 상단 수신거부 탭 후 꺼짐 상태가 표시됐다. 인트로 갤러리에 남은 예전 임시 고지를 실제 문자열로 교체하고 누락 import를 보완한 뒤 `testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest`를 다시 통과했다(48초, 95 tasks). 이 수정은 debug 소스에만 해당하므로 서명 release 파일은 변하지 않는다. 수정된 실제 인트로의 한국어 기본 글자와 영어1.5배 글자에서도 고지·동의/거부 버튼을 확인했다.

동일한16KB 에뮬레이터에 **전달용 최종 서명 APK**를 새로 설치했다. 앱을 종료한 상태에서 `MainActivity`에 Console과 같은 `notification_kind=reward`, `open_target=reward_settings`를 전달했을 때 로그인·PointHome 화면 없이 `SettingsActivity`의 전용 수신설정이 열렸다(`LaunchState:COLD`, TotalTime1,972ms). 뒤로 가면 MainActivity로 복귀하고 소비된 설정 진입을 반복하지 않았다. 이 결과는 실제 FCM 메시지를 발송하거나 시스템 알림을 눌러 확인한 결과와 구별한다. 확인 후 화면 크기 override를 복원하고 읽기 전용 에뮬레이터를 종료했다.

실제 FCM의 포그라운드/백그라운드/종료 상태 수신, 직접 액션 PendingIntent의 시스템 알림 실행, 토픽 서버 반영·오프라인·디스크 실패·빠른 연속 전환, 6.7→6.8 실제 업데이트, TalkBack·제조사 실기기·모든 광고 SDK/리워드 지급은 미검증이다. 단위 경계 테스트가 실제 서버 발송 시간을 검증하지는 않는다.

Firebase 캠페인3186944161412585896은 월수금토일14시 한국시간·하루1회·TTL1시간·`open_target=reward_settings`의 **초안**으로 유지한다. 새 앱 배포/실제 FCM 검증 후 활성화가 필요하다. 공개 개인정보처리방침 배포, Play 업로드·심사 제출과 Crashlytics 매핑 직접 업로드도 수행하지 않았다.

전체 로그·서명/ELF 검증·스크린샷·계측 결과는 Git 제외 `design/qa-release-2026-10-30/`에 보존한다. 이번 한국어 출시 노트는 `store-assets/release-notes/6.8-ko-KR.txt`다.
