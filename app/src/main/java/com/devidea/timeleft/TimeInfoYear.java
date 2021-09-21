 package com.devidea.timeleft;


import java.time.LocalDate;
import java.util.Locale;

public class TimeInfoYear implements TimeInfo{

    @Override
    public AdapterItem setTimeItem() {
        AdapterItem adapterItem = new AdapterItem();

        float YearPercent =  ((float)LocalDate.now().getDayOfYear() / (float) LocalDate.now().lengthOfYear()) * 100;

        adapterItem.setSummery(LocalDate.now().getYear()+"년의 ");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", YearPercent));

        return adapterItem;
    }

}
