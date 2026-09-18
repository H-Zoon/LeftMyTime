# LeftMyTime 디자인 기준 v1 — 이전 구현 기록

2026-09-18 사용자가 A안 ‘시간 중심’을 선택했다. 이후 디자인 결정은 [디자인 기준 v2](DESIGN_SYSTEM.md)를 따른다.
아래 내용은 개편 전 코드와 검수 환경을 이해하기 위한 기록이다. 둥근 글꼴·카드·진행 링을 유지하라는 신규 UI 지침으로 사용하지 않는다.

작성일: 2026-09-18. 기준: 현재 저장소의 Compose UI와 연결된 기기의 홈 화면.
이 문서는 앞으로 구현할 이상적인 전체 개편안이 아니라 **현재 코드에서 재사용할 기준**이다.

## 제품의 인상

조용하고 명확한 시간 도구. 남은 시간이 시각적 중심이며 장식은 정보를 보조한다.
기존 나눔스퀘어라운드, 원형 진행 표시, 사용자 선택 팔레트를 유지한다.
화면을 추가해도 같은 앱으로 느껴지도록 탐색, 카드, 텍스트 위계를 공유한다.

## 코드가 기준이 되는 위치

| 역할 | 기준 | 구현 |
| --- | --- | --- |
| 색상 | 선택 팔레트의 의미별 색상 사용 | `ui/theme/TimeLeftTheme.kt` |
| 글꼴 | 모든 Material 텍스트 역할에 나눔스퀘어라운드 | `TimeLeftTypography` |
| 화면 좌우 여백 | 16dp | `LayoutTokens.ScreenHorizontal` |
| 화면 세로 여백·섹션 간격 | 16dp | `ScreenVertical`, `SectionGap` |
| 내용 카드 | surface 배경, medium 모서리 16dp, 기본 그림자 없음 | `ui/components/TimeLeftCard.kt` |
| 카드 내용 여백 | 일반 내용 16dp; 입력 행 등 밀도 조정은 Spacing 사용 | `LayoutTokens.CardPadding` |
| 뒤로 가기와 화면 제목 | titleLarge, background 배경, 고정 상단바 | `ui/components/TimeLeftTopAppBar.kt` |
| 터치 영역 | 최소 48dp; 내부 아이콘은 더 작아도 됨 | `LayoutTokens.MinTouchTarget` |
| 세부 간격 | 4 / 8 / 12 / 16 / 20 / 28dp | `Spacing` |
| 움직임 | 작은 피드백 150ms, 일반 전환 200ms, 대표 링 720ms | `Motion` |

현재 공통 카드 적용 범위: 홈 대표 카드·일정 카드·빈 상태, 편집의 일반 입력/설정 카드와 시간 다이얼, 설정 섹션.
날짜 범위 요약의 색상 면, 작은 배지, 선택 버튼은 일반 내용 카드와 용도가 달라 별도 Surface를 사용한다.
위젯 설정과 RemoteViews 위젯의 전체 정렬은 후속 과제다.

## 색상 의미

- `primary`: 주요 행동과 시간 강조. 같은 종류의 추가 행동은 같은 색을 사용한다.
- `primaryContainer / onPrimaryContainer`: 목록/격자 등 선택 상태. 반드시 현재 팔레트를 따른다.
- `surface / onSurface`: 카드와 기본 내용. `background / onBackground`: 화면 바탕.
- `onSurfaceVariant`: 보조 설명. `error`: 오류와 삭제 행동.
- 카테고리 색상은 사용자가 고른 메타데이터 색상이다. 다크 테마에서는 `itemAccentColor`가 같은 색조의 밝기를 높인다. 그 선택만으로 앱의 구조나 타이포그래피가 달라지지 않는다.
- 색만으로 선택·종료·오류를 구별하지 않는다. 텍스트 또는 선택 의미를 함께 제공한다.

이번 수정은 실제 사용 중인 primary container와 surface tint의 Material 기본 보라색 유입을 막는다.
아직 사용하지 않는 색상 역할까지 전체 브랜드 팔레트로 정의한 것은 아니다. 새로운 Material 구성요소를 도입할 때 소비하는 색상 역할과 전경/배경 대비를 함께 확인한다.

## 정보와 글자 위계

