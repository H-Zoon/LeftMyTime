## 1인 기획, 개발에서 스토어 출시까지.  -TimeLeft-

**2022.07 ~ 지금까지 다양한 유저들의 피드백을 받아 성장중인 어플리케이션입니다.** 

![unnamed](https://github.com/H-Zoon/LeftMyTime/assets/43941511/c6d6dc62-68ca-4963-b367-011d8bfda834)


### UseSkill

- MVVM 패턴 적용
- Android Jetpack (Compose UI, Android KTX)
- Flow / Live Data를 이용한 반응형 프로그래밍
- **Collaboration** - Gitflow, Figma

### 사용자의 피드백으로 발전하는 TimeLeft

2022.07월부터 지금까지 TimeLeft를 서비스하며 많은 유저들의 기능 추가요구가 있었습니다.

1. 한눈에 크게 볼 수 있는 시간 지나간 정도
2. 밤에도 편하게 볼 수 있는 야간모드 지원
3. 사용자가 원하는 항목 추가하기 
4. 위젯기능을 추가하여 어디서든 시간 확인하기.

다음과 같은 내용의 피드백이 주요 내용이였고, 이를 개선, 기능 추가하여 유저의 사용성을 지속적으로 발전시키는 작업을 진행하였습니다.

![Screenshot_20230521_210852_TimeLeft](https://github.com/H-Zoon/LeftMyTime/assets/43941511/0c731773-c798-489c-af71-a41993ddf0bd)

이 과정에서 **단순한 카드형태**의 일정확인기능, **실시간으로 변화하는** 시간 업데이트,  **서랍형식의 아이템** 추가, **주간/야간모드**를 지원하여 사용자가 쉽고 편하게 이용하도록 구현하였습니다.

### 새로운 기술 적용으로 반응성 향상시키기

사용자의 항목 추가 기능을 개발하면서 각 초마다 해당 항목을 업데이트 해야한다는 난관에 봉착하였습니다.

또한 해당 프로젝트는 자바로 개발되어 당시 화두였던 코틀린을 적용해 보고 싶었습니다.

따라서 Flow를 통해 사용자 아이템을 반환받고 실시간으로 업데이트 하는 기능을 성공적으로 구현하고, 해당 프로젝트 전체를 코틀린으로 Migration을 진행하였습니다.

```kotlin
// Flow<ArrayList<AdapterItem>> 타입을 반환하는 함수를 정의합니다.
private fun initItem(): Flow<ArrayList<AdapterItem>> = flow {
        // 데이터베이스에서 모든 아이템을 비동기적으로 가져옵니다.
        AppDatabase.getDatabase(App.context()).itemDao().getAllFlow().collect() { allItems ->
            // AdapterItem을 담을 ArrayList를 초기화합니다.
            val itemListArray = ArrayList<AdapterItem>()

            // 디버깅을 위한 로그 메시지를 출력합니다.
            Log.d("Flow", "flow")

            // 모든 아이템에 대해 반복하며 처리합니다.
            for (i in allItems.indices) {
                // 아이템의 타입을 로그로 출력합니다.
                Log.d("Flow", allItems[i].type)

                // 아이템의 타입에 따라 다른 AdapterItem을 생성합니다.
                if ((allItems[i].type == "Time")) {
                    // 타입이 "Time"인 경우, customTimeItem을 생성하여 ArrayList에 추가합니다.
                    itemListArray.add(
                        ITEM_GENERATE.customTimeItem(
                            allItems[i]
                        )
                    )
                } else {
                    // 그 외의 경우, customMonthItem을 생성하여 ArrayList에 추가합니다.
                    itemListArray.add(
                        ITEM_GENERATE.customMonthItem(
                            allItems[i]
                        )
                    )
                }
            }

            // 생성한 ArrayList를 Flow로 방출합니다. 이를 통해 Flow를 수집하는 곳에서 이 ArrayList를 받아 처리할 수 있습니다.
            emit(itemListArray)
        }
    }
```

### review

개발자로서 나만의 어플리케이션을 제작하고 출판하고싶은 욕망이 있었습니다.  처음해보는 1인기획, 개발 그리고 스토어 출간에 두려움이 있었습니다.  

**하지만, 이를 성공적으로 이루었고 지금까지 많은 사람들이 이용하고 있습니다. 또한 그 과정에서 Coroutine과 RoomDB를 통해 비동기적 UI 업데이트, 사용자 친화적인 UI를 구성하는 능력을 얻었습니다.** 

또한 스토어에 업로드를 위한 이미지파일 준비와 프로덕트 출시를 준비하며 각 버전에 대한 관리, 출판 능력을 길렀습니다. 

모두 처음해보는 과정이였지만 효율적인 어플리케이션 개발. 어플리케이션의 출반까지 전 과정을 공부할 수 있었던 좋은 기회였습니다.
