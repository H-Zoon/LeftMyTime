# ‘이만큼의 시간’ 콘텐츠 기준

## 현재 방향 — 2026-10-29

사용자가 윔블던·선수 이름처럼 배경지식이 필요한 기록보다 의지를 주거나 자기계발에 연결되는 이야기를 요청했다. **작은 시작, 연습, 수정, 휴식, 자신의 속도**를 중심으로 14개를 제공한다. 기존 9개를 다시 쓰고 학습·습관 사례 5개를 추가했다. 이 기준이 2026-09-21의 ‘활동 권유·동기부여를 넣지 않는다’는 이전 편집 기준보다 우선한다.

- 이름만 아는 것을 전제하지 않는다. 작가·작곡가·게임 등 누구와 무엇에 관한 이야기인지 짧게 설명한다.
- 출처로 확인되는 사실 또는 당사자가 소개한 방법에 앱이 작성한 짧은 응원을 연결한다. 응원은 인물의 명언이나 연구 결과로 꾸미지 않는다.
- 빨리 해낸 성과보다 작게 시작하고 고쳐 가는 과정을 강조한다. 다른 사람의 작업 시간은 사용자의 달성 기준이 아니다. 성과 보장·횟수 환산·죄책감을 주는 명령·과장된 성공 일화는 쓰지 않는다.
- 한/영 본문은 2~4문장으로 관리한다. 사실의 정밀도와 출처를 함께 확인한다. 습관의 시작 시간·집중 구간·하루 일과·작품 전체 제작 기간을 구별한다.

## 화면과 선택 기준

- 기간/개인 일정 시트와 위젯 상세의 기존 `TimeStoriesSection`, 본문 서체·간격·48dp 버튼·스와이프·저장·출처 보기를 재사용한다. 별도 카드나 홈 피드는 만들지 않는다.
- 제목 → 비슷한 길이의 이야기와 기간 → 사실·과정·짧은 응원 → 기존 행동 순서다. 방법의 권장 구간도 다루므로 한국어 기간 안내를 ‘비슷한 시간의 기록’에서 ‘비슷한 길이의 이야기’로 바꿨다.
- 최초 표시 시 남은 길이와 기준 기간의 비율이 0.5–2인 이야기만 선택한다. 기존 상대 거리 정렬과 읽은 시각에 따른 순서를 유지한다. 원래 기간·실제 달력 계산·남은 시간·진행률은 바꾸지 않는다.
- 범위로 제시된 기간은 선택용 대표값만 사용하고 본문과 기간 표시에 원래 범위를 유지한다. 2분은 습관을 시작하는 상한, 1시간은 50분 공부와 10분 휴식을 합친 예시이며 그 안에 끝낼 수 있는 작업량이 아니다.
- 후보·선택은 읽는 동안과 회전 시 고정한다. 시작 전 시간 일정은 시작까지의 길이와 연결한다. 남은 길이가 0 이하이거나 가까운 사례가 없으면 구역을 숨긴다. 콘텐츠를 추린 만큼 기존에 이야기가 있던 일부 길이에서도 숨겨질 수 있다.
- 보관함 ‘전체’와 상세에는 현재 14개만 노출한다. 이전 17개는 `archivedStories`로 분리해 **이미 저장한 경우에만 ‘저장한 이야기’에서 읽거나 저장 해제**할 수 있다. 기존 ID·즐겨찾기·읽은 시각을 삭제하거나 다른 이야기 ID로 재사용하지 않는다.
- 문구는 한/영 `strings.xml`, 기간·ID·출처 URL은 `TimeStories.kt`에서 관리한다. 외부 자료는 출처를 눌렀을 때만 연다. 새 네트워크 요청·계측·설정·DB 변경은 없다.

## 현재 이야기와 확인 근거

2026-10-29 당사자·공식 기관 자료를 재확인했다. 기간의 대표값은 가까운 이야기 선택용이며 사용자에게 필요한 작업량으로 해석하지 않는다.