| 용도 | 기본 텍스트 역할 |
| --- | --- |
| 대표 남은 시간 | displaySmall 또는 링 내부 headlineSmall |
| 대표 일정 제목 | headlineSmall |
| 화면 제목 | titleLarge |
| 일정·설정 항목 제목 | titleMedium / bodyLarge |
| 종료 날짜·보조 설명 | bodyMedium |
| 짧은 구분·보조 라벨 | labelLarge |

현재 크기를 크게 바꾸지 않고 누락된 글꼴 역할을 채웠다. 작은 본문(11–12sp), 고정 높이 헤더, 좁은 격자의 긴 카운트다운은 추가 검증·개선 대상이다.
숫자와 링의 의미는 각각 남은 시간/경과 비율이다. 링을 남은 비율로 바꾸는 것은 외관 수정이 아닌 제품 동작 변경이다.

## 변경 순서

1. 사용자 문제를 한 문장으로 적는다. 예: “초록 테마에서도 선택 버튼이 보라색이라 다른 화면처럼 느껴진다.”
2. 기존 토큰·구성요소로 해결한다. 새 규칙이면 사용처와 이유를 이 문서에 추가한다.
3. debug 갤러리/Preview에서 동일 데이터로 전후를 비교한다.
4. 기능 검증과 시각 검증 결과를 따로 기록한다. 빌드 성공만으로 화면 품질을 보증하지 않는다.
5. 출시 후에는 변경 범위에 맞는 실제 사용자 반응과 오류를 확인한다. 다음 기능 전에 새 기준의 문제를 먼저 수정한다.

## 시각 검증

이번 실제 확인 범위와 이미지는 [검수 기록](QA_2026-09-18.md)에 있다.

`app/src/debug/java/com/devidea/timeleft/design/DesignGallery.kt`에 홈 목록·격자·빈 상태·날짜 편집·시간 편집·설정 Preview가 있다.
각 Preview는 5개 팔레트와 밝게/어둡게/320dp 큰 글자(1.5배) 조합을 제공한다. 실제 검증 여부는 별도 기록한다.
일정 표본은 고정되어 있지만 편집 화면의 날짜 계산과 권한 상태는 실행 환경을 따른다. 이미지 자동 비교를 도입할 때는 시계와 권한도 주입해야 한다.

에뮬레이터에서 debug 전용 갤러리를 열 수 있다. `screen`: home, grid, empty, editor, editor-time, settings. 실제 DB 저장 콜백은 비어 있다. 알림 권한 버튼처럼 OS와 상호작용하는 기능은 에뮬레이터에서만 확인한다.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.devidea.timeleft/.design.DesignGalleryActivity --es screen home --es palette emerald --es theme light
adb -s emulator-5554 shell am start -n com.devidea.timeleft/.design.DesignGalleryActivity --es screen home --es palette emerald --es theme dark --ef fontScale 1.5
```

화면을 바꿀 때 기존 갤러리 Activity를 종료하고 다시 연다. 이 Activity와 샘플 데이터는 release 빌드에 들어가지 않는다.

| 조건 | 확인 내용 |
| --- | --- |
| 5개 팔레트 × 밝게/어둡게 | 선택 버튼·칩·대화상자에 다른 팔레트 색이 섞이지 않는가 |
| 320dp / 기본 / 큰 글자 1.5배 이상 | 시간·주요 행동이 겹치거나 사라지지 않는가 |
| 빈 목록 / 일정 1개 / 여러 개 | 화면 우선순위와 추가 동선이 명확한가 |
| 긴 제목 / 긴 카테고리 / 종료 일정 | 줄바꿈·잘림·정보 구분이 적절한가 |
| 한국어 / 영어 | 라벨과 날짜 표현이 레이아웃을 깨지 않는가 |
| 터치 / TalkBack | 이름·선택 상태·뒤로 가기·핵심 시간 정보가 전달되는가 |
| 위젯 | 앱과 시간 의미·테마가 일치하며 작은 크기에서도 읽히는가 |

## 참고

- [Android: Compose 디자인 시스템](https://developer.android.com/develop/ui/compose/designsystems)
- [Android: 접근성 기본값과 터치 영역](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- [Android: Compose Preview와 여러 환경 미리보기](https://developer.android.com/develop/ui/compose/tooling/previews)
