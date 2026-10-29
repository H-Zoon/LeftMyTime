# 6.7 (24) 서명 배포 빌드 — 2026-10-29

사용자 요청으로 현재 작업 트리의 수정사항을 반영한 서명 AAB/APK를 생성했다. 버전은 기존 6.6(23)에서 **6.7(24)**로 올렸다. 기준 브랜치는 `feature/reward-notifications`, HEAD는 `f2b3bfc`이며 아직 커밋하지 않은 앱 변경을 포함한다. 이번 작업에서 커밋·Git push·Play Console 업로드·심사 제출·실제 출시는 하지 않았다.

## 포함 범위

- 집중 바로 시작→준비 화면의 시간 다이얼→시작 동선, 집중 기록의 내부 진입.
- 성장·자기계발 이야기 14개, 기존에 저장한 이전 이야기 보존, 한/영 문구와 출처.
- ‘저장한 일정으로 추가’ 명칭과 저장/적용/빈 상태/삭제/백업 안내.
- ‘문장으로 추가’ 메뉴·Activity·파서·편집기 연결·한/영 문구·전용 테스트 소스 제거. 기존 사용자 일정은 보존한다.
- 현재 작업 트리에 있던 PointHome SDK/플로팅·리워드 알림·진입 계측, 개인정보처리방침 HTTPS 연결과 기타 이전 수정사항도 포함한다. 기존 수정사항을 따로 되돌리지 않았다.
- DB 스키마·기존 사용자 데이터·설정 키를 이번 서명 작업에서 변경하지 않았다. 버전과 키 폴더의 Git 제외 규칙만 빌드 준비를 위해 갱신했다.

## 서명 키와 Console 확인

**최종 산출물은 사용자가 다시 지정한 `key/keystore.properties`의 `key/androidkey.keystore`로 서명했다.** 같은 폴더의 다른 JKS를 임의로 선택하지 않았다. 키 저장소 열기와 개인 키 비밀번호를 확인하고, 프로젝트 루트의 Git 제외 `keystore.properties`를 이 설정으로 갱신했다. 비밀번호는 출력하거나 문서에 기록하지 않았다. 설정 파일 권한은0600, `/key/`와 `/keyStore/`는 모두 Git 제외다.

**최종 AAB/APK의 인증서 SHA-256**:

`65da43af6724aef8c7faae98adfd93be415eb6f26bc5b0a9b630227148c37456`

이는 과거6.6 빌드 기록의 인증서와 일치한다. 실제 Play Console에 등록된 업로드 인증서와 versionCode24 사용 여부를 이번 작업에서 직접 조회한 것은 아니다.

처음 지정된 `keyStore/upload-keystore.jks`로 만든 후보는 다른 인증서 `b76fb7de88e68d3d20068d7b73644513f5b907ffb6fe4d9b627748e764a08dac`를 사용했다. 이 후보와 당시 검증 로그는 Git 제외 `design/qa-release-2026-10-29/previous-key-build/`에 구분해 보존했고 전달 경로는 최종 기존키 산출물로 교체했다. 아래 파일 해시와 최종 검증은 재지정한 `key/androidkey.keystore` 기준이다.

## 빌드·자동 검사

JBR21과 기존 Gradle 캐시를 사용했다. 첫 제한 환경 실행은 캐시 잠금 접근으로 시작하지 못해 허용된 확장 실행으로 진행했다. 첫 본 실행은 단위 테스트 후 기본 JVM 힙 2GB의 GC thrashing으로 중단됐다. 앱 코드를 바꾸지 않고 실행 옵션의 JVM 힙을 4GB로 늘리고 worker 수를 2로 제한해 다시 실행했다.

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew '-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8' \
  --max-workers=2 \
  testDebugUnitTest assembleDebug lintDebug \
  testReleaseUnitTest assembleRelease bundleRelease lintRelease \
  -x uploadCrashlyticsMappingFileRelease --console=plain
