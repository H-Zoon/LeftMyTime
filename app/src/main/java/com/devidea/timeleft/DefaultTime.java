package com.devidea.timeleft;


import java.time.Duration;
import java.time.LocalTime;
import java.util.Locale;

public class DefaultTime implements TimeInfo{

    @Override
    public AdapterItem setTimeItem() {
        AdapterItem adapterItem = new AdapterItem();

        LocalTime now = LocalTime.now();
        LocalTime endValue = LocalTime.parse("23:59:59");
        float secondsValue = Duration.between(LocalTime.of(0,0), now).getSeconds();

        float TimePercent = (secondsValue/ 86400) * 100;

        adapterItem.setSummery("오늘의 ");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", TimePercent));
        adapterItem.setStartDay("시작시간: 00:00");
        adapterItem.setEndDay("종료시간: 23:59");
        adapterItem.setLeftDay("남은시간: " +(LocalTime.ofSecondOfDay(Duration.between(now, endValue).getSeconds())));

        return adapterItem;
    }


}
