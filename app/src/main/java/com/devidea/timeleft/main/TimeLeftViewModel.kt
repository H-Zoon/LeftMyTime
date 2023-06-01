package com.devidea.timeleft.main

import android.util.Log
import androidx.lifecycle.*
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.ListLiveData
import com.devidea.timeleft.datadase.itemdata.DataRepository
import com.devidea.timeleft.main.MainActivity.Companion.ITEM_GENERATE
import com.devidea.timeleft.datadase.itemdata.ItemDao
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime.*
import java.time.format.DateTimeFormatter

class TimeLeftViewModel(private val repository: DataRepository) : ViewModel() {
    private val listFlow: Flow<List<ItemEntity>> = repository.allData
    var listFlow2: MutableLiveData<ArrayList<AdapterItem>> = MutableLiveData()
    var listFlow3: ArrayList<AdapterItem> = ArrayList()

    init {
        viewModelScope.launch {
            listFlow.collectLatest { list ->
                Log.d("datachange", "yes")
                while (isActive) {
                    listFlow3 = ArrayList()
                    for (i in list.indices) {
                        if ((list[i].type == "Time")) {
                            listFlow3.add(
                                ITEM_GENERATE.customTimeItem(
                                    list[i]
                                )
                            )
                        } else {
                            listFlow3.add(
                                ITEM_GENERATE.customMonthItem(
                                    list[i]
                                )
                            )
                        }
                    }
                    listFlow2.value = listFlow3
                    delay(1000L)
                }
                }
            }
        }
    }


//ViewModel 을 통해 전달되는 인자가 있을 때 사용
class TimeLeftViewModelFactory(
    private val repository: DataRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimeLeftViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimeLeftViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}