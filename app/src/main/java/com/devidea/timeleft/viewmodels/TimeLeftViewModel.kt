package com.devidea.timeleft.viewmodels

import androidx.lifecycle.*
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.activity.MainActivity.Companion.ITEM_GENERATE
import com.devidea.timeleft.datadase.itemdata.ItemDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime.*
import java.time.format.DateTimeFormatter

class TimeLeftViewModel(private val itemDao: ItemDao) : ViewModel() {

    // 내부에서 설정하는 자료형은 뮤터블로 변경가능하도록 설정
    private var _timeValue = MutableLiveData<String>()
    private var _recyclerViewValue = MutableLiveData<AdapterItem>()

    // 변경되지 않는 데이터를 가져올 때 이름을 _언더스코어 없이 설정
    // 공개적으로 가져오는 변수는 private 이 아닌 public으로 외부에서도 접근 가능하도록 설정
    // 하지만 값을 직접 라이브데이터에 접근하지 않고 뷰모델을 통해 가져올 수 있도록 설정

    val timeValue: LiveData<String>
        get() = _timeValue

    val recyclerViewValue: LiveData<AdapterItem>
        get() = _recyclerViewValue

    init {
        viewModelScope.launch {
            while (true) {
                _timeValue.value = now().format(DateTimeFormatter.ofPattern("a h:m:ss"))
                _recyclerViewValue.value = ITEM_GENERATE.timeItem()
                delay(1000)
            }
        }
    }

}

//ViewModel 을 통해 전달되는 인자가 있을 때 사용
class TimeLeftViewModelFactory(
    private val itemDao: ItemDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimeLeftViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimeLeftViewModel(itemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}