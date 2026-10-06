# Play Console 출시 준비 — 6.4 (21)

> **최신 빌드:** 2026-10-06 위젯 갱신 복구·초 표시 하위 호스트 지원을 포함한 **6.6(23) 서명 APK/AAB**를 생성했다. [최신 빌드·서명·검증 범위](RELEASE_BUILD_2026-10-06.md)를 따른다. 이전 홈·목록 정렬/아이콘과 자동 수집 변경도 포함한다. Console에는 재업로드하지 않았으므로 아래 6.4(21)의 초안·신고·공개 정책 상태와 구분한다.

> **2026-09-28 후속 변경:** 사용자 요청으로 로컬 코드를 필수 자동 수집·전송으로 변경했다. 아래 내용은 선택형 수집인 **6.4(21)의 업로드 당시 기록**이다. 업로드된 AAB·설치 APK·스토어 초안·공개 개인정보처리방침에는 새 변경이 반영되지 않았다. 이번 변경 후 빌드·테스트·Console 수정은 실행하지 않았고, 스토어 등록정보는 사용자가 수정한다. 새 서명 빌드에는 21보다 높은 미사용 versionCode가 필요하다. 최신 구현과 검증 대기는 [작업 인계](WORK_HANDOFF.md)의 2026-09-28 항목을 따른다.

## 현재 상태

사용자의 서명 빌드·Play Console 업로드·스토어 정보 갱신 요청으로 진행했다. **릴리즈와 데이터 보안 선언은 검토 전송 대기, 스토어 문구·이미지는 초안 저장 상태**다. Google 검토 제출·앱 출시·Git 커밋·푸시는 하지 않았다. 공개 개인정보처리방침은 사용자 후속 승인으로 교체 게시했다.

- Console의 기존 프로덕션: 6.3 / versionCode 20. 로컬 6.1 / 18을 **6.4 / 21**로 올렸다.
- AAB 업로드 성공, 릴리즈 이름 `6.4 (21)`, 한국어 출시 노트 입력 및 저장.
- 게시 개요에 `6.4 (21) 전체 출시 시작`, `데이터 보안 설문지 작성`의 **변경사항 2개**가 표시됨. 광고 ID 미사용 선언도 저장됨.
- **관리형 게시가 꺼져 있다.** 검토 제출 후 승인 시 게시될 수 있으므로, 스토어 초안을 마무리하기 전에 제출하지 않는다. 롤아웃은 현재 화면의 기본 100%이며 실제 시작하지 않았다.
- 업로드 검사의 광고 ID 오류는 선언을 실제 manifest와 일치시켜 해소했다. 남은 경고 1개는 네이티브 디버그 기호 미포함이다. R8 ReTrace 매핑은 AAB에 포함되어 Console에서 인식됐다.
- Console의 빠른 정책·품질 검사는 마지막 확인에서 완료되어 ‘이제 검토를 위해 변경사항을 전송할 수 있습니다’로 표시됐다. Google의 본 심사 승인이나 기기별 사전 출시 보고서 통과로 확대하지 않는다.

## 빌드·서명과 검증 범위

기준 커밋 `b28146e`, `develop`의 작업 트리에서 버전만 변경했다. 기능 코드·DB·설정 키는 수정하지 않았다.

```sh
./gradlew assembleRelease bundleRelease assembleDebug \
  -x uploadCrashlyticsMappingFileRelease --console=plain
```

JBR 21로 성공, 1분 22초 / 107 tasks(47 executed, 60 up-to-date). 초기 제한 환경에서 Gradle 캐시 쓰기가 막혀 승인된 확장 실행으로 완료했다. 별도 단위 테스트·전체 Lint·계측 테스트는 이번 버전 변경 후 재실행하지 않았으며 release 빌드의 lintVital은 통과했다. 기존 기능 검증은 [직전 최종 QA](QA_RELEASE_2026-09-27.md)의 수행 범위만 인용한다.

| 산출물 | SHA-256 |
| --- | --- |
| `app/build/outputs/bundle/release/app-release.aab` | `f33ab8c5e89730e7a286e093fd823ed0d9ea93d092cd20a955a0afd02c91dd63` |
| `app/build/outputs/apk/release/app-release.apk` | `2daae3a200e133b4cd5dc6f47ee0055515d3807867df1ac1967c8e01837a460d` |

