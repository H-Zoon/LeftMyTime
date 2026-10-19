# Firebase 운영 기능 (2026-10-15)

Remote Config, Performance Monitoring, App Distribution의 앱·Gradle 연결을 추가했다. 기존 Firebase 프로젝트와 `google-services.json`을 사용한다. 이번 작업에서 Firebase Console 변경, 앱 빌드·설치, 테스터 초대·업로드는 실행하지 않았다. 출시 버전은 6.6(23)을 유지한다.

## Remote Config

| 키 | Console 자료형 | 앱 기본값 | 적용 범위 |
| --- | --- | --- | --- |
| `widget_seconds_enabled` | Boolean | `true` | 지원 기기에서 제공하는 실험적 위젯 초 표시 |
| `pointhome_entry_enabled` | Boolean | `true` | 홈의 PointHome 플로팅 진입 UI 생성 여부 |

`AppRemoteConfig`가 프로세스 시작 시 로컬 설정을 읽고 이전 실행에서 받아 둔 값을 활성화한다. 새 값은 비동기로 fetch하고 **다음 프로세스 시작 시 활성화**한다. fetch 최소 간격은 12시간이며 실시간 리스너나 `fetchAndActivate()`를 사용하지 않는다. 화면을 열 때마다 네트워크를 기다리지 않는다.

- Console의 **Remote Config → 매개변수**에 위 키를 Boolean으로 추가하고 원하는 값을 게시한다. 아직 게시하지 않았거나 값이 없으면 기존 동작을 유지한다.
- 원격 값이 정확한 `false`일 때만 제품 초 표시 경로를 끈다. 누락·빈값·잘못된 값·SDK 오류는 기본값 `true`로 처리한다. 콘솔에서는 문자열 대신 Boolean을 사용한다.
- `true`여도 기존 Android/호스트 문서 지원 판정과 사용자의 초 표시 선택을 모두 만족해야 한다. 원격 설정이 미지원 기기를 지원으로 바꾸거나 저장된 사용자 선택을 덮어쓰지 않는다. debug 검증용 capability는 유지한다.
- 적용된 `false`에서는 다음 위젯 갱신부터 기존 정적 표시를 사용한다. 이미 런처에 제출한 위젯을 즉시 회수하지 않으며, 앱 프로세스 재시작과 위젯 갱신이 필요할 수 있다. 초기화 중에는 기본/기존 활성 값을 사용한다.
- 12시간 제한, 네트워크 및 다음 실행 적용 전략 때문에 긴급 즉시 차단 용도로 간주하지 않는다. 앱 강제 종료 후 다시 열어야 하는 경우 단순한 Activity 재진입과 구분한다.

최종 검증 시 테스트 설치에서 `true → false → true`를 게시하고 fetch 간격을 고려해 각 다음 프로세스와 위젯 갱신을 확인한다. 오프라인·키 삭제·잘못된 값·초 미지원 기기·사용자 선택 꺼짐도 포함한다. 운영 코드의 fetch 간격을 0으로 낮추지 않는다.

### PointHome 진입 UI (2026-10-19)

Firebase Console의 **Remote Config → 매개변수**에 `pointhome_entry_enabled`를 **Boolean**으로 추가한다. `true`이면 기존 조건(자격값·SDK 초기화 성공)을 만족할 때 기본 플로팅 버튼 ‘해택 받기’를 생성하고, `false`이면 진입 UI 생성 호출을 생략한다. 누락·빈값·잘못된 값·설정 초기화 오류는 기본값 `true`로 처리해 기존 표시 동작을 유지한다.

앱은 시작 시 저장된 값의 활성화가 끝날 때까지 버튼 호출만 비동기로 기다린다. 홈 화면과 SDK 초기화는 그대로 진행하며, 설정 준비 전 기본값으로 버튼을 먼저 표시하지 않는다. 확정한 진입 설정은 같은 프로세스 동안 고정하고 Activity 재생성에도 같은 값을 사용한다. Activity가 파괴되면 대기 중인 UI 호출도 취소한다.

