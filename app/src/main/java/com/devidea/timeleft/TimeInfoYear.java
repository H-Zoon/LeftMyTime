 package com.devidea.timeleft;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Date;
import java.util.Locale;

public class TimeInfoYear implements TimeInfo{

    @Override
    public AdapterItem setTimeItem() {
        AdapterItem adapterItem = new AdapterItem();

        YearMonth yearMonth = YearMonth.from(LocalDate.now());
        Year year = Year.from(LocalDate.now());

        int lengthOfYear = yearMonth.lengthOfYear(); //해당 년의 총 일수

        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat format_Day = new SimpleDateFormat("D", Locale.KOREA);
        int day = Integer.parseInt(format_Day.format(date));
        float YearPercent = ((float) day / lengthOfYear) * 100;

        adapterItem.setSummery(year+"년이 벌써..");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", YearPercent));

        return adapterItem;
    }

}