- APK 서명 검증, 16KB ZIP 정렬 통과. AAB는 `jar verified` 및 Console 업로드 수락 확인. JDK의 자체 서명 인증서/신뢰 체인/타임스탬프·JAR 처리 관련 경고를 무경고로 표현하지 않는다.
- 업로드 인증서 SHA-256 `65:DA:43:AF:67:24:AE:F8:C7:FA:AE:98:AD:FD:93:BE:41:5E:B6:F2:6B:C5:B0:A9:B6:30:22:71:48:C3:74:56`이 Console과 일치. Google의 앱 서명 인증서와 업로드 인증서는 별개다.
- 패키지 `com.devidea.timeleft`, minSdk26 / targetSdk36, 6.4 / 21 확인. 지원 기기에서 이 업데이트로 제외된 기기는 0개였다.
- Firebase Crashlytics 매핑의 직접 업로드는 제외했다. Play의 R8 매핑 인식과 Firebase 매핑 업로드 성공을 혼동하지 않는다.
- 기존 API37 에뮬레이터에 debug 6.4 / 21을 갱신 설치해 실제 구성요소를 촬영했다. 실제 휴대폰, Play 분할 APK 설치·스토어 6.3에서 업데이트, 장기 운영 조건은 이번에 검수하지 않았다.

## 스토어 소개 자료

원문은 [`store-assets/store-listing/ko-KR.json`](../store-assets/store-listing/ko-KR.json)에 보존했다. 앱 이름은 유지하고 짧은 설명(57자), 자세한 설명(1,225자), 출시 노트(293자)를 신규 기능에 맞췄다. 위젯 초 표시의 Android17 이상·지원 런처·선택형 실험 기능 조건을 명시했다. 기기 간 동기화 제공으로 설명하지 않았다.

| 자료 | 파일·형식 | Console 상태 |
| --- | --- | --- |
| 아이콘 | `store-assets/store-listing/app-icon-512.png`, 기존 최신 512×512 | 교체 후 초안 저장 |
| 대표 배너 | `store-assets/store-listing/feature-graphic-1024x500.jpg`, 1024×500 | 이전 보라색 배너 교체 후 초안 저장 |
| 전화 스크린샷 | `store-assets/store-listing/screenshots/ko-KR/`, RGB PNG 1080×1920, 6장 | 01~06 순서로 교체 후 초안 저장 |

스크린샷 순서: 시간 중심 홈 → 일반/요약 위젯 → 전체 일정 → 시간 이야기 → 테마 미리보기 → 다크 모드. `TimeLeftTheme`, 홈·목록·상세·테마의 운영 구성요소와 실제 위젯 렌더러를 재사용한 debug 갤러리를 촬영했다. 예시 일정임을 표시했으며 사용자 일정·개인 화면을 업로드하지 않았다. 외부 문구와 여백만 코드로 합성하고 앱 UI 비율을 유지했다. 별도의 가짜 앱 화면이나 생성형 사진을 만들지 않았다.

**AI 제작 여부는 사용자가 직접 선택하기로 답변했다.** 이전 8개 애셋 일괄 지정 시도는 자동 승인 검토가 외부 정책 신고의 명시적 승인·관여 범위 확인 부족을 이유로 거부했으며, 재시도하지 않았다. 소개 문구·이미지는 `초안 변경사항`으로 저장했고 AI 선택값은 비워 두었다. 스토어 등록정보의 2/2 검토 화면에서 사용자가 신고를 선택한 뒤 저장해야 게시 개요의 변경사항에 포함된다.

## 데이터 보안·공개 정책

기존 Console 데이터 보안은 2022년 9월 13일의 ‘수집 없음’이었다. 신규 `AppTelemetry`의 기본 꺼짐 선택형 수집, manifest의 광고 ID 제거, Firebase SDK 공개 안내 및 실제 설정을 대조했다.

저장한 신고:

| 항목 | 값 |
| --- | --- |
| 수집 데이터 | 대략적인 위치, 앱 상호작용, 비정상 종료 로그, 진단, 기기 또는 기타 ID |
| 각 데이터의 처리 | 수집됨 / 임시 처리 아님 / 사용자 선택 / 애널리틱스 목적 |
| 제3자 공유 | 공유 없음. 확인한 Firebase 처리 위탁과 Play의 서비스 제공업체 예외를 적용 |
| 전송 암호화 | 예 |
| 계정 생성·외부 계정 로그인 | 제공하지 않음 |
| 데이터 삭제 요청 선택 질문 | 미선택. 익명 기록을 개별 이용자와 연결해 삭제할 수 있다고 단정하지 않음 |

