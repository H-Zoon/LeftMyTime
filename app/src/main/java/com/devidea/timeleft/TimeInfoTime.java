package com.devidea.timeleft;


import java.time.Duration;
import java.time.LocalTime;
import java.util.Locale;

public class TimeInfoTime implements TimeInfo{

    @Override
    public AdapterItem setTimeItem() {
        AdapterItem adapterItem = new AdapterItem();

        LocalTime now = LocalTime.now();
        float secondsValue = Duration.between(LocalTime.of(0,0), now).getSeconds();

        float TimePercent = (secondsValue/ 86400) * 100;

        adapterItem.setSummery("오늘의 ");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", TimePercent));

        return adapterItem;
    }


}
