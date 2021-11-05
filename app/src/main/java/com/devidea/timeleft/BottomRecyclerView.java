package com.devidea.timeleft;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import static com.devidea.timeleft.MainActivity.ITEM_GENERATE;
import static com.devidea.timeleft.MainActivity.appDatabase;

public class BottomRecyclerView extends androidx.recyclerview.widget.RecyclerView.Adapter<BottomRecyclerView.ViewHolder> {

    //array list
    private final ArrayList<AdapterItem> arrayList;
    // Item의 클릭 상태를 저장할 array 객체
    private SparseBooleanArray selectedItems;
    // 직전에 클릭됐던 Item의 position
    private static int prePosition = -1;
    //context
    private static Context context;

    //CustomAdapter 생성자
    public BottomRecyclerView(ArrayList<AdapterItem> arrayList) {
        this.arrayList = arrayList;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public BottomRecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        context = viewGroup.getContext();
        selectedItems = new SparseBooleanArray(getItemCount());
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_recyclerview_bottom, viewGroup, false);

        return new BottomRecyclerView.ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {

        viewHolder.startValue.setText(arrayList.get(position).getStartDay());
        viewHolder.endValue.setText(arrayList.get(position).getEndDay());
        viewHolder.leftValue.setText(arrayList.get(position).getLeftDay());
        if (arrayList.get(position).isAutoUpdate()) {
            viewHolder.autoUpdate.setText("이후 반복되는 일정이에요");
        } else {
            viewHolder.autoUpdate.setText("100% 달성후 끝나는 일정이에요");
        }

        viewHolder.startValue.setVisibility(View.GONE);
        viewHolder.endValue.setVisibility(View.GONE);
        viewHolder.leftValue.setVisibility(View.GONE);
        viewHolder.autoUpdate.setVisibility(View.GONE);
        viewHolder.deleteButton.setVisibility(View.GONE);

        viewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                builder.setMessage("정말 삭제할까요?");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        appDatabase.DatabaseDao().deleteItem(arrayList.get(position).getId());
                        appDatabase.DatabaseDao().deleteCustomWidget(arrayList.get(position).getId());

                        MainActivity.GetDBItem();
                        Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_LONG).show();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
                selectedItems.put(position, false);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();

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
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            for (Object payload : payloads) {
                int itemID = (int) payload;
                AdapterItem adapterItem = ITEM_GENERATE.customTimeItem(appDatabase.DatabaseDao().getSelectItem(itemID));
                holder.leftValue.setText(adapterItem.getLeftDay());
                holder.percent.setText(adapterItem.getPercentString() + "%");
                holder.progressBar.setProgress((int)Float.parseFloat(adapterItem.getPercentString()));


            }
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        private final TextView summery;
        private final TextView percent;
        private final ProgressBar progressBar;
        private final TextView startValue;
        private final TextView endValue;
        private final TextView leftValue;
        private final TextView autoUpdate;
        private final Button deleteButton, imageButton;

        //ViewHolder
        public ViewHolder(View view) {
            super(view);
            summery = view.findViewById(R.id.summery);
            percent = view.findViewById(R.id.percent_text);
            progressBar = view.findViewById(R.id.progress);
            startValue = view.findViewById(R.id.start_day);
            endValue = view.findViewById(R.id.end_day);
            autoUpdate = view.findViewById(R.id.update_is);
            leftValue = view.findViewById(R.id.left_day);
            imageButton = view.findViewById(R.id.imageButton);

            deleteButton = view.findViewById(R.id.delete_button);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();

                    if (selectedItems.get(getAdapterPosition())) {
                        // 펼쳐진 Item을 클릭 시
                        selectedItems.put(getAdapterPosition(), false);
                    } else {
                        // 직전의 클릭됐던 Item의 클릭상태를 지움
                        selectedItems.put(pos, false);
                        prePosition = pos;
                        // 클릭한 Item의 position을 저장
                        selectedItems.put(getAdapterPosition(), true);
                    }
                    changeVisibility(selectedItems.get(getAdapterPosition()));
                }
            });
        }


        private void changeVisibility(final boolean isExpanded) {
            // height 값을 dp로 지정
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
                    // imageView의 높이 변경
                    itemView.findViewById(R.id.view).getLayoutParams().height = (int) animation.getAnimatedValue();
                    itemView.findViewById(R.id.view).requestLayout();
                    // imageView가 실제로 사라지게하는 부분
                    startValue.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                    endValue.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                    leftValue.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                    autoUpdate.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                    deleteButton.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                    imageButton.setBackgroundResource(isExpanded ? R.drawable.baseline_expand_less_black_36 : R.drawable.baseline_expand_more_black_36);
                }
            });
            // Animation start
            va.start();
        }
    }
}