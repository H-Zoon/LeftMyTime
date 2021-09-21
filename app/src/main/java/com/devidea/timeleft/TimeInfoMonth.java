package com.devidea.timeleft;


import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class TimeInfoMonth implements TimeInfo{

    @Override
    public AdapterItem setTimeItem() {
        AdapterItem adapterItem = new AdapterItem();

        int monthOfDay = LocalDate.now().getDayOfMonth();
        int lengthOfMonth = LocalDate.now().lengthOfMonth();

        float MonthPercent = (float) monthOfDay / lengthOfMonth * 100;

        adapterItem.setSummery(LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.KOREAN)+"의 ");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", MonthPercent));

        return adapterItem;
    }
}
