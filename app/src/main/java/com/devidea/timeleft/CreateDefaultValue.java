package com.devidea.timeleft;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

public class CreateDefaultValue implements TimeCalculator {

    @Override
    public AdapterItem setTimeItem() {
        AdapterItem adapterItem = new AdapterItem();

        LocalTime now = LocalTime.now();
        LocalTime endValue = LocalTime.parse("23:59:59");
        float secondsValue = Duration.between(LocalTime.of(0, 0), now).getSeconds();

        float TimePercent = (secondsValue / 86400) * 100;

        adapterItem.setSummery("오늘의 ");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", TimePercent));
        adapterItem.setStartDay("시작시간: 00:00");
        adapterItem.setEndDay("종료시간: 23:59");
        adapterItem.setLeftDay("남은시간: " + (LocalTime.ofSecondOfDay(Duration.between(now, endValue).getSeconds())));

        return adapterItem;
    }

    @Override
    public AdapterItem setYearItem() {
        AdapterItem adapterItem = new AdapterItem();

        float YearPercent = ((float) LocalDate.now().getDayOfYear() / (float) LocalDate.now().lengthOfYear()) * 100;

        adapterItem.setSummery(LocalDate.now().getYear() + "년의 ");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", YearPercent));

        adapterItem.setStartDay("시작일: " + LocalDate.now().getYear() + "년" + " 1월" + " 1일");
        adapterItem.setEndDay("종료일: " + LocalDate.now().getYear() + "년" + " 12월" + " 31일");
        adapterItem.setLeftDay("남은일: " + (LocalDate.now().lengthOfYear() - LocalDate.now().getDayOfYear()) + "일");

        return adapterItem;
    }

    @Override
    public AdapterItem setDayItem() {
        AdapterItem adapterItem = new AdapterItem();

        int monthOfDay = LocalDate.now().getDayOfMonth();
        int lengthOfMonth = LocalDate.now().lengthOfMonth();
        String summery = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.KOREAN);

        float MonthPercent = (float) monthOfDay / lengthOfMonth * 100;

        adapterItem.setSummery(summery + "의 ");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", MonthPercent));

        adapterItem.setStartDay("시작일: " + summery + " 1일");
        adapterItem.setEndDay("종료일: " + summery + " " + lengthOfMonth + "일");
        adapterItem.setLeftDay("남은일: " + (lengthOfMonth - monthOfDay) + "일");

        return adapterItem;
    }

}
