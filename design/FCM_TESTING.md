# FID 기반 FCM 등록·수신 테스트

구현일: 2026-10-15. 기준: `develop`의 `cd35af4` 이후 변경.

## 구현과 범위

- 기존 Firebase 프로젝트 `timeleft-f83ba`, Android 패키지 `com.devidea.timeleft`를 사용한다.
- Firebase BoM을 34.13.0 → **34.16.0**으로 변경하고 `firebase-messaging`을 추가했다. 이 조합의 Messaging 25.1.1은 FID 등록 API와 FID 변경 시 재등록 수정을 포함한다. 기존 Analytics 버전은 유지되고 Crashlytics·Common·Installations는 새 BoM의 호환 조합을 따른다.
- Manifest의 `firebase_messaging_installation_id_enabled=true`와 `FirebaseMessaging.register()`를 사용한다. 등록 성공은 `FirebaseMessagingService.onRegistered(installationId)`로 확인한다. `FirebaseInstallations.getId()`만 조회한 결과를 등록 성공으로 간주하지 않는다.
- SDK 기본 자동 초기화와 재등록 동작을 사용하며, 앱 프로세스 시작 및 디버그 화면의 ‘다시 등록’에서도 수동 등록을 요청한다. 실패는 앱을 종료시키지 않고 진단 상태와 `TimeLeftFCM` Logcat에 남긴다.
- 포그라운드의 notification 메시지와 서비스에 전달되는 data 메시지는 전용 `remote_messages` 채널에 표시한다. notification 제목/본문 → data의 `title`/`body` → 한/영 기본 문구 순서로 표시한다. 일정·집중 알림과 채널·알림 태그를 분리하며, 알림을 누르면 기존 홈을 연다.
- 백그라운드의 notification 메시지는 SDK가 표시한다. 이 경우 `onMessageReceived()`가 실행되지 않아 진단 화면의 서비스 수신 시각은 갱신되지 않을 수 있다. data 메시지의 배송 시점은 Android·FCM의 백그라운드 정책을 따른다.
- FID는 debug 빌드에서만 로그로 출력한다. 메시지 본문·data 값/키·sender를 로그나 Analytics에 추가하지 않는다. 진단 화면의 FID·수신 메타데이터는 프로세스 메모리에서만 유지하며 Firebase SDK 자체 등록 저장소는 SDK가 관리한다. 별도 앱 서버 저장이나 토픽 구독은 추가하지 않았다.
- 앱에 포함되는 `docs/privacy.html`에 FCM 설치·등록 정보 처리 및 메시지 전달 목적을 반영했다. 공개 웹사이트나 스토어 설정을 게시/수정한 것은 아니다.

## 테스트 화면 열기

빌드·설치 후 연결된 테스트 기기에서 실행한다. 기기가 여러 개면 각 `adb` 명령에 `-s SERIAL`을 추가한다.

```sh
adb shell am start -n com.devidea.timeleft/.messaging.FcmDiagnosticsActivity
adb logcat -s TimeLeftFCM
```

이 Activity와 진단 문구는 `src/debug`에만 있어 release APK에 포함되지 않는다. 기존 `TimeLeftTheme`, `TimeLeftTopAppBar`, `TimeLeftInlineButton`, `Spacing`, `LayoutTokens`를 재사용한다.

1. Google Play services가 동작하는 기기/에뮬레이터에서 앱을 실행한다.
2. 등록 완료 및 등록 시각을 확인하고 FID를 복사한다. 대기·실패 시 네트워크를 확인하고 ‘다시 등록’을 누른다.
3. Android 13 이상에서는 ‘알림 권한 요청’을 허용한다. 기기 앱 알림 및 ‘메시지’ 채널도 활성화한다. 권한 허용 여부와 FCM 등록 성공은 별개다.
4. 같은 Firebase 프로젝트의 권한 있는 개발 환경에서 아래 메시지를 발송한다. 발송 인증 정보는 개발 PC/서버에서 사용한다.

