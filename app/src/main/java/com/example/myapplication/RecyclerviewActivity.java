package com.example.myapplication;


import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    //array list
    private final ArrayList<ItemList> arrayList;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView summery;
        private final TextView percent;
        private final ProgressBar progressBar;

        //ViewHolder
        public ViewHolder(View view) {
            super(view);

            summery = (TextView) view.findViewById(R.id.summery);
            percent = (TextView) view.findViewById(R.id.percent_text);
            progressBar = (ProgressBar) view.findViewById(R.id.progress);
        }

    }

    //CustomAdapter 생성자
    public CustomAdapter(ArrayList<ItemList> arrayList) {
        this.arrayList = arrayList;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_recyclerview, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {

        String summery_buf = arrayList.get(position).getSummery();
        viewHolder.summery.setText(summery_buf);

        String percent_buf = arrayList.get(position).getPercent_string();
        viewHolder.percent.setText(percent_buf + "%");

        int percent = (int)Float.parseFloat(percent_buf);

        ObjectAnimator.ofInt(viewHolder.progressBar, "progress", percent)
                .setDuration(1500)
                .start();

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(viewHolder, position, payloads);
        } else {
            for (Object payload : payloads) {
                if (payload instanceof String) {
                    String type = (String) payload;
                    viewHolder.percent.setText(type + "%");
                    int percent = (int)Float.parseFloat(type);
                    viewHolder.progressBar.setProgress(percent);
                }
            }
        }
    }

    @Override
    public int getItemCount () {
        return arrayList.size();
    }

}