```

**BUILD SUCCESSFUL**, 2분43초, 138 tasks(26 executed, 112 up-to-date). 첫 실행에서 끝난 단위 테스트/컴파일 등은 최종 실행에서 up-to-date로 재사용했다. 전역 Gradle 메모리 설정은 변경하지 않았다.

기존 키를 재지정한 후에는 같은4GB/worker2 옵션으로 `assembleRelease bundleRelease -x uploadCrashlyticsMappingFileRelease --console=plain`을 다시 실행했다. **BUILD SUCCESSFUL, 2분5초**. 앱 코드·버전은 바꾸지 않았으며 아래 단위/Lint 결과는 앞선 동일 앱 코드의 검증 결과를 유지한다. 서명·AAB 구조/manifest·ZIP/ELF 정렬과 매핑 일치는 새 산출물에서 다시 검사했다. 이전 후보와 AAB 내부 내용을 비교한 결과 DEX·assets·네이티브 라이브러리·manifest·매핑은 동일하며, 서명 파일 이외에는 R8 compilation 메타데이터와 재생성된 `com.google.firebase.crashlytics.mapping_file_id` 리소스만 달랐다.


| 항목 | 결과 |
| --- | --- |
| Debug 단위 테스트 | 27 suites / 151개, 실패·오류·제외 0 |
| Release 단위 테스트 | 동일한 151개를 release 구성에서 실행, 실패·오류·제외 0 |
| Debug Lint | 오류0 / 경고169 / 힌트10 |
| Release Lint | 오류0 / 경고166 / 힌트7 |
| 기존 Debug Lint | 경고172 / 힌트7. 변경 전 보고서를 보존했다 |
| Debug Lint 비교 | ID·메시지·위치 파일 기준 추가14 / 제거14. 행 번호 이동은 제외 |

새로 보이는 항목은 사용되지 않는 `focus_minutes/focus_custom/focus_duration_minutes/focus_duration_limit` 4개, `UseKtx` 2개와 원시형 Compose 상태 힌트3개 등이다. 라이브러리 최신 버전 알림의 메시지 변경과 Cauly SDK 3.5.41→3.5.46에 따른 위치 변화도 비교 수에 포함돼 실제 신규 결함 수와 같지 않다. SDK의 기존 `TrustAllX509TrustManager` 경고는 같은 내용으로 계속 관찰됐다. 경고를 모두 해결했다고 쓰지 않는다.

최종 빌드 로그에는 광고 SDK에서 참조하는 Billing/Pangle 클래스 누락 경고 22종과 일부 네이티브 심볼 strip 불가 안내가 있다. R8·패키징은 성공했으나 해당 광고 경로의 실기기 실행을 검증한 것은 아니다. AAB 서명 검사에는 자체 서명/신뢰 체인·타임스탬프·POSIX 속성 경고가 남으며 서명 실패와 구별한다.

## 산출물과 검증

| 산출물 | 바이트 | SHA-256 |
| --- | ---: | --- |
| `app/build/outputs/release-6.7-24/TimeLeft-6.7-24.aab` | 30,987,491 | `fbeccd3ee98ac6f31cb7fa4df093665aeb270ee4e82614a79ef42282dd89ba03` |
| `app/build/outputs/release-6.7-24/TimeLeft-6.7-24.apk` | 28,736,464 | `4ebcceda8c73b431248a3f3e26cc47c94396edb6bc232e3abb3294666e2ca1eb` |
| `app/build/outputs/release-6.7-24/mapping.txt` | 74,062,031 | `e0a51ef0dc10e71f5b626f27b4c90ccb8b0766f05903c0ec45e54cfb0482da2b` |

위 전달용 사본은 원래 `app/build/outputs/bundle/release/app-release.aab`, `app/build/outputs/apk/release/app-release.apk`, `app/build/outputs/mapping/release/mapping.txt`와 해시가 일치한다. `app/build/outputs/release-6.7-24/SHA256SUMS.txt`에 함께 기록했다. AAB/APK·매핑·로그·키는 Git에 포함하지 않는다.

- Bundletool1.18.3 `validate` 통과. AAB manifest와 APK metadata의 앱 ID `com.devidea.timeleft`, 버전6.7/24 확인.
- AAB manifest의 debug 비활성, debug 갤러리/probe와 제거한 문장 입력 Activity 부재 확인.
- APK `apksigner verify` V2 통과, AAB `jarsigner -verify`의 `jar verified` 확인. 두 파일의 인증서가 제공된 키의 위 SHA-256과 일치한다.
- APK `zipalign -c -P 16 -v 4` 통과. AAB 설정 `PAGE_ALIGNMENT_16K` 확인.
- 최종 APK/AAB에 포함된 네이티브 라이브러리30개(arm64-v8a11 / armeabi-v7a11 / x86 4 / x86_64 4)의 모든 ELF LOAD 정렬이 16,384바이트 이상이다. APK/AAB 라이브러리 바이트도 일치한다. 실제 16KB 기기 실행 검사와 구별한다.
- AAB에 포함된 `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`과 별도 매핑 파일의 해시 일치.
- 개인정보처리방침 문장 입력 삭제는 로컬 원본/게시용 파일에 반영된 상태다. 앱이 읽는 공개 방침의 새 웹 배포는 이번 작업에 포함하지 않았다.

## 미실행 범위

설치/업데이트·실화면·계측 테스트·실기기 광고/리워드·알림 수신·SDK/Analytics 서버 전송·Play 분할 APK 실행은 확인하지 않았다. 이전 기록의 위젯 확대 안내 접근성 및 제조사 런처/장시간 갱신·전력 문제도 이번 서명 빌드로 해결되거나 검증된 것으로 쓰지 않는다. Crashlytics 매핑 직접 업로드와 Firebase App Distribution 전송, Hosting 배포, Play Console 변경은 하지 않았다.

자동 검사와 산출물 검증 로그는 Git 제외 `design/qa-release-2026-10-29/`에 보존했다. `release-summary.json`, `lint-debug-comparison.json`, `packaged-native-verification.json`, `gradle-build.log`, 최종 `gradle-resign.log`, `resign-payload-comparison.json`·`resign-resource-comparison.json`과 메모리 부족 첫 실행 로그를 함께 따른다.

## 한국어 출시 노트

Play Console에 사용할 원문은 `store-assets/release-notes/6.7-ko-KR.txt`, 등록정보 JSON의 `releaseNotes`에도 같은 본문을 반영했다. 앱 빌드 산출물은 변경하지 않았으며 스토어에 게시하지 않았다.

• 집중 시간을 다이얼로 정하고 시작할 수 있도록 화면을 정리했습니다.
• ‘이만큼의 시간’을 작은 시작과 꾸준한 성장을 응원하는 이야기로 바꿨습니다.
• ‘일정 입력 틀’의 이름을 ‘저장한 일정으로 추가’로 바꿨습니다.
• ‘문장으로 추가’ 기능을 제거했습니다.
• 홈의 ‘혜택 받기’와 혜택 알림 수신 설정을 추가했습니다.
• 위젯 초 표시의 호환성을 개선했습니다.
