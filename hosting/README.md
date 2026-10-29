# TimeLeft 개발자 웹사이트 / app-ads.txt

Firebase 프로젝트: `timeleft-f83ba`

2026-10-29 첫 배포 완료:

- 개발자 웹사이트: [https://timeleft-f83ba.web.app](https://timeleft-f83ba.web.app)
- 판매자 목록: [https://timeleft-f83ba.web.app/app-ads.txt](https://timeleft-f83ba.web.app/app-ads.txt)
- 개인정보처리방침: [https://timeleft-f83ba.web.app/privacy.html](https://timeleft-f83ba.web.app/privacy.html)
- 소개 페이지·방침·CSS·판매자 목록 모두 공개 HTTP 200이며 로컬 파일과 바이트 단위로 일치함을 확인했다. 응답 헤더와 해시는 `deployment-report.json`에 기록했다.
- Play Console 웹사이트 등록과 아바티 측 크롤러 인식 확인은 아직 수행하지 않았다.

## 게시 파일

- `public/index.html`: 앱 소개, Google Play·문의·이 사이트의 개인정보처리방침 링크
- `public/privacy.html`: `docs/privacy.html`에서 생성한 개인정보처리방침 게시본. 직접 편집하지 않고 `python3 sync-privacy.py`로 갱신한다.
- `public/styles.css`: 앱 디자인 v2의 밝은/어두운 색상 역할과 시스템 글꼴, 얇은 눈금·구분선. CSS 변수는 문서의 웹 대응 값이며 Android의 Kotlin 테마를 직접 가져오지는 않는다.
- `public/app-ads.txt`: 사용자가 제공한 PointHome 판매자 목록
- `source/app-ads.original.txt`: 받은 원문. 공개 디렉터리 밖에서 보관한다.
- `app-ads-report.json`: 원문/게시본 해시와 중복 정리·검토 범위

공개 배포 디렉터리는 `public/`뿐이다. 이 사이트에는 가입·분석 SDK·쿠키 배너를 추가하지 않았다. 이 설명은 Firebase Hosting 자체의 서버 측 로그까지 없다는 의미는 아니다. 개인정보처리방침은 재검증 캐시 헤더를 사용한다.

앱의 `PrivacyPolicyActivity`는 같은 HTTPS 방침 주소를 WebView에서 연다. 기존 `PrivacyPolicyScreen`과 `TimeLeftTopAppBar`를 재사용하며 외부 정책 링크는 브라우저, 이메일은 메일 앱으로 연결한다. 최초 URL 변경을 설치 앱에 적용하려면 새 앱 빌드 배포가 필요하다. 이 변경이 설치된 이후에는 같은 URL의 방침 본문만 갱신할 때 앱 재배포가 필요하지 않다. 온라인 문서이므로 네트워크 연결이 필요하다.

## 목록 반영 기록 — 2026-10-29

- 원문 2,034줄 중 첫 줄은 ‘app-ads.txt line’이라는 설명이다.
- 판매자 기록 2,033개에서 공백·도메인 대소문자·관계 표기를 정규화한 정확한 중복 453개를 제거했다.
- 게시 목록은 고유 항목 1,580개(DIRECT 95개, RESELLER 1,485개)다.
- 판매자/계정 ID의 철자·대소문자와 거래 관계를 보존했다. 3개 필드/4개 필드 기록도 임의로 합치지 않았다.
- 같은 계정에 DIRECT와 RESELLER가 함께 제공된 32개 경우는 공급 목록대로 두 관계를 모두 유지했다. 어느 관계가 맞는지 추정하여 수정하지 않았다.
- 첨부된 공급 목록에 포함된 판매자는 SDK 직접 의존성 목록만으로 필터링하지 않는다. 광고 판매 경로와 SDK 탑재 목록은 같은 개념이 아니다.
- 구문·중복·원문 대비 고유 항목 보존을 확인했다. 개별 판매자 계정 소유권과 공급 계약까지 독립적으로 검증한 것은 아니다.

## 이후 갱신

Firebase CLI는 [공식 설치 안내](https://firebase.google.com/docs/cli)를 따른다. 이 작업에서는 Node.js 24와 Firebase CLI 15.34.2로 실행했다. 다른 PC나 새 CLI 세션에서는 프로젝트 권한이 있는 계정으로 로그인해야 한다.

```bash
cd /Users/jun/LeftMyTime/hosting
firebase login
python3 sync-privacy.py
firebase deploy --only hosting --project timeleft-f83ba
```

아바티에서 새 목록을 받으면 원문을 보관하고 `public/app-ads.txt`를 갱신한 뒤 다시 배포한다. 판매자 ID와 DIRECT/RESELLER 값을 임의로 바꾸지 않는다. 앱 재빌드는 필요하지 않다.

## 공개 검증과 스토어 연결

배포가 성공하면 CLI가 출력한 Hosting URL을 기준으로 다음을 확인한다.

1. `/`에 소개 페이지가 보인다.
2. `/app-ads.txt`는 HTTP 200, `text/plain; charset=utf-8`로 응답한다.
3. 응답 본문은 로컬 파일과 일치한다. HTML 소개 페이지로 재작성하거나 로그인 화면으로 보내지 않는다.
4. HTTPS 파일 주소를 아바티에 전달하고 해당 공급 목록의 인식을 확인받는다.
5. Play Console의 스토어 연락처 ‘웹사이트’에는 Hosting의 루트 주소를 등록한다. 개인정보처리방침 URL에는 `https://timeleft-f83ba.web.app/privacy.html`을 등록한다. Console 변경은 이번 작업에서 수행하지 않았다.
6. `/privacy.html`은 HTTP 200, HTML 응답이며 게시 본문이 `docs/privacy.html`과 일치하는지 확인한다.

앱 소개 사이트의 대표 기능 문구는 실제 앱·스토어 설명을 바탕으로 작성했다. 실제 앱 화면이나 가상의 사용 후기를 추가하지 않았다.

## 화면 확인 범위

기본 데스크톱 어두운 화면과 360px 검수용 밝은 화면을 확인했다. 작은 화면에서는 실제 콘텐츠 폭 345px에서 가로 넘침이 없고 모든 링크 터치 영역이 48px 이상임을 DOM으로 확인했다. 공개 사이트의 기본 화면도 확인했다. 캡처는 Git에서 제외되는 `design/qa-hosting-2026-10-29/`에 있다. 큰 글자·스크린리더·모든 기기별 실화면을 검증한 것은 아니다. 웹사이트는 고정된 소개 콘텐츠이며 빈 데이터 상태나 입력 제목 상태를 갖지 않는다.

방침 페이지도 공개 데스크톱 어두운 화면과 360px 밝은 화면을 확인했다. 본문이 화면 안에서 줄바꿈하며 11개 항목과 시행일을 유지한다. 본문 링크도 최소 48px 높이를 제공한다. 네이티브 앱 화면·오프라인·외부 앱 연결은 실행 검수하지 않았다.

방침은 작은 화면의 글자 200%에서도 줄바꿈과 가로 넘침 없음을 확인했다. 이 검수는 루트 글꼴 크기 확대이며 Android 시스템 글자 설정 검수는 아니다. 방침 CSS 주소에 파일 해시를 붙여 이전 브라우저 캐시 때문에 새 스타일 반영이 늦어지는 것을 방지한다.