Console 미리보기에서 5개 항목 모두 `옵션 / 애널리틱스`로 표시되고 `변경사항이 저장되었습니다. 게시 개요에서 전송하여 검토를 받으세요.`를 확인했다. Google 검토에는 전송하지 않았다.

실제 Firebase 프로젝트 `timeleft-f83ba`와 연결 Analytics 속성의 **읽기 전용 확인**:

- Firebase의 Google Analytics·Google Play 연결 확인. Google Ads·BigQuery·Cloud Logging은 연결 버튼 상태, Slack·Jira·PagerDuty는 설치 버튼 상태였다. 새 연결/설치는 하지 않았다.
- Analytics ‘Google 제품 및 서비스’ 데이터 공유 꺼짐. 참여 모델링/집계 통계·기술 지원·계정 기반 비즈니스 추천은 켜져 있었다. 이를 전부 꺼져 있다고 표현하지 않는다.
- Google 신호 데이터는 사용 설정 전 상태. 도시 수준 위치·기기 메타데이터 수집은 켜져 있었다. 위치 권한/GPS 수집과 구별하여 ‘대략적인 위치’에 반영했다.
- 이벤트 보관 2개월, 사용자 보관 14개월, 새 활동 시 사용자 보관 기간 재설정 켜짐. 집계 보고서는 별도 정책이다. 콘솔 설정은 변경하지 않았다.
- Analytics 계정에 데이터 처리 약관 미수락 안내가 있었으나 약관에 동의하지 않았다. 운영자는 실제 적용 범위와 계약 상태를 확인해야 한다.
- GA의 모든 제품 링크를 개별 전수 검수하거나 실제 동의/철회에 따른 서버 이벤트 도착·삭제를 시험한 것은 아니다. 기존 [QA의 수집 실증 미검수](QA_RELEASE_2026-09-27.md)는 유지한다.

**공개 정책 갱신 완료.** 사용자가 ‘TimeLeft만 사용하므로 기존 페이지 교체’를 선택했다. 기존 Google Sites의 게시 주소 `developsidea`를 대조한 뒤, ‘수집 없음’의 2022년 본문을 앱의 최신 [`docs/privacy.html`](../docs/privacy.html)로 교체했다. 변경 전후 검토 화면에서 홈 본문만 변경됨을 확인하고 게시했다. [기존 공개 주소](https://sites.google.com/view/developsidea/%ED%99%88)를 새로 열어 2026년 9월 26일 시행일·선택형 수집 설명을 확인하고, 원문의 48개 문단·항목이 모두 존재하는지 비교해 누락 0개를 확인했다. 주소·접근 권한·사이트 검색 설정은 변경하지 않았다. 공개 정책과 앱 내 정책을 동일하게 유지했고 Console URL도 그대로 유효하다.

공식 판단 근거: [Firebase SDK 공개 안내](https://firebase.google.com/docs/android/play-data-disclosure), [Analytics 데이터 공개](https://support.google.com/analytics/answer/11582702?hl=en), [Play 데이터 보안 정의·위탁 예외](https://support.google.com/googleplay/android-developer/answer/10787469?hl=ko), [AI 애셋 라벨 안내](https://support.google.com/googleplay/android-developer/answer/17262077?hl=ko).

## 이어서 할 일

1. 사용자가 스토어 등록정보의 AI 제작 여부를 직접 선택하고 저장한다.
2. 공개 정책 갱신은 완료했다. 운영 설정을 바꿀 때 앱 내 정책·공개 정책·데이터 보안 신고를 함께 유지한다.
3. 기존 QA의 실기기·스플래시 중간 모션·런처 조건을 확인한다. Console 빠른 검사는 완료됐고, 네이티브 기호 경고는 릴리즈 차단 오류와 구별한다.
4. 게시 개요에서 릴리즈·스토어·데이터 보안·광고 ID를 함께 검토한 뒤 사용자의 출시 지시에 따라 검토 제출한다. 현재 관리형 게시가 꺼져 있음을 고려한다.

증거·빌드 로그·캡처 스크립트는 Git 제외 `design/qa-store-release-2026-09-27/`에 있다. `console-publishing.png`, `console-store-draft.png`, `console-store-ai-handoff.png`, `console-data-safety-saved.png`, `console-ad-id.png`, `analytics-retention.png`, `privacy-published.png`, `store-preview.png`를 참조한다. 실제 스토어용 최종 이미지는 `store-assets/`에 보존했다. 에뮬레이터는 기본 1280×2856·정상 MainActivity로 복귀했다.
