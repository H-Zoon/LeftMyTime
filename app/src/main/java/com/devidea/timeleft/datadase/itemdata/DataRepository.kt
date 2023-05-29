package com.devidea.timeleft.datadase.itemdata

import com.devidea.timeleft.datadase.itemdata.ItemDao
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DataRepository (private val itemdao: ItemDao) {
    val allData: Flow<List<ItemEntity>> = itemdao.getAllData()

    fun deleteItem(id: Int){
        CoroutineScope(Dispatchers.IO).launch {
            itemdao.deleteItem(id)
        }
    }


}