새 설정은 기존 12시간 최소 간격으로 fetch하고 **다음 프로세스 시작에 적용**한다. 단순 홈 복귀나 Activity 재생성만으로 새 원격 값이 반영되는 것은 아니며, 콘솔 게시 뒤 기기가 fetch를 마친 다음 프로세스 재시작이 필요하다. 첫 설치에서 아직 원격 값을 받은 적이 없으면 기본값 `true`를 사용한다. 실시간 리스너·추가 fetch·SDK 초기화/중단 제어·리워드 알림 동의/토픽 변경은 추가하지 않았다.

검증 대기: 기본/누락/정상 false·true/잘못된 값, 이전 true→새 false와 이전 false→새 true의 다음 프로세스 적용, 설정 읽기가 늦은 경우 false 버튼 미노출, 대기 중 Activity 파괴·재생성, 오프라인 캐시와 자격값 누락, 기존 위젯 설정 보존. 이번에는 코드·XML·호출 순서와 공백만 정적으로 검토하며 빌드·테스트·콘솔 게시·기기 검수는 실행하지 않는다.

## Performance Monitoring

`firebase-perf` SDK와 Gradle 플러그인(2.0.2)을 적용했다. SDK 자동 계측과 함께 다음 두 사용자 정의 trace를 기록한다.

| trace | 시작 → 종료 | 한계 |
| --- | --- | --- |
| `home_data_load` | 홈 데이터 구독·재시도 → 첫 로딩 완료/오류 상태 계산 | Compose 첫 화면 그리기 시간은 아님 |
| `widget_update_submit` | 위젯 갱신의 조회·계산 시작 → `updateAppWidget` 반환 | mutex 대기·이후 경계 예약·런처 실제 그리기는 제외 |

고정 속성 `outcome`은 `success`, `fallback`, `error`, `cancelled` 중 하나다. 홈의 잘못된 레코드 대체나 초 렌더링 실패 후 정적 표시 성공은 `fallback`으로 구분한다. 매초 ticker에 새 trace를 만들지 않으며 예외·취소에서도 trace를 닫는다. 계측 SDK 오류는 제품 동작을 막지 않는다.

일정 제목·내용·정확한 날짜·일정/위젯 ID·사용자 ID를 사용자 정의 trace에 넣지 않는다. 자동 성능 수집은 활성화되어 있으며 SDK가 지원하는 시작·렌더링·HTTP/S 요청 정보를 처리한다. 앱 사용자 정보 안내와 bundled 개인정보처리방침에 이를 반영했다. Compose 세부 화면별 성능이 이 계측만으로 모두 구분되는 것은 아니다.

최종 빌드·설치 후 **Firebase Console → Performance**에서 해당 앱을 열고 앱 시작·홈 진입·위젯 갱신 뒤 두 trace와 `outcome`을 확인한다. 전송·샘플링·콘솔 반영은 즉시 완료되지 않을 수 있다. 서버 도착과 실측 기준선은 아직 검증하지 않았다.

## App Distribution

Gradle 플러그인 5.3.0을 적용하고 `debug`·`release`에 APK 배포 task를 구성했다. 앱 안에 별도 배포 SDK나 업데이트 팝업을 넣지 않는다. 일반 `assemble`에는 업로드를 연결하지 않았다. Firebase 앱 ID는 Google Services 플러그인에서 가져온다.

Firebase Console의 **App Distribution**에서 현재 Android 앱을 시작하고 테스터 그룹의 alias를 정한다. 업로드 환경은 해당 프로젝트 권한이 있는 Firebase CLI 로그인 또는 외부 자격 증명으로 인증한다. 서비스 계정 JSON을 쓸 경우 저장소 밖에 두고 `GOOGLE_APPLICATION_CREDENTIALS`에 절대 경로를 지정한다. 인증 파일은 APK 리소스에 포함하지 않는다.

