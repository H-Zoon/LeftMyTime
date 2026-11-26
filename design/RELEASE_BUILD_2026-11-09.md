# 6.8 (25) 서명 배포 빌드 — 2026-11-09

후속 버전 범프: **이6.8(25) 산출물은 배포 전달본에서 제외한다.** 사용자 지시로 [6.9(26)을 다시 빌드·검증](RELEASE_BUILD_6.9_2026-11-09.md)했다. 아래는 범프 전 이력이다.

사용자가 AAB·기존 서명 키를 명시해 현재 미커밋 변경을 포함한 Release를 빌드했다. 버전은 6.8(25)을 유지했다. Play/Firebase 업로드·Crashlytics 매핑 업로드·커밋·push는 수행하지 않았다.

## 빌드와 검사

JBR21, 실행별 힙4GB/메타스페이스1GB, worker2. 기본 샌드박스에서 기존 Gradle 캐시 잠금 파일 접근이 거부되어 승인된 캐시 접근으로 다시 실행했다.

```sh
./gradlew '-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8' --max-workers=2 testReleaseUnitTest assembleRelease bundleRelease lintRelease -x uploadCrashlyticsMappingFileRelease --console=plain
```

- BUILD SUCCESSFUL: 3분1초, 76 tasks(47실행/29최신).
- Release 단위: 28 suites, 163개 통과, 실패·오류·제외0.
- Release Lint: 오류0, 경고162·Hint7. 10-30 보고서는 경고166·Hint7이다. 상세 차이는 Git 제외 `qa-release-2026-11-09/lint-release-comparison.json`에 기록했다.
- Lint 비교의 추가11/제거14 항목은 파일 경로 이동에 따른 기존 제안과 의존성 최신 버전 안내 변화 등을 포함한다. 새 항목을 검토했으며 11개의 새 결함으로 해석하지 않는다. 이전부터 남은 API33 권한 상수·상태 생성·KTX 제안은 유지한다.
- 광고 SDK의 R8 Missing class 경고22종은 10-30 로그와 동일하며 새 클래스는 없다. 모든 광고 서비스 경로의 실행 통과를 뜻하지 않는다.
- Debug·계측은 이번 빌드 작업에서 재실행하지 않았다. 직전 Debug 검수는 [최종 검수](QA_FINAL_2026-11-09.md)를 따른다.

## 전달 파일과 검증

이전 `release-6.8-25/`는 보존하고 새 `release-6.8-25-2026-11-09/`에 사본을 생성했다. 원본과 SHA-256 일치, 같은 폴더의 `SHA256SUMS.txt`를 확인했다.

| 파일 | 바이트 | SHA-256 |
| --- | ---: | --- |
| [TimeLeft-6.8-25.aab](../app/build/outputs/release-6.8-25-2026-11-09/TimeLeft-6.8-25.aab) | 30,975,413 | `83bc10c08ac007e01006fb177f39e66a495584f31bf4339be8e17c14af449a65` |
| [TimeLeft-6.8-25.apk](../app/build/outputs/release-6.8-25-2026-11-09/TimeLeft-6.8-25.apk) | 28,691,656 | `fef93ea16e2c5054f124fb75c23c6331d3f87fe999aabce967d976ec5d9d51d2` |
| [mapping.txt](../app/build/outputs/release-6.8-25-2026-11-09/mapping.txt) | 74,113,957 | `5fc5d97612f8ed680307ad73c588cd4f0ef648426e7dd9a0cf88fcb6e2f6043a` |

- 기존 서명 인증서 SHA-256: `65da43af6724aef8c7faae98adfd93be415eb6f26bc5b0a9b630227148c37456`. AAB/APK 모두 일치한다.
- Bundletool1.18.3 validate, 앱ID `com.devidea.timeleft`, 6.8/25, debuggable=false 확인.
- AAB jarsigner, APK apksigner V2·zipalign16KB 검증 통과.
- AAB PAGE_ALIGNMENT_16K, 네이티브30개 ELF LOAD 정렬>=16KB, APK/AAB 네이티브 바이트 일치.
- AAB 내 난독화 매핑과 전달 mapping.txt 일치. 비밀번호 설정·키 파일 패키징 없음.
- Release에서 debug 갤러리·이야기 Activity·제거한 문장 입력 Activity가 없으며 SettingsActivity exported=false.

## 범위

이번 서명 Release의 설치·실화면/실기기 검수는 하지 않았다. Play Console의 versionCode25 사용 여부·등록 업로드 인증서는 직접 조회하지 않았다. 새 UI·데이터 삭제 동작의 직전 Debug 검수와 서명 Release 실행 검수를 구분한다. 코드·디자인 변경은 추가하지 않았다.

로그·서명·매니페스트·정렬·테스트 합계·Lint 비교는 Git 제외 `design/qa-release-2026-11-09/`에 보존한다.
