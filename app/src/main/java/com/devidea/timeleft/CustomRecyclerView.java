package com.devidea.timeleft;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import static com.devidea.timeleft.MainActivity.appDatabase;

public class CustomRecyclerView extends androidx.recyclerview.widget.RecyclerView.Adapter<CustomRecyclerView.ViewHolder> {

    //array list
    private final ArrayList<AdapterItem> arrayList;
    // Item의 클릭 상태를 저장할 array 객체
    private static SparseBooleanArray selectedItems = new SparseBooleanArray();
    // 직전에 클릭됐던 Item의 position
    private static int prePosition = -1;
    //context
    private static Context context;

    //CustomAdapter 생성자
    public CustomRecyclerView(ArrayList<AdapterItem> arrayList) {
        this.arrayList = arrayList;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public CustomRecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        context = viewGroup.getContext();
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_recyclerview_custom, viewGroup, false);

        return new CustomRecyclerView.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {

        viewHolder.startDay.setText("설정일: " + arrayList.get(position).getStartDay());
        viewHolder.endDay.setText("종료일: " + arrayList.get(position).getEndDay());
        viewHolder.leftDay.setText("남은일: D-" + arrayList.get(position).getLeftDay());

        viewHolder.startDay.setVisibility(View.GONE);
        viewHolder.endDay.setVisibility(View.GONE);
        viewHolder.leftDay.setVisibility(View.GONE);
        viewHolder.deleteButton.setVisibility(View.GONE);

        viewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appDatabase.DatabaseDao().deleteItem(arrayList.get(position).getId());
                appDatabase.DatabaseDao().delete_s(String.valueOf(arrayList.get(position).getId()));
                MainActivity.refreshItem();
                Log.d("deldte", String.valueOf(arrayList.get(position).getId()));
            }
        });

        String summery_buf = arrayList.get(position).getSummery();
        viewHolder.summery.setText(summery_buf);

        String percent_buf = arrayList.get(position).getPercentString();
        viewHolder.percent.setText(percent_buf + "%");

        int percent = (int) Float.parseFloat(percent_buf);

        ObjectAnimator.ofInt(viewHolder.progressBar, "progress", percent)
                .setDuration(1500)
                .start();

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        private final TextView summery;
        private final TextView percent;
        private final ProgressBar progressBar;
        private final TextView startDay;
        private final TextView endDay;
        private final TextView leftDay;
        private final Button deleteButton;

        //ViewHolder
        public ViewHolder(View view) {
            super(view);
            summery = (TextView) view.findViewById(R.id.summery);
            percent = (TextView) view.findViewById(R.id.percent_text);
            progressBar = (ProgressBar) view.findViewById(R.id.progress);
            startDay = view.findViewById(R.id.start_day);
            endDay = view.findViewById(R.id.end_day);
            leftDay = view.findViewById(R.id.left_day);
            deleteButton = view.findViewById(R.id.delete_button);

            itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick (View v){
                    int pos = getAdapterPosition();

                    if (selectedItems.get(getAdapterPosition())) {
                        // 펼쳐진 Item을 클릭 시
                        selectedItems.delete(getAdapterPosition());
                    } else {
                        // 직전의 클릭됐던 Item의 클릭상태를 지움
                        selectedItems.delete(prePosition);
                        // 클릭한 Item의 position을 저장
                        selectedItems.put(getAdapterPosition(), true);
                    }
                    changeVisibility(selectedItems.get(getAdapterPosition()));
                    Log.d("start", "start");
                }
            });
        }



        private void changeVisibility(final boolean isExpanded) {
            // height 값을 dp로 지정해서 넣고싶으면 아래 소스를 이용
            int dpValue = 200;
            float d = context.getResources().getDisplayMetrics().density;
            int height = (int) (dpValue * d);

            // ValueAnimator.ofInt(int... values)는 View가 변할 값을 지정, 인자는 int 배열
            ValueAnimator va = isExpanded ? ValueAnimator.ofInt(0, height) : ValueAnimator.ofInt(height, 0);
            // Animation이 실행되는 시간, n/1000초
            va.setDuration(600);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    // value는 height 값
                    int value = (int) animation.getAnimatedValue();
                    // imageView의 높이 변경
                    itemView.findViewById(R.id.view).getLayoutParams().height = value;
                    itemView.findViewById(R.id.view).requestLayout();
                    // imageView가 실제로 사라지게하는 부분
                    startDay.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                    endDay.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                    leftDay.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                    deleteButton.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                }
            });
            // Animation start
            va.start();
        }
    }
}