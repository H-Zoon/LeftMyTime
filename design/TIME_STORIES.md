# ‘이만큼의 시간’ 콘텐츠 기준

2026-09-21 사용자 승인: 활동 제안을 제거하고, 남은 시간과 비슷한 길이 안에서 실제로 일어난 일을 보여준다. 코드 구현 상태이며 빌드·테스트·실화면 검수 전이다.

2026-09-23 사용자 승인: 조사한 추가 후보 10개를 반영해 총 26개로 확장했다. 기존 `TimeStoriesSection`의 노출·순환·출처 보기와 기간 연결 규칙을 재사용하며 한/영 문구를 함께 추가했다.

## 화면과 선택 기준

- `TimeDetailContent`의 기간/개인 일정 시트·위젯 상세가 같은 `TimeStoriesSection`을 쓴다. 별도 카드 없이 소제목, 사건의 실제 소요 시간, 짧은 이야기, 다른 이야기/출처 버튼을 표시한다.
- 최초 표시 시 남은 길이와 사건 길이의 비율이 0.5–2인 사례만 선택하고 `abs(ln(사건 길이 / 남은 길이))` 오름차순으로 정렬한다. 더 긴 사건도 들어갈 수 있으므로 ‘비슷한 길이’임을 문구로 밝히고 실제 소요 시간을 표시한다. 정확히 그 시간에 할 수 있다는 뜻으로 쓰지 않는다.
- 후보와 순서는 상세를 읽는 동안 고정한다. 초 감소나 화면 회전으로 교체하지 않고 버튼 또는 본문의 가로 스와이프로 순환한다. 180ms/8dp 이동과 페이드를 적용하고 출처와 접근성 설명은 선택한 이야기 하나를 따른다. 시스템 애니메이션 배율 0이면 즉시 전환한다. 상세 재진입·대상/상태/기간 변경 시 새 후보를 선택한다. 남은 길이가 0 이하이면 숨긴다. 시작 전 시간 일정은 시작까지의 길이를 사용한다. 날짜 일정의 기존 시작 전·종료 상태 처리도 유지한다.
- 양의 남은 길이에도 사례가 없을 수 있다. 빈 카드나 억지 대체 문구를 표시하지 않는다. 사례가 하나면 다른 이야기 버튼을 숨기고 출처만 제공한다. 일수는 달력 길이 비교에만 사용한다.
- 한/영 문구는 `strings.xml`, 기간·고유 ID·출처 URL은 `TimeStories.kt`에서 관리한다. 외부 데이터 요청·실시간 생성·새 저장 설정을 추가하지 않는다. 출처는 사용자가 눌렀을 때만 외부 앱에서 열며 브라우저가 없으면 안내한다.

## 확인한 사례와 출처

확인일: 2026-09-21. 아래는 짧게 다시 쓴 사실이며 원문 인용이나 이미지 사용이 아니다. 기준 초는 가까운 사례 선택용이고 화면에는 출처의 정밀도에 맞춘 기간을 쓴다.