| ID | 표시 기간 / 선택 기준 | 주제와 출처 |
| --- | --- | --- |
| `wright-first` | 12초 / 12초 | 첫 비행도 작은 시작이었다는 응원. 비행 시간과 이후 시도는 [미국 국립공원관리청](https://www.nps.gov/wrbr/learn/historyculture/thefirstflight.htm), [공식 안내의 네 비행 기록](https://home.nps.gov/wrbr/planyourvisit/park-brochure.htm). |
| `wright-fourth` | 59초 / 59초 | 같은 날 네 번의 시도, 한 번의 결과로 가능성을 정하지 않기. [미국 국립공원관리청](https://www.nps.gov/wrbr/learn/historyculture/thefirstflight.htm). |
| `two-minute-start` | 2분 이내 / 120초 | 《아주 작은 습관의 힘》의 작은 시작 원칙. 습관 완성 기간이 아니다. [저자 제임스 클리어](https://jamesclear.com/how-to-stop-procrastinating). |
| `newport-planning` | 10~20분 / 15분 | 다음 날 계획을 세우고 예상과 다르면 고치는 일과. [저자 칼 뉴포트](https://calnewport.com/deep-habits-the-importance-of-planning-every-minute-of-your-work-day/). |
| `pomodoro-focus` | 25분 / 1,500초 | 한 가지 일의 집중 구간과 휴식. 25분의 보편적 효과나 완성을 보장하지 않는다. [포모도로 기법 공식 설명](https://www.pomodorotechnique.com/what-is-the-pomodoro-technique/). |
| `study-and-rest` | 1시간 / 3,600초 | 50분 공부 + 10분 휴식의 한 주기. 휴식도 이어 가는 과정. [칼 뉴포트의 학습·휴식 설명](https://calnewport.com/on-deep-breaks/). |
| `murakami-routine` | 하루 5~6시간 / 5.5시간 | 장편 집필 중의 개인 일과이며 소설 전체 집필 기간이나 사용자 목표가 아니다. 자신의 짧은 일과를 응원한다. [파리 리뷰가 공개한 당사자 인터뷰 발췌](https://www.theparisreview.org/blog/2021/08/03/redux-the-runner-trying-to-disappear/). |
| `beatles-session` | 하루 / 86,400초 | 하루에 녹음한 10곡 중 무대에서 연주해 온 곡들이 있었다는 준비의 이야기. 연속 24시간 작업이 아니다. [밴드 공식 앨범 기록](https://www.thebeatles.com/please-please-me). |
| `rocky-draft` | 3일 반 / 3.5일 | 수개월 구상 후 첫 초고, 이어 두 차례 추가 초고. 처음부터 완벽하지 않아도 된다는 응원. [AFI의 인터뷰 기반 제작 기록](https://catalog.afi.com/Film/53862-ROCKY?cxt=topsearches). |
| `handel-messiah` | 24일 / 24일 | 첫 악보 이후 공연에 맞춘 수정. 현재 결과를 마지막 평가로 삼지 않기. [새스커툰 심포니](https://saskatoonsymphony.org/handels-messiah-premiere/). |
| `picasso-guernica` | 약 5주 / 34일 | 5월 1일 스케치부터 6월 4일 완성. 구도·인물의 수정을 기록한 사진에 근거한다. [소장 미술관의 제작 과정 기록](https://guernica.museoreinasofia.es/en/document/dora-maars-report-guernicas-progression). |
| `dickens-carol` | 약 6주 / 42일 | 여러 날의 집필을 오늘의 짧은 문장과 연결한다. 일별 분량은 만들지 않는다. [찰스 디킨스 박물관](https://dickensmuseum.com/blogs/news/the-lost-portrait). |
| `minecraft-pocket` | 약 3개월 / 90일 | 최소 기능의 초기 모바일 버전에서 시작. 90일은 비교값이며 실제 달력 계산에 쓰지 않는다. [게임 공식 개발 회고](https://www.minecraft.net/en-us/article/pocket-edition-5-years-old). |
| `empire-state` | 1년 45일 / 410일 | 1930-03-17~1931-05-01 공사, 주당 약 4층 반의 철골 진행. 큰 목표를 단계로 나누기. [빌딩 공식 역사](https://www.esbnyc.com/about/history). |

## 이번 변경의 확인 범위와 검수 대기

코드·한/영 리소스·출처를 정적으로 검토했다. 한/영 XML 파싱·중복 키 없음·기간 포맷 인자 일치·31개 고유 ID(현재 14/이전 17)·기존 26개 ID 보존·모든 이야기 리소스 참조·새 5개의 한/영 본문과 기간·저장 목록의 이전 항목 필터·`git diff --check`를 확인했다. 빌드·Lint·단위 테스트는 명시적인 최종 검증 요청 전까지 실행하지 않는다. 새 설치·실행 화면 검수도 수행하지 않았다. 설치된 앱에는 새 문구가 반영되지 않았으며 전후 캡처가 없다. 화면 구조·색·서체를 바꾸지 않은 콘텐츠 변경이다.

최종 검증 요청 시 `./gradlew testDebugUnitTest assembleDebug lintDebug`와 함께 다음을 확인한다.

- 실제 `TimeDetailContent`를 쓰는 debug 갤러리의 `detail-story-habit`(2분), `detail-story-planning`(15분), `detail-story-focus`(25분), `detail-story-rest`(1시간), `detail-story-routine`(5.5시간), `detail-story-day`(하루), 기존 `detail-story-seconds`·`detail-story-music`·`detail-story-literature`를 확인한다.
- 상세와 보관함 ‘전체’에 현재 14개만 표시되고 윔블던 등 이전 17개가 후보에 섞이지 않는지 확인한다. 기존 ID의 저장·읽은 시각 보존, 새 5개 저장/해제, 저장해 둔 이전 이야기의 보기/해제, 저장 목록이 비었을 때의 기존 안내를 확인한다.
- 0/음수/유사 사례 없음 숨김, 0.5/2배 경계, 초/일 입력, 회전/재진입/초 감소, 버튼·스와이프 순환과 출처 대응을 확인한다. 시간 선택 알고리즘과 저장 형식은 기존과 같다.
- 밝게/어둡게·320dp·글자 1.0/1.3/2.0·한/영·긴 제목·긴 출처명에서 본문 줄바꿈, 버튼 48dp, 스크롤과 TalkBack을 확인한다. 특히 4문장 본문과 10~20분/하루 5~6시간의 기간 라벨을 포함한다.
- 기간/개인 일정 시트·위젯 상세·보관함에서 같은 문구와 출처를 사용하는지 확인한다. 집중 타이머 실행 화면에는 이야기를 추가하지 않는다.

## 이전 출처 조사 기록 — 2026-09-21·23

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


위 표는 이전 콘텐츠의 근거를 보존한 기록이다. 현재 노출 여부·수정 문구·출처는 위의 2026-10-29 기준을 따른다.
