# ‘이만큼의 시간’ 콘텐츠 기준

2026-09-21 사용자 승인: 활동 제안을 제거하고, 남은 시간과 비슷한 길이 안에서 실제로 일어난 일을 보여준다. 코드 구현 상태이며 빌드·테스트·실화면 검수 전이다.

## 화면과 선택 기준

- `TimeDetailContent`의 기간 시트·위젯 상세가 같은 `TimeStoriesSection`을 쓴다. 별도 카드 없이 소제목, 사건의 실제 소요 시간, 짧은 이야기, 다른 이야기/출처 버튼을 표시한다.
- 최초 표시 시 남은 길이와 사건 길이의 비율이 0.5–2인 사례만 선택하고 `abs(ln(사건 길이 / 남은 길이))` 오름차순으로 정렬한다. 더 긴 사건도 들어갈 수 있으므로 ‘비슷한 길이’임을 문구로 밝히고 실제 소요 시간을 표시한다. 정확히 그 시간에 할 수 있다는 뜻으로 쓰지 않는다.
- 후보와 순서는 상세를 읽는 동안 고정한다. 초 감소나 화면 회전으로 교체하지 않고 버튼으로 순환한다. 상세 재진입·대상/상태/기간 변경 시 새 후보를 선택한다. 남은 길이가 0 이하이면 숨긴다. 시작 전 시간 일정은 시작까지의 길이를 사용한다. 날짜 일정의 기존 시작 전·종료 상태 처리도 유지한다.
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

## 추가·수정 원칙

기관·박물관·당사자·주최 측의 근거를 우선하고 기간의 시작/끝과 정밀도를 기록한다. 초고, 집필, 녹음, 경기 시간, 전체 여정을 섞지 않는다. 예술·탐험·과학·스포츠의 사례를 확장하되 동기부여 명령, ‘당신도’, ‘몇 번 가능’ 같은 성과 비교를 붙이지 않는다. 검증할 수 없는 일화는 추가하지 않는다. 한/영 문구와 출처를 함께 검토하며 최신 기록이라는 표현 대신 해당 사건의 날짜를 적는다.

## 검수 대기

- 실제 상세를 쓰는 갤러리: `detail-time`, `detail-today`, `detail-month`, `detail-year`, `detail-story-seconds`, `detail-story-music`, `detail-story-literature`, `detail-waiting`, `detail-expired`, `detail-last-second`.
- 25일→메시아, 42일→디킨스, 12초→첫 비행, 0/음수/비슷한 사례 없음→구역 숨김, 0.5/2배 경계와 상대 거리 순서, 초/일 입력을 최종 검증한다.
- 버튼 순환, 재진입, 회전, 초 감소, 시작/종료 전환, 진행 숨김에서도 이야기 유지, 출처 링크와 브라우저 부재를 확인한다.
- 밝게/어둡게·320dp·글자 1.0/1.3/2.0·한/영·긴 출처명·긴 제목·빈 상태에서 줄바꿈과 48dp 버튼, 기준선, 스크롤을 확인한다. 시각 결과와 전후 이미지는 사용자 요청 후 새 APK로 기록한다.

코드·문구·XML 구문·중복 키·누락 참조·고유 ID/출처 대응·공백 오류를 정적으로 검토했다. 빌드·Lint·단위 테스트·새 APK 설치·시각 검수는 명시적인 최종 검증 요청 전까지 수행하지 않는다.