## FID로 시험 발송

HTTP v1의 `message.fid`를 사용한다. 아래는 실행 전 `FID_FROM_DEBUG_SCREEN`을 바꾸고, 메시지 전송 권한이 있는 환경에서 유효한 OAuth 액세스 토큰을 준비하는 예시다. 앱에 발송 인증 정보를 넣지 않는다. Firebase 콘솔의 기존 토큰 입력 화면이 FID를 지원하는지는 별도로 확인해야 한다.

`message.json`:

```json
{
  "message": {
    "fid": "FID_FROM_DEBUG_SCREEN",
    "notification": {
      "title": "FCM 테스트",
      "body": "FID 등록 및 수신 확인"
    },
    "android": {
      "notification": {
        "channel_id": "remote_messages"
      }
    }
  }
}
```

```sh
curl --fail-with-body \
  --request POST \
  --header "Authorization: Bearer $FCM_ACCESS_TOKEN" \
  --header "Content-Type: application/json; charset=UTF-8" \
  --data-binary @message.json \
  https://fcm.googleapis.com/v1/projects/timeleft-f83ba/messages:send
```

백그라운드에서도 서비스 콜백을 확인하려면 `notification`을 제거하고 다음처럼 `data`를 넣는다. 기존 data와 notification을 함께 넣으면 백그라운드에서는 SDK의 notification 처리 경로가 적용된다.

```json
{
  "message": {
    "fid": "FID_FROM_DEBUG_SCREEN",
    "data": {
      "title": "FCM 데이터 테스트",
      "body": "서비스 수신 콜백 확인"
    },
    "android": {
      "priority": "HIGH"
    }
  }
}
```

HTTP 성공 응답은 발송 접수 결과다. 실제 수신은 기기 알림과 서비스 수신 시각/Logcat으로 별도 확인한다. 강제 종료 상태는 앱을 다시 연 후 시험한다.

## 현재 확인 범위와 검증 대기

확인: Messaging 25.1.1 실제 AAR/소스의 `register(): Task<Void>`·`onRegistered(String)` API, BoM 매핑, 코드 리뷰, XML·리소스 참조와 공백 정적 확인. **빌드·Lint·단위/계측 테스트·설치·실제 FID 발급·서버 발송·기기 수신·시각 검수는 실행하지 않았다.** 기존 설치/배포 APK는 이번 소스 변경을 포함한다고 간주하지 않는다.

사용자가 최종 검증을 요청하면 프로젝트 기본 `./gradlew testDebugUnitTest assembleDebug lintDebug` 및 다음 항목을 확인한다.

- 최초 실행·오프라인 실패 후 재시도·동일 FID 재등록·프로세스 재시작·앱 데이터 초기화 후 FID 변경/재등록.
- 포그라운드/백그라운드에서 notification, data-only, 혼합 메시지, 제목/본문 없음, 반복 메시지.
- Android 13 이상 알림 권한 거절/허용, 전체 알림/메시지 채널 차단 및 설정 복귀. 차단 중 등록/수신과 표시 결과 구분.
- 기존 일정·집중 알림 채널과 알림 탭 동선 유지.
- 디버그 화면의 한/영, 밝게/어둡게, 320dp, 큰 글자, 긴 FID/오류 줄바꿈·복사·스크롤·48dp 터치 영역.
- release에서 디버그 Activity/문구 및 FID 로그 제외. BoM 변경에 따른 기존 Analytics·Crashlytics 초기화.

## 공식 근거

- [Android FCM 초기화·FID 등록](https://firebase.google.com/docs/cloud-messaging/android/get-started)
- [Messaging 25.1.1 릴리스 노트](https://firebase.google.com/support/release-notes/android#messaging_v25-1-1)
- [FirebaseMessaging.register](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessaging#register())
- [Android 메시지 수신 동작](https://firebase.google.com/docs/cloud-messaging/android/receive-messages)
- [HTTP v1 Message: fid](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages)
