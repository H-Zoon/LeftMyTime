package com.devidea.timeleft

import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.animation.ObjectAnimator
import android.widget.ProgressBar
import android.view.View
import java.util.ArrayList

/**
이 TopRecyclerView 는 ViewHolder 의 Binding 을 통해 작성되었습니다.
 */

internal class TopRecyclerView
constructor(private val arrayList: ArrayList<AdapterItem>) :
    RecyclerView.Adapter<TopRecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_recyclerview_top, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        viewHolder.title.text = arrayList[position].title
        viewHolder.percent.text = arrayList[position].percent.toString() + "%"
        viewHolder.leftValue.text = arrayList[position].leftString
        ObjectAnimator.ofInt(viewHolder.progressBar, "progress", arrayList[position].percent.toInt())
            .setDuration(1500)
            .start()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val adapterItem: AdapterItem = payloads[0] as AdapterItem

            holder.percent.text = adapterItem.percent.toString() + "%"
            holder.progressBar.progress = adapterItem.percent.toInt()
            holder.leftValue.text = adapterItem.leftString
        }
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView
        val percent: TextView
        val progressBar: ProgressBar
        val leftValue: TextView

        //ViewHolder
        init {
            title = view.findViewById<View>(R.id.title) as TextView
            percent = view.findViewById<View>(R.id.percent_text) as TextView
            progressBar = view.findViewById<View>(R.id.progress) as ProgressBar
            leftValue = view.findViewById(R.id.left_day)
        }
    }
}