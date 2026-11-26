# 6.9 (26) 서명 배포 빌드 — 2026-11-09

사용자 버전 범프 지시에 따라 versionName을6.8→6.9, versionCode를25→26으로 올리고 기존 키로 다시 빌드했다. **배포 전달본은 이 문서의6.9(26)이며, 같은 날 먼저 생성한6.8(25)을 대체한다.** 기능 코드는 추가로 수정하지 않았다. [출시 노트](../store-assets/release-notes/6.9-ko-KR.txt)를 준비했다.

## 빌드·검증

JBR21, 실행별 힙4GB/메타스페이스1GB, worker2로 다음 범위를 실행했다.

```sh
./gradlew '-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8' --max-workers=2 testReleaseUnitTest assembleRelease bundleRelease lintRelease -x uploadCrashlyticsMappingFileRelease --console=plain
```

- BUILD SUCCESSFUL: 2분8초, 76 tasks(38실행/38최신).
- Release 단위: 28 suites, 163개 통과·실패/오류/제외0.
- Lint 오류0, 경고162·Hint7. 같은 날 범프 전 검사와 항목 비교 추가0/제거0.
- Bundletool1.18.3 validate, AAB와 APK 메타데이터에서 앱ID `com.devidea.timeleft`·6.9/26·debuggable=false 확인.
- AAB jarsigner·APK apksigner V2 통과. 양쪽 인증서 SHA-256은 기존 `65da43af6724aef8c7faae98adfd93be415eb6f26bc5b0a9b630227148c37456`와 일치한다.
- APK zipalign16KB, AAB PAGE_ALIGNMENT_16K, 네이티브30개 ELF LOAD 정렬>=16KB·APK/AAB 바이트 일치 확인.
- AAB 내 난독화 매핑과 전달 mapping.txt 일치, 키/비밀번호 설정 파일 미포함 확인.
- Release에서 debug 갤러리·이야기·제거된 문장 입력 Activity 없음, SettingsActivity exported=false 확인.

## 전달 파일

| 파일 | 바이트 | SHA-256 |
| --- | ---: | --- |
| [TimeLeft-6.9-26.aab](../app/build/outputs/release-6.9-26-2026-11-09/TimeLeft-6.9-26.aab) | 30,975,649 | `14daacdffa5c814eb74c8ab938c764a80f623fcebdd00f1d856a3bfdd5cdcb18` |
| [TimeLeft-6.9-26.apk](../app/build/outputs/release-6.9-26-2026-11-09/TimeLeft-6.9-26.apk) | 28,691,656 | `7fbaaf594c9be0658482f29c851f840500014524db2de81a7f099d5f35c5a6d0` |
| [mapping.txt](../app/build/outputs/release-6.9-26-2026-11-09/mapping.txt) | 74,114,416 | `83da95cd1af3167f01885aaf24227f40cafe49845f86c97aa38e1f6e7b46d0fd` |

같은 폴더에 SHA256SUMS.txt를 기록했고 사본과 Gradle 원본 해시 일치를 확인했다. 이전6.8 산출물은 이력으로 보존한다.

## 범위

이번 서명 Release의 설치·실화면·실기기 검수는 하지 않았다. Debug/계측은 재실행하지 않았으며 직전 범위는 [최종 Debug 검수](QA_FINAL_2026-11-09.md)를 따른다. Play Console 업로드·versionCode26 사용 여부 확인·Firebase 배포·Crashlytics 매핑 업로드·커밋/push는 하지 않았다. 기존 광고 SDK 경고는 남아 있으며 모든 광고 서비스 실행 통과를 뜻하지 않는다.

로그와 검증 JSON은 Git 제외 `design/qa-release-6.9-2026-11-09/`에 보존한다.
