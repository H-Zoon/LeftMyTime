package com.devidea.timeleft;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeInfoDttm implements TimeInfo{

    @Override
    public AdapterItem setTimeItem() {
        AdapterItem adapterItem = new AdapterItem();
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat format_hour = new SimpleDateFormat("H", Locale.KOREA);
        SimpleDateFormat format_min = new SimpleDateFormat("m", Locale.KOREA);
        SimpleDateFormat format_sec = new SimpleDateFormat("s", Locale.KOREA);
        int hour = Integer.parseInt(format_hour.format(date));
        int min = Integer.parseInt(format_min.format(date));
        int sec = Integer.parseInt(format_sec.format(date));

        float TimePercent = ((((float) hour * 3600) + (min * 60) + sec) / 86400) * 100;

        adapterItem.setSummery("Time Left is");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", TimePercent));

        return adapterItem;
    }
}
