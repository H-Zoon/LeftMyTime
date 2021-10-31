 package com.devidea.timeleft;


import java.time.LocalDate;
import java.util.Locale;

public class DefaultYear implements TimeInfo{

    @Override
    public AdapterItem setTimeItem() {
        AdapterItem adapterItem = new AdapterItem();

        float YearPercent =  ((float)LocalDate.now().getDayOfYear() / (float) LocalDate.now().lengthOfYear()) * 100;

        adapterItem.setSummery(LocalDate.now().getYear()+"년의 ");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", YearPercent));

        adapterItem.setStartDay("시작일: " + LocalDate.now().getYear()+"년" +" 1월"+" 1일");
        adapterItem.setEndDay("종료일: " +  LocalDate.now().getYear()+"년" +" 12월"+" 31일");
        adapterItem.setLeftDay("남은일: " + (LocalDate.now().lengthOfYear() - LocalDate.now().getDayOfYear()) + "일");

        return adapterItem;
    }

}
