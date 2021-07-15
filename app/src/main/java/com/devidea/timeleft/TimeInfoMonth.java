package com.devidea.timeleft;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Locale;

public class TimeInfoMonth implements TimeInfo{

    @Override
    public AdapterItem setTimeItem() {
        AdapterItem adapterItem = new AdapterItem();

        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat format_month_day = new SimpleDateFormat("d", Locale.KOREA);
        int month_day = Integer.parseInt(format_month_day.format(date));
        LocalDate newDate = LocalDate.of(2021, 3, 1); //해당 달의 일수를 돌려받을때 사용합니다.
        int lengthOfMon = newDate.lengthOfMonth();
        float MonthPercent = (float) month_day / lengthOfMon * 100;

        adapterItem.setSummery("Month Left is");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", MonthPercent));

        return adapterItem;
    }
}
