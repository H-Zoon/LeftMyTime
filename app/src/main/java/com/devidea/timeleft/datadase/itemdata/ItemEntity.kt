package com.devidea.timeleft.datadase.itemdata

import androidx.annotation.NonNull
import androidx.room.*

/*
type = Month, Time 중 하나
summery = 사용자가 지정한 제목
startValue = 시작일 yyyy-mm-yy 형식
endValue = 종료일 yyyy-mm-yy 형식

type == Month 의 경우
autoUpdateFlag 0 = 일정끝난 후 반복안함
autoUpdateFlag 1 = 일정끝난 후 updateRate 일 뒤 반복
autoUpdateFlag 2 = 일정끝난 후 다음달 updateRate 일에 반복

type == Time 인 경우
autoUpdateFlag 0 = 항상 반복
autoUpdateFlag 1 = 주말에 반복하지않음
*/

@Entity
data class ItemEntity(
    @NonNull var type: String,
    @NonNull var summery: String,
    @NonNull var startValue: String,
    @NonNull var endValue: String,
    @NonNull var autoUpdateFlag: Int,
    @NonNull var updateRate: Int,
    @NonNull var alarmRate: Int
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

}