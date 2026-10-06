# 6.6 (23) 서명 배포 빌드

2026-10-06. 사용자 요청에 따라 위젯 수정사항을 커밋하고, 버전을 **6.5(22) → 6.6(23)**으로 올려 배포용 **AAB**와 검증용 APK를 생성했다. 사용자가 AAR 대신 AAB임을 확인했다.

## 포함 변경

- 기능 기준 커밋: `5f3a056` (`fix(widget): recover stale updates and extend seconds support`). 이번 빌드에는 이 커밋과 버전 범프가 포함된다.
- 위젯 갱신 실패 시 기존 경계 알람·렌더러와 WorkManager의 고유 주기 작업으로 복구를 시도한다. 일부 위젯 실패를 격리하고 다음 경계를 다시 예약한다. 15분 정확 갱신을 보장하는 것은 아니다.
- 초 단위 실험 기능을 지원하는 호스트 문서 6·8까지 확대했다. 조건은 `API >=35`와 문서 `6`, `8`, `>=9`이며 기본값은 꺼짐이다. Android 16의 검증된 런처에서 동작하며 Android 15 시험 환경에서는 정적 표시를 유지한다.
- 기존 `WidgetSecondsPlan`, 숫자·눈금·상태 계산, `WidgetPalette`, `WidgetTextMeasure`, `TimeRulerTokens`를 재사용한다. DB·기존 일정·설정 키는 변경하지 않았다.

## 빌드와 산출물

JBR 21을 사용했다.

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew assembleRelease bundleRelease \
  -x uploadCrashlyticsMappingFileRelease --console=plain
```

**BUILD SUCCESSFUL**, 1분 18초, 63 tasks(40 executed, 23 up-to-date). R8·리소스 축소와 release `lintVital`을 포함한다.

| 산출물 | 크기 | SHA-256 |
| --- | --- | --- |
| `app/build/outputs/bundle/release/app-release.aab` | 8,201,107 bytes | `180d232684a5b8a2b76ea52f1312e5c1772722a57e67ea4ccb3a74ff17afbae2` |
| `app/build/outputs/apk/release/app-release.apk` | 5,741,343 bytes | `c7e584b9fa945e8dcecd90b614c69bbe38ee40c7e3c2aa374b6f9e067344f369` |
| `app/build/outputs/mapping/release/mapping.txt` | 33,288,779 bytes | `a2edf0ed5bdb65223e8fb6d1da7ca19592cce00873173b4ca5985c61254cd43d` |

산출물과 상세 로그는 Git에서 제외한다. 빌드·검증 로그는 `design/qa-release-2026-10-06/`에 보존했다.

## 산출물 검증

- Bundletool 1.18.3의 `validate` 성공. `dump manifest`로 AAB 자체의 패키지 `com.devidea.timeleft`, 버전 **6.6 / 23**, minSdk26 / targetSdk36, 디버그 비활성 확인. APK의 `aapt2 dump badging` 결과도 일치한다.
- AAB `jarsigner -verify` 결과 `jar verified`. 기존 빌드와 같은 자체 서명 인증서·신뢰 체인·타임스탬프 없음·POSIX 속성 경고는 남는다. 서명 실패로 분류하지 않으며 무경고라고 표현하지 않는다.
- AAB 업로드 인증서 SHA-256은 기존 기록과 같은 `65:DA:43:AF:67:24:AE:F8:C7:FA:AE:98:AD:FD:93:BE:41:5E:B6:F2:6B:C5:B0:A9:B6:30:22:71:48:C3:74:56`이다.
- APK `apksigner verify` 성공(v2). `zipalign -c -P 16 -v 4` 성공. AAB 설정은 `PAGE_ALIGNMENT_16K`, 포함된 네이티브 라이브러리 8개의 모든 ELF LOAD 세그먼트 정렬은 16,384 bytes다. 실제 16KB 페이지 기기 실행 검사를 대신하지 않는다.
- AAB 내 `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`이 별도 `mapping.txt`와 SHA-256까지 일치한다. Firebase 매핑 직접 업로드는 제외했다.

## 기능 검증과 남은 범위

버전 범프 직전 동일 기능 코드의 [위젯 실행 QA](QA_WIDGET_SECONDS_COMPATIBILITY_2026-10-06.md): 단위 **113개 통과**, debug·계측 APK 빌드 성공, 전체 debug Lint **오류 0 / 경고 158 / 힌트 7**. 문서6/8 최종 계측은 각각 6개 실행 통과·2개 조건 제외였으며, 실제 런처에서 초 숫자·눈금·시작/종료·프로세스 종료·화면 복귀와 오늘을 확인했다. 문서6 자정 순환도 확인했다. 기존 전체 Lint를 재실행하여 모든 경고를 새 문제와 일대일 비교한 것은 아니다.

이번 버전 범프 뒤에는 release 빌드와 산출물 검증만 수행했다. 단위·전체 debug Lint·계측·화면 검수를 다시 실행하지 않았고, 새 release APK/AAB를 기기에 설치하지 않았다.

- **알려진 결함:** 문서8 / Pixel Launcher의 공간 부족 확대 안내 접근성 영역이 1×1px로 보고된다. 일반 터치는 동작하지만 접근성 48dp 기준을 통과하지 못했다. 정상 숫자 위젯의 본문·새로고침 영역은 확인된 범위에서 정상이다.
- **미검증:** 사용자 휴대폰과 Samsung/One UI에서 장시간 자동 갱신·Doze·전력 사용, 실제 15분 WorkManager 실행 지연, 새 release 설치/업데이트와 Play 분할 APK 실행.
- Play Console 업로드·심사 제출·출시·Git push는 실행하지 않았다. versionCode23은 로컬 저장소의 22에서 증가시킨 값이며 이번 작업에서 Console의 현재 사용 여부를 조회하지 않았다.
- 사용자 `.idea` 변경은 기능·출시 커밋에서 제외했다.

## 한국어 출시 노트 초안

위젯 갱신이 멈췄을 때 자동으로 복구를 시도하도록 개선했습니다. 초 단위 표시 실험 기능을 일부 Android 16 환경까지 확대하고, 시간 숫자와 눈금·상태 전환의 호환성을 보완했습니다. 지원 여부는 Android 버전과 홈 화면 런처에 따라 달라집니다.
