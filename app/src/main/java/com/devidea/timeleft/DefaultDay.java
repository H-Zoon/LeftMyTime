package com.devidea.timeleft;


import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

public class DefaultDay implements TimeInfo{

    @Override
    public AdapterItem setTimeItem() {
        AdapterItem adapterItem = new AdapterItem();

        int monthOfDay = LocalDate.now().getDayOfMonth();
        int lengthOfMonth = LocalDate.now().lengthOfMonth();
        String summery = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.KOREAN);

        float MonthPercent = (float) monthOfDay / lengthOfMonth * 100;

        adapterItem.setSummery(summery +"의 ");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", MonthPercent));

        adapterItem.setStartDay("시작일: " + summery + " 1일");
        adapterItem.setEndDay("종료일: " + summery +" "+ lengthOfMonth + "일");
        adapterItem.setLeftDay("남은일: " + (lengthOfMonth - monthOfDay) + "일");

        return adapterItem;
    }
}
