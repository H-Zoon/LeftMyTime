package com.devidea.timeleft;


import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import static com.devidea.timeleft.MainActivity.ITEM_GENERATE;

class TopRecyclerView extends androidx.recyclerview.widget.RecyclerView.Adapter<TopRecyclerView.ViewHolder> {

    //array list
    private final ArrayList<AdapterItem> arrayList;

    //CustomAdapter 생성자
    public TopRecyclerView(ArrayList<AdapterItem> arrayList) {
        this.arrayList = arrayList;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_recyclerview_top, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {

        //String summery_buf = arrayList.get(position).getSummery();
        viewHolder.summery.setText(arrayList.get(position).getSummery());

        String percent_buf = arrayList.get(position).getPercentString();
        viewHolder.percent.setText(percent_buf + "%");

        viewHolder.startValue.setText(arrayList.get(position).getStartDay());
        viewHolder.endValue.setText(arrayList.get(position).getEndDay());
        viewHolder.leftValue.setText(arrayList.get(position).getLeftDay());

        int percent = (int) Float.parseFloat(percent_buf);

        ObjectAnimator.ofInt(viewHolder.progressBar, "progress", percent)
                .setDuration(1500)
                .start();

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            AdapterItem adapterItem = ITEM_GENERATE.timeItem();
            holder.percent.setText(adapterItem.getPercentString() + "%");
            holder.progressBar.setProgress((int) Float.parseFloat(adapterItem.getPercentString()));
            holder.leftValue.setText(adapterItem.getLeftDay());
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        private final TextView summery;
        private final TextView percent;
        private final ProgressBar progressBar;
        private final TextView startValue;
        private final TextView endValue;
        private final TextView leftValue;

        //ViewHolder
        public ViewHolder(View view) {
            super(view);
            summery = (TextView) view.findViewById(R.id.summery);
            percent = (TextView) view.findViewById(R.id.percent_text);
            progressBar = (ProgressBar) view.findViewById(R.id.progress);

            startValue = view.findViewById(R.id.start_day);
            endValue = view.findViewById(R.id.end_day);
            leftValue = view.findViewById(R.id.left_day);

        }
    }
}






