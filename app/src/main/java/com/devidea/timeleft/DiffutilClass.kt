package com.devidea.timeleft

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.devidea.timeleft.databinding.ActivityMainBinding
import java.util.ArrayList

class DiffutilClass(
    private val oldTiles: ArrayList<AdapterItem>,
    private val newTiles: ArrayList<AdapterItem>,
    private val adapter: BottomRecyclerView
) : DiffUtil.Callback(){
    //이전 목록의 갯수 반환
    override fun getOldListSize(): Int {
        return oldTiles.size
    }
    //새로운 목록의 갯수 반환
    override fun getNewListSize(): Int {
        return newTiles.size
    }
    //두 객체가 같은 항목인지 확인
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        //Log.d("areItemsTheSame",(oldTiles[oldItemPosition].id == newTiles[newItemPosition].id).toString() )
        return oldTiles[oldItemPosition].id == newTiles[newItemPosition].id
    }
    //두 항목의 데이터가 같은지
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        //Log.d("areContentsTheSame",(oldTiles[oldItemPosition].leftDay == newTiles[newItemPosition].leftDay).toString() )
        //Log.d("leftDay",(oldTiles[oldItemPosition].leftDay).toString() + (newTiles[newItemPosition].leftDay.toString()) )
        return oldTiles[oldItemPosition].leftDay == newTiles[newItemPosition].leftDay
    }
    //페이로드 반환
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        //Log.d("getChangePayload",oldItemPosition.toString() )
        //adapter.notifyItemChanged(oldItemPosition, newTiles[newItemPosition])
        return adapter.notifyItemChanged(oldItemPosition, newTiles[newItemPosition])
    }
}