| ID | 표시 기간 | 기간의 범위와 확인한 출처 |
| --- | --- | --- |
| `wright-first` | 12초 | 1903년 첫 동력 비행 한 차례, 약 36m. [스미스소니언](https://airandspace.si.edu/collection-objects/1903-wright-flyer/nasm_A19610048000) |
| `wright-fourth` | 59초 | 같은 날 네 번째 비행. [스미스소니언](https://airandspace.si.edu/multimedia-gallery/video/inventing-airplane-changing-world) |
| `bannister-mile` | 3분 59.4초 | 1954년 1마일 경기 기록. 기준값도 239.4초이며 현재 세계기록이라는 뜻이 아니다. [세계육상연맹](https://worldathletics.org/news/news/3594) |
| `shepard-flight` | 15분 22초 | 1961년 발사부터 착수까지. [NASA](https://www.nasa.gov/history/may-5-1961-alan-shepard-becomes-the-first-american-in-space/) |
| `white-spacewalk` | 약 23분 | 1965년 우주유영 시작/종료 기준. NASA 인물 소개의 캡슐 밖 체류 21분과 혼용하지 않는다. [NASA APOD](https://apod.nasa.gov/apod/ap050604.html) |
| `ganna-hour` | 1시간 | 2022년 트랙 56.792km 주행. 최신 기록 보유 여부를 주장하지 않는다. [국제사이클연맹](https://www.uci.org/pressrelease/filippo-ganna-breaks-the-uci-hour-record-timed-by-tissot/82aysypXuU0sdkVD69YVI) |
| `gagarin-flight` | 1시간 48분 | 1961년 최초 유인 우주비행 108분. [NASA](https://science.nasa.gov/resource/yuri-gagarin-first-human-in-space/) |
| `isner-mahut` | 11시간 5분 | 2010년 경기의 누적 플레이 시간. 사흘에 걸쳐 열렸다고 본문에 명시하며 연속 체류 시간과 구분한다. [기네스 세계기록](https://www.guinnessworldrecords.com/world-records/94409-longest-tennis-wimbledon-match-singles) |
| `beatles-session` | 하루 | 1963-02-11에 앨범 14곡 중 10곡 녹음. 24시간 연속 녹음·앨범 전체 제작이라는 뜻이 아니다. 하루는 비교용 달력 단위다. [비틀스 공식](https://www.thebeatles.com/please-please-me) |
| `lindbergh-flight` | 33시간 30분 | 1927년 뉴욕–파리 단독 무착륙 비행. [스미스소니언](https://www.si.edu/newsdesk/releases/national-air-and-space-museum-lowers-spirit-st-louis-ground-level) |
| `gemini-four` | 약 4일 | 1965년 임무를 NASA 소개의 일 단위 정밀도로 표시한다. [NASA](https://www.nasa.gov/mission/gemini-iv/) |
| `apollo-eleven` | 약 8일 3시간 | 1969년 발사–지구 귀환 전체 임무. 선택 기준 195시간 18분 35초, 표시에는 반올림된 ‘약’을 쓴다. [NASA](https://www.nasa.gov/history/apollo-11-mission-overview/) |
| `handel-messiah` | 24일 | 1741-08-22부터 09-14까지 초기 악보 작성. 이후 수정이 있었음을 본문에 명시하며 ‘교향곡’으로 부르지 않는다. [새스커툰 심포니](https://saskatoonsymphony.org/handels-messiah-premiere/) |
| `dickens-carol` | 약 6주 | 1843년 《크리스마스 캐럴》 집필 기간. 출판·인쇄·평생의 구상 기간과 구분한다. 선택 기준은 42일. [찰스 디킨스 박물관](https://dickensmuseum.com/blogs/news/the-lost-portrait) |
| `bly-journey` | 약 72일 | 실제 세계 일주. 자료의 일 단위 정밀도를 따르며 분·초를 지어내지 않는다. [미국 국립공원관리청](https://home.nps.gov/people/nellie-bly.htm) |
| `perseverance-journey` | 203일 | 2021년 화성 착륙까지의 지구 출발 후 여정. 개발 기간과 구분한다. [NASA 착륙 발표](https://www.nasa.gov/news-release/touchdown-nasas-mars-perseverance-rover-safely-lands-on-red-planet/) |

### 추가 사례 — 2026-09-23 확인

| ID | 표시 기간 | 기간의 범위와 확인한 출처 |
| --- | --- | --- |
| `park-cube` | 3.13초 | 2023-06-11 맥스 박의 3×3 큐브 한 차례 풀이. 현재 세계기록 보유 여부를 주장하지 않는다. [기네스 세계기록](https://www.guinnessworldrecords.com/news/2023/6/max-park-makes-history-by-solving-cube-in-fastest-time-ever-752905) |
| `cage-silence` | 4분 33초 | 1952년 《4분 33초》 초연. 연주자가 건반을 누르지 않은 공연 길이이며 작곡 기간이나 주변까지 무음이었다는 뜻이 아니다. 선택 기준 273초. [뉴욕 현대미술관](https://www.moma.org/collection/works/163616) |
| `versailles-balloon` | 8분 | 1783-09-19 양·오리·수탉을 태운 열기구 비행. 준비 시간은 제외하며 생환 후 왕실 동물원에 자리를 얻었다는 후일담을 포함한다. [베르사유 궁전](https://en.chateauversailles.fr/discover/history/key-dates/first-hot-air-balloon-flight) |
| `queen-live-aid` | 약 21분 | 1985-07-13 퀸의 라이브 에이드 웸블리 무대. 사전에 배정된 19/20분과 실제 공연 길이를 혼용하지 않으며 전체 라이브 에이드 행사의 길이가 아니다. [퀸 공식 사이트](https://www.queenonline.com/news/watch-queen-the-greatest-live-aid-episode-30) |
| `honnold-el-capitan` | 3시간 56분 | 2017-06-03 알렉스 호놀드의 엘캐피탄 프리 솔로 등반 한 차례. 준비·훈련 기간이 아니며 2018년 별도 2인 등반 기록과 구분한다. 선택 기준 14,160초. [내셔널 지오그래픽](https://www.nationalgeographic.com/magazine/2019/02/alex-honnold-made-ultimate-climb-el-capitan-without-rope/) |
| `rocky-draft` | 3일 반 | 스탤론의 인터뷰를 인용한 AFI 기록상 《록키》의 첫 시나리오 초고 작성 기간. 수개월의 구상과 이후 두 차례 추가 초고가 있었음을 본문에 명시하며 영화 전체 제작 기간으로 쓰지 않는다. 선택 기준 3.5일. [미국영화연구소](https://catalog.afi.com/Film/53862-ROCKY?cxt=topsearches) |
| `mozart-linz` | 약 4일 | 1783-10-31 편지와 나흘 뒤 공연을 근거로 한 《린츠 교향곡》 작곡 기간. 가져온 교향곡이 없었다는 설명이며 악보를 분실했다고 단정하지 않는다. 24시간 연속 작업량으로 표현하지 않는다. [시드니 심포니 해설 15–16쪽](https://files.baskercdn.com/sydneysymphony/files/3316_sso_programs2023_17mozartsgreatmassincminor_art_web_singles.pdf) |
| `picasso-guernica` | 약 5주 | 1937-05-01 첫 스케치부터 06-04 완성까지. 선택 기준은 날짜 차이 34일, 표시는 약 5주다. 5월 11일부터의 본화 작업과 구별한다. 폭 776.6cm는 본문에서 약 7.8m로 표시한다. [레이나 소피아 미술관 작품 기록](https://www.museoreinasofia.es/en/collections/artwork/guernica-0/) · [스케치·제작 과정 기록](https://guernica.museoreinasofia.es/en/document/dora-maars-report-guernicas-progression) |
| `minecraft-pocket` | 약 3개월 | 최소 기능만 갖춘 초기 모바일 버전의 개발 기간. 현재 제품 전체 개발 기간이 아니며, 초기에는 멀티플레이 계획도 없었다는 개발자 설명을 포함한다. 선택용 비교값만 약 90일로 두며 앱의 실제 달력 계산에 적용하지 않는다. [마인크래프트 공식 사이트](https://www.minecraft.net/en-us/article/pocket-edition-5-years-old) |
| `empire-state` | 1년 45일 | 102층 엠파이어 스테이트 빌딩의 1930-03-17부터 1931-05-01까지 공사 기간. 해당 날짜 차이인 410일을 선택 기준으로 사용하며 다른 연도의 일수를 365일로 고정하지 않는다. [빌딩 공식 역사](https://www.esbnyc.com/about/history) |

## 추가·수정 원칙

기관·박물관·당사자·주최 측의 근거를 우선하고 기간의 시작/끝과 정밀도를 기록한다. 초고, 집필, 녹음, 경기 시간, 전체 여정을 섞지 않는다. 예술·탐험·과학·스포츠의 사례를 확장하되 동기부여 명령, ‘당신도’, ‘몇 번 가능’ 같은 성과 비교를 붙이지 않는다. 검증할 수 없는 일화는 추가하지 않는다. 한/영 문구와 출처를 함께 검토하며 최신 기록이라는 표현 대신 해당 사건의 날짜를 적는다.

## 검수 대기

- 실제 상세를 쓰는 갤러리: `detail-time`, `detail-today`, `detail-month`, `detail-year`, `detail-story-seconds`, `detail-story-music`, `detail-story-literature`, `detail-waiting`, `detail-expired`, `detail-last-second`.
- 25일→메시아, 42일→디킨스, 12초→첫 비행, 0/음수/비슷한 사례 없음→구역 숨김, 0.5/2배 경계와 상대 거리 순서, 초/일 입력을 최종 검증한다.
- 추가 10개는 각 사건과 가까운 남은 길이에서 후보에 들어가고 순환할 수 있는지 확인한다. 특히 3초→큐브, 4분 33초→케이지, 8분→열기구, 21분→퀸, 3시간 56분→호놀드, 3일 반(초 입력)→록키, 4일→모차르트/제미니 동률, 34일→피카소, 90일→마인크래프트, 410일→엠파이어 스테이트 빌딩을 확인한다. 새 콘텐츠 추가로 달라진 후보 수·순서와 기존 사례의 접근성을 포함한다.
- 버튼/좌우 스와이프 순환, 빠른 연속 조작, 세로 스크롤 충돌, 전환 중 출처 대응, 재진입, 회전, 초 감소, 시작/종료 전환, 진행 숨김에서도 이야기 유지, 출처 링크와 브라우저 부재를 확인한다.
- 밝게/어둡게·320dp·글자 1.0/1.3/2.0·한/영·긴 출처명·긴 제목·빈 상태에서 줄바꿈과 48dp 버튼, 기준선, 스크롤을 확인한다. 시각 결과와 전후 이미지는 사용자 요청 후 새 APK로 기록한다.
- 케이지·열기구처럼 두 문장이 긴 본문과 시드니 심포니·마인크래프트 등의 긴 출처명, PDF 출처의 외부 열기를 포함한다. 이번 콘텐츠 추가는 화면 레이아웃을 바꾸지 않았으며 실제 화면 검수를 완료한 것으로 취급하지 않는다.

코드·문구·XML 구문·중복 키·누락 참조·고유 ID/출처 대응·공백 오류를 정적으로 검토했다. 빌드·Lint·단위 테스트·새 APK 설치·시각 검수는 명시적인 최종 검증 요청 전까지 수행하지 않는다.
