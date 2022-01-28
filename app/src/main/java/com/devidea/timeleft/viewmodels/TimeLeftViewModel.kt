package com.devidea.timeleft.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalDateTime.*
import java.time.format.DateTimeFormatter

class TimeLeftViewModel() : ViewModel() {

    // 내부에서 설정하는 자료형은 뮤터블로 변경가능하도록 설정
    private var _currentValue = MutableLiveData<String>()
    private var _currentValue2 = MutableLiveData<String>()

    // 변경되지 않는 데이터를 가져올 때 이름을 _언더스코어 없이 설정
    // 공개적으로 가져오는 변수는 private 이 아닌 public으로 외부에서도 접근 가능하도록 설정
    // 하지만 값을 직접 라이브데이터에 접근하지 않고 뷰모델을 통해 가져올 수 있도록 설정
    val currentValue: LiveData<String>
        get() = _currentValue

    val currentValue2: LiveData<String>
        get() = _currentValue2

    init {
        _currentValue.value = now().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
        viewModelScope.launch {
            while (true) {
                _currentValue2.value = now().format(DateTimeFormatter.ofPattern("a h:m:ss"))
                delay(1000)
            }
        }
    }

}