package com.devidea.timeleft;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

import static com.devidea.timeleft.MainActivity.appDatabase;

public class ItemGenerator {

    public void saveMonthItem(String summery, int end, boolean autoUpdate) {

        EntityItemInfo entityItemInfo = new EntityItemInfo("month", String.valueOf(LocalDate.now()), String.valueOf(LocalDate.now().plusDays(end)), summery, autoUpdate);
        appDatabase.DatabaseDao().saveItem(entityItemInfo);

    }

    public void saveTimeItem(String summery, LocalTime startValue, LocalTime endValue, boolean autoUpdate) {

        EntityItemInfo entityItemInfo = new EntityItemInfo("time", String.valueOf(startValue), String.valueOf(endValue), summery, autoUpdate);
        appDatabase.DatabaseDao().saveItem(entityItemInfo);

    }

    public AdapterItem generateTimeItem(EntityItemInfo itemInfo) {

        AdapterItem adapterItem = new AdapterItem();

        LocalTime startValue = LocalTime.parse(itemInfo.getStartValue()); //시작시간
        LocalTime endValue = LocalTime.parse(itemInfo.getEndValue()); // 종료시간
        LocalTime time = LocalTime.now(); //현재 시간

        adapterItem.setAutoUpdate(itemInfo.isAutoUpdate());
        adapterItem.setSummery(itemInfo.getSummery());
        adapterItem.setStartDay("설정시간: " + startValue);
        adapterItem.setEndDay("종료시간: " + endValue);
        adapterItem.setId(itemInfo.getId());

        if (time.isAfter(startValue) && time.isBefore(endValue)) {
            float range = Duration.between(startValue, endValue).getSeconds();
            float sendTime = Duration.between(startValue, time).getSeconds();

            adapterItem.setLeftDay("남은시간: " + (LocalTime.ofSecondOfDay(Duration.between(time, endValue).getSeconds())));

            float timePercent = (sendTime / range) * 100;

            adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", timePercent));

        } else {
            adapterItem.setPercentString("100");
            adapterItem.setLeftDay("설정시간이 지나면 계산해 드릴께요");
        }

        return adapterItem;

    }


    public AdapterItem generateItem(EntityItemInfo itemInfo) {

        AdapterItem adapterItem = new AdapterItem();

        LocalDate startDate = LocalDate.parse(itemInfo.getStartValue());
        LocalDate endDate = LocalDate.parse(itemInfo.getEndValue());
        LocalDate today = LocalDate.now();

        //설정일
        int setDay = endDate.getDayOfMonth() - startDate.getDayOfMonth();
        //설정일에서 지난일
        int sendDay = today.getDayOfMonth() - startDate.getDayOfMonth();
        //설정일까지 남은일
        int leftDay = endDate.getDayOfMonth() - today.getDayOfMonth();


        //종료일로 넘어가고 자동 업데이트 체크한 경우
        if (today.isAfter(endDate) && itemInfo.isAutoUpdate()) {
            //시작일: 종료일, 종료일: 종료일 + 설정일수로 UPDATE
            appDatabase.DatabaseDao().updateItem(String.valueOf(endDate), String.valueOf(endDate.plusDays(setDay)), itemInfo.getId());
        }

        float MonthPercent = (float) sendDay / setDay * 100;

        adapterItem.setStartDay("설정일: " + startDate);
        adapterItem.setEndDay("종료일: " + endDate);
        adapterItem.setLeftDay("남은일: D-" + leftDay);
        adapterItem.setAutoUpdate(itemInfo.isAutoUpdate());
        adapterItem.setSummery(itemInfo.getSummery());
        adapterItem.setId(itemInfo.getId());

        if(MonthPercent<100){
            adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", MonthPercent));
        }
        else {
            adapterItem.setPercentString("100");
        }

        return adapterItem;
    }


}