| Gradle 속성 (`-P`) | 환경변수 | 값 |
| --- | --- | --- |
| `firebaseDistributionGroups` | `FIREBASE_APP_DISTRIBUTION_GROUPS` | 그룹 alias, 쉼표 구분 |
| `firebaseDistributionTesters` | `FIREBASE_APP_DISTRIBUTION_TESTERS` | 테스터 이메일, 쉼표 구분 |
| `firebaseDistributionReleaseNotesFile` | `FIREBASE_APP_DISTRIBUTION_RELEASE_NOTES_FILE` | 릴리즈 노트 파일 경로, 상대 경로는 저장소 루트 기준 |

Gradle 속성이 같은 환경변수보다 우선한다. 그룹 또는 테스터와 해당 빌드의 릴리즈 노트를 준비한다. 이메일이나 비밀키를 저장소의 Gradle 파일·속성 파일에 커밋하지 않는다.

아래는 **실제 업로드와 테스터 전달을 수행하는 명령**이다. 수신 대상과 배포 요청이 확정된 환경에서 실행한다. 이번 구현에서는 실행하지 않았다.

```bash
# 저장소 루트에서 실행. 값은 실제 승인된 대상과 로컬 파일로 지정한다.
export FIREBASE_APP_DISTRIBUTION_GROUPS='your-test-group-alias'
export FIREBASE_APP_DISTRIBUTION_RELEASE_NOTES_FILE='/absolute/path/to/release-notes.txt'

# 테스트용 debug APK
./gradlew :app:assembleDebug :app:appDistributionUploadDebug

# 기존 release 서명 설정(keystore.properties)이 있는 환경의 release APK
./gradlew :app:assembleRelease :app:appDistributionUploadRelease
```

기존 앱을 데이터 유지 업데이트하려면 동일 applicationId와 서명이 필요하다. 기존 release 설치를 갱신하는 테스터에게는 동일 release 키로 서명한 APK를 사용한다. debug 키가 다른 설치와 호환된다고 가정하지 않는다. App Distribution은 Play 출시와 별개이며 이 설정은 Play 업로드를 수행하지 않는다.

## 검증과 출시 인계

- 정적 확인: 공식 SDK·플러그인 API, Kotlin 호출부, XML/한·영 문자열, 변경 공백 및 기존 FCM 작업 보존을 검토했다.
- Remote Config 해석·지원 경계 회귀 테스트 소스를 추가했으며 실행하지 않았다.
- 프로젝트 지침에 따라 **빌드·Lint·단위/계측 테스트·기기 설치·시각 검수는 미실행**이다. 사용자 최종 검증 요청 시 기본 `./gradlew testDebugUnitTest assembleDebug lintDebug`와 위 기능별 실행 검증을 진행한다.
- 빌드 후 Google Services·Perf 바이트코드 계측·release R8 조합, Remote Config 다음 프로세스 적용, Performance 서버 수신, 서명 APK 테스터 설치가 남아 있다.
- 공개 개인정보처리방침·스토어 데이터 보안은 기존 출시 인계대로 사용자가 갱신한다. 이번에는 저장소 내 방침과 안내만 수정했다. Console 게시·인증/권한 생성·테스터 초대·업로드는 미실행이다.

## 공식 참고

- [Remote Config 로딩 전략](https://firebase.google.com/docs/remote-config/loading)
- [Remote Config Android API](https://firebase.google.com/docs/reference/android/com/google/firebase/remoteconfig/FirebaseRemoteConfig)
- [Performance Monitoring Android 설정](https://firebase.google.com/docs/perf-mon/get-started-android)
- [App Distribution Gradle 설정·인증·업로드](https://firebase.google.com/docs/app-distribution/android/distribute-gradle)
- [Firebase 개인정보 처리 항목·보관](https://firebase.google.com/support/privacy)
