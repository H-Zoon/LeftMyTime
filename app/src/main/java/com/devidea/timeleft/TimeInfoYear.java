package com.devidea.timeleft;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeInfoYear implements TimeInfo{

    @Override
    public AdapterItem setTimeItem() {
        AdapterItem adapterItem = new AdapterItem();

        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat format_Day = new SimpleDateFormat("D", Locale.KOREA);
        int day = Integer.parseInt(format_Day.format(date));
        float YearPercent = ((float) day / 365) * 100;

        adapterItem.setSummery("Year Left is");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", YearPercent));

        return adapterItem;
    }


}
