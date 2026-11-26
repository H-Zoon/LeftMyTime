# 코드 구조

기준 경로는 `app/src/main/java/com/devidea/timeleft/`다. 앱은 `:app` 단일 모듈을 유지하며, 화면과 그 화면의 Activity·ViewModel·전용 표현은 같은 기능 패키지에 둔다.

## 기능별 배치

| 패키지 | 책임 |
| --- | --- |
| `home` | 홈 화면·일정 목록·대표 선택, `MainActivity`, `TimeLeftViewModel`, 시작 화면 전환 |
| `editor` | 일정 편집 화면·입력 상태·시간 다이얼, `ItemEditorActivity` |
| `settings` | 설정·리워드 알림 설정·테마 미리보기·개인정보처리방침 화면과 Activity |
| `focus`, `calendar`, `backup`, `templates`, `periods` | 각 기능의 화면과 전용 상태·로직 |
| `videorecords` | 날짜 일정의 개인 영상 기록, 촬영·로컬 파일 저장과 Media3 재생 |
| `pointhome` | PointHome 연동·소개 화면·노출 정책 |
| `widget` | 위젯 설정·표현·갱신·초 표시와 런처 대응 |
| `notification` | 일정 알림 예약·수신·권한 판정·공통 권한 안내 UI |
| `messaging` | FCM 메시지·리워드 동의/구독·푸시 표시와 진입 처리 |

## 함께 사용하는 코드

- `ui`에는 앱과 위젯이 함께 쓰는 `TimeDisplayItem`, 표시용 시간 포맷, 상세 정보, 아이콘 표현이 있다. `TimeDisplayFactory.fromSchedule`은 현재 회차의 일정 한 건을 표시 모델로 바꾼다. 현재 회차 선택과 오류 처리는 호출부에서 담당한다.
- `TimeDisplayFactory.standardPeriods()`는 오늘·이번 달·올해 순으로 목록을 만든다. 홈은 초마다 오늘만 갱신하고, 월·연 값은 날짜가 바뀔 때 계산해 재사용한다.
- 홈 날짜 표시는 `TimeLeftViewModel.kt`의 `HomeDateCache`에서 원본 일정·현재 날짜·고정 만료 상태를 확인한 뒤 현재 회차를 계산한다. 캐시는 홈 데이터 수집 흐름 안에서 소유한다. `HomeHeroCandidates`는 대표 선택과 날짜 목록이 같은 정렬 결과를 사용하게 하며, 전체 일정의 검색·정렬은 전체보기에서 계산한다.
- `ui/components`와 `ui/theme`에는 여러 화면이 공유하는 UI와 디자인 토큰을 둔다.
- `calc`는 시간·날짜·반복 회차를 계산한다. `database`에는 Room 정의와 일정을 읽고 쓰는 `TimeLeftRepository`를 함께 둔다.
- `config`는 사용자 설정 키와 원격 설정, `telemetry`는 계측을 담당한다. 앱 시작과 의존성 연결은 루트의 `App.kt`와 `AppModule.kt`에서 찾을 수 있다.

새 기능의 파일은 먼저 해당 기능에 둔다. 클래스 종류만으로 `activity`, `viewmodels` 패키지를 다시 만들지 않는다. 실제로 여러 기능이 공유하는 UI만 `ui/components`에 둔다. 새 기능마다 UI·data·domain 등의 하위 계층을 일괄 생성하지 않는다.

## 외부에 저장되는 이름

Activity의 구현은 기능 패키지에 있지만, 이미 설치된 앱의 런처 바로가기나 알림이 저장한 컴포넌트 이름은 유지해야 한다. Manifest에 다음 이전 이름을 `activity-alias`로 보존한다.

| 기존 컴포넌트 | 실제 Activity |
| --- | --- |
| `.activity.MainActivity` | `.home.MainActivity` |
| `.activity.ItemEditorActivity` | `.editor.ItemEditorActivity` |
| `.activity.SettingsActivity` | `.settings.SettingsActivity` |
| `.activity.PrivacyPolicyActivity` | `.settings.PrivacyPolicyActivity` |

런처의 `MAIN`/`LAUNCHER` 필터는 기존 Main alias 한곳에 둔다. 실제 홈 Activity의 `singleTop`과 시작 테마를 유지한다. 새 앱 내부 Intent는 실제 Activity를 가리키며 이전 컴포넌트로 저장된 Intent는 alias가 연결한다. target Activity를 alias보다 앞에 선언하는 등 [Android의 activity-alias 규칙](https://developer.android.com/guide/topics/manifest/activity-alias-element)을 따른다. 업그레이드 후 기존 알림·최근 앱·런처의 동작은 최종 설치 검증 대상이다.

`AppDatabase`의 패키지·DB 이름·버전과 내보낸 스키마 경로는 유지한다. Entity·enum의 패키지를 정리할 때 테이블·컬럼·raw 값이나 기존 데이터·사용자 설정 키를 바꾸지 않는다. 위젯 provider·Receiver·Worker의 클래스명도 이번 정리의 대상이 아니다.

## 테스트와 검증

기능 테스트는 같은 패키지를 사용한다. 예를 들어 홈 선택 테스트는 `app/src/test/java/com/devidea/timeleft/home/HomeSelectionTest.kt`에 둔다. Debug 디자인 갤러리는 이동한 실제 화면을 계속 재사용한다.

사용자의 명시적 최종 검증 요청 전에는 빌드·Lint·단위 테스트를 실행하지 않는다. 이번 변경의 실제 검증 범위와 남은 조건은 `WORK_HANDOFF.md`의 2026-11-03 구조 정리 기록을 따른다.
