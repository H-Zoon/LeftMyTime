
# LeftMyTime

LeftMyTime은 사용자가 설정한 특정 날짜 또는 시간까지 남은 시간을 계산하고 관리할 수 있도록 도와주는 안드로이드 애플리케이션입니다. 디데이(D-day) 계산, 반복 일정 관리 등 다양한 기능을 통해 중요한 이벤트를 잊지 않고 준비할 수 있도록 지원합니다.

## 스토어 링크

<a href='https://play.google.com/store/apps/details?id=com.devidea.timeleft'><img alt='Google Play에서 다운로드' src='https://play.google.com/intl/en_us/badges/static/images/badges/ko_badge_web_generic.png' width="200"/></a>

## 주요 기능

* **디데이(D-day) 계산**: 특정 날짜까지 남은 일수 또는 지난 일수를 계산합니다.
* **시간 계산**: 특정 시간까지 남은 시간을 시, 분, 초 단위로 표시합니다.
* **반복 설정**: 일정 종료 후 지정한 일수 뒤 또는 매월 지정한 날짜에 반복할 수 있습니다.
* **위젯 지원**: 홈 화면에 위젯을 추가하여 앱을 실행하지 않고도 남은 시간을 확인할 수 있습니다.
* **사용자 맞춤 설정**: 다양한 테마와 설정을 통해 자신만의 스타일로 앱을 꾸밀 수 있습니다.
* **로컬 데이터 저장**: 일정은 기기의 Room 데이터베이스에 저장됩니다. Android 백업 및 복원은 현재 비활성화되어 있습니다.

## 기술

* **언어**: [Kotlin](https://kotlinlang.org/)
* **아키텍처**: MVVM (Model-View-ViewModel)
* **UI**:
    * Jetpack Compose
    * Material 3
    * RemoteViews 기반 홈 화면 위젯
* **비동기 처리**:
    * Coroutines
    * Lifecycle KTX (ViewModel, Flow)
* **데이터베이스**:
    * Room

## 라이브러리

### Android Jetpack

* **`androidx.appcompat`**: 다양한 안드로이드 버전에서 일관된 UI와 기능을 제공하기 위한 라이브러리입니다.
* **`com.google.android.material`**: 머티리얼 디자인 가이드라인을 따르는 UI 컴포넌트 모음입니다.
* **`androidx.compose`**: 선언형 UI를 구현하기 위한 Jetpack Compose 라이브러리입니다.
* **`androidx.constraintlayout`**: 복잡한 UI를 평평한 뷰 계층 구조로 만들 수 있는 유연한 레이아웃입니다.
* **`androidx.room`**: SQLite 데이터베이스를 쉽게 사용할 수 있도록 도와주는 ORM(Object Relational Mapping) 라이브러리입니다.
* **`androidx.lifecycle`**: `ViewModel`, `Flow` 수집 등을 통해 UI 컨트롤러의 생명주기를 관리하고, 생명주기를 인지하는 컴포넌트를 만들 수 있도록 지원합니다.
* **`androidx.activity:activity-ktx`**: `by viewModels` 와 같은 Kotlin 확장 기능을 제공하여 Activity에서 ViewModel을 더 쉽게 사용할 수 있도록 합니다.
* **`androidx.preference:preference-ktx`**: 설정 화면을 쉽게 구현할 수 있도록 도와주는 라이브러리입니다.

### Coroutines
* **`org.jetbrains.kotlinx:kotlinx-coroutines-android`**: 안드로이드의 Main 스레드에서 UI를 차단하지 않고 비동기 작업을 수행할 수 있도록 지원합니다.

### 테스트
* **`junit:junit`**: 자바 프로그래밍 언어용 단위 테스트 프레임워크입니다.
* **`androidx.test.ext:junit`**: 안드로이드 테스트를 위한 JUnit 확장 라이브러리입니다.
* **`androidx.test.espresso:espresso-core`**: 안드로이드 UI 테스트 작성을 위한 프레임워크입니다.

## 시작 가이드

### 요구사항

* Android Studio
* Min SDK 26
* Compile SDK 36
* Target SDK 36

### 빌드 방법

1.  이 저장소를 클론합니다.
    ```bash
    git clone [https://github.com/h-zoon/leftmytime.git](https://github.com/h-zoon/leftmytime.git)
    ```
2.  Android Studio에서 프로젝트를 엽니다.
3.  필요한 Gradle 종속성이 동기화될 때까지 기다립니다.
4.  'Run' 버튼을 클릭하거나 `Shift+F10`을 눌러 앱을 빌드하고 실행합니다.
