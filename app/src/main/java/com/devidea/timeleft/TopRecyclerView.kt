package com.devidea.timeleft

import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.animation.ObjectAnimator
import android.widget.ProgressBar
import android.view.View
import java.util.ArrayList

internal class TopRecyclerView
constructor(private val arrayList: ArrayList<AdapterItem?>) :
    RecyclerView.Adapter<TopRecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_recyclerview_top, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        viewHolder.summery.text = arrayList[position]!!.summery
        val percent_buf: String? = arrayList[position]!!.percentString
        viewHolder.percent.text = percent_buf + "%"
        viewHolder.leftValue.text = arrayList[position]!!.leftDay
        val percent: Int = percent_buf!!.toFloat().toInt()
        ObjectAnimator.ofInt(viewHolder.progressBar, "progress", percent)
            .setDuration(1500)
            .start()


    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val adapterItem: AdapterItem = payloads[0] as AdapterItem

            holder.percent.text = adapterItem.percentString + "%"
            holder.progressBar.progress = adapterItem.percentString!!.toFloat().toInt()
            holder.leftValue.text = adapterItem.leftDay
        }
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        val summery: TextView
        val percent: TextView
        val progressBar: ProgressBar
        val leftValue: TextView

        //ViewHolder
        init {
            summery = view.findViewById<View>(R.id.summery) as TextView
            percent = view.findViewById<View>(R.id.percent_text) as TextView
            progressBar = view.findViewById<View>(R.id.progress) as ProgressBar
            leftValue = view.findViewById(R.id.left_day)
        }
    }
}