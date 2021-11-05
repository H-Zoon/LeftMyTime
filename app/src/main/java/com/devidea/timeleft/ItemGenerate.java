package com.devidea.timeleft;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static com.devidea.timeleft.MainActivity.appDatabase;

public class ItemGenerate implements InterfaceItem {

    @Override
    public AdapterItem timeItem() {
        AdapterItem adapterItem = new AdapterItem();

        LocalTime now = LocalTime.now();
        LocalTime endValue = LocalTime.parse("23:59:59");
        float secondsValue = Duration.between(LocalTime.of(0, 0), now).getSeconds();

        float TimePercent = (secondsValue / 86400) * 100;

        adapterItem.setSummery("오늘의 ");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", TimePercent));
        adapterItem.setLeftDay("남은시간: " + (LocalTime.ofSecondOfDay(Duration.between(now, endValue).getSeconds())));

        return adapterItem;
    }

    @Override
    public AdapterItem yearItem() {
        AdapterItem adapterItem = new AdapterItem();

        float YearPercent = ((float) LocalDate.now().getDayOfYear() / (float) LocalDate.now().lengthOfYear()) * 100;

        adapterItem.setSummery(LocalDate.now().getYear() + "년의 ");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", YearPercent));

        adapterItem.setLeftDay("남은일: " + (LocalDate.now().lengthOfYear() - LocalDate.now().getDayOfYear()) + "일");

        return adapterItem;
    }

    @Override
    public AdapterItem monthItem() {
        AdapterItem adapterItem = new AdapterItem();

        int monthOfDay = LocalDate.now().getDayOfMonth();
        int lengthOfMonth = LocalDate.now().lengthOfMonth();
        String summery = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.KOREAN);

        float MonthPercent = (float) monthOfDay / lengthOfMonth * 100;

        adapterItem.setSummery(summery + "의 ");
        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", MonthPercent));

        adapterItem.setLeftDay("남은일: " + (lengthOfMonth - monthOfDay) + "일");

        return adapterItem;
    }

    @Override
    public AdapterItem customTimeItem(EntityItemInfo itemInfo) {

        AdapterItem adapterItem = new AdapterItem();

        LocalTime startValue = LocalTime.parse(itemInfo.getStartValue()); //시작시간
        LocalTime endValue = LocalTime.parse(itemInfo.getEndValue()); // 종료시간
        LocalTime time = LocalTime.now(); //현재 시간

        adapterItem.setAutoUpdate(itemInfo.isAutoUpdate());
        adapterItem.setSummery(itemInfo.getSummery());
        adapterItem.setStartDay("설정시간: " + startValue);
        adapterItem.setEndDay("종료시간: " + endValue);
        adapterItem.setId(itemInfo.getId());

        if (time.isAfter(startValue) && time.isBefore(endValue)) {
            float range = Duration.between(startValue, endValue).getSeconds();
            float sendTime = Duration.between(startValue, time).getSeconds();

            adapterItem.setLeftDay("남은시간: " + (LocalTime.ofSecondOfDay(Duration.between(time, endValue).getSeconds())));

            float timePercent = (sendTime / range) * 100;

            adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", timePercent));

        } else {
            adapterItem.setPercentString("100");
            adapterItem.setLeftDay("설정시간이 지나면 계산해 드릴께요");
        }

        return adapterItem;

    }

    @Override
    public AdapterItem customMonthItem(EntityItemInfo itemInfo) {

        AdapterItem adapterItem = new AdapterItem();

        LocalDate startDate = LocalDate.parse(itemInfo.getStartValue());
        LocalDate endDate = LocalDate.parse(itemInfo.getEndValue());
        LocalDate today = LocalDate.now();
        int id = itemInfo.getId();

        //설정일
        int setDay = (int) ChronoUnit.DAYS.between(startDate, endDate);

        //설정일에서 지난일
        int sendDay = (int) ChronoUnit.DAYS.between(startDate, today);

        //설정일까지 남은일
        int leftDay = (int) ChronoUnit.DAYS.between(today, endDate);
        int lengthOfMonth = LocalDate.now().lengthOfMonth();

        //종료일로 넘어가고 자동 업데이트 체크한 경우
        if (today.isAfter(endDate) && itemInfo.isAutoUpdate()) {
            //시작일: 종료일, 종료일: 종료일 + 설정일수로 UPDATE
            appDatabase.DatabaseDao().updateItem(String.valueOf(endDate), String.valueOf(endDate.plusDays(lengthOfMonth)), itemInfo.getId());

            itemInfo = appDatabase.DatabaseDao().getSelectItem(id);

            startDate = LocalDate.parse(itemInfo.getStartValue());
            endDate = LocalDate.parse(itemInfo.getEndValue());

            setDay = (int) ChronoUnit.DAYS.between(startDate, endDate);
            sendDay = (int) ChronoUnit.DAYS.between(startDate, today);
            leftDay = (int) ChronoUnit.DAYS.between(today, endDate);
        }

        float MonthPercent = (float) sendDay / setDay * 100;

        adapterItem.setStartDay("설정일: " + startDate);
        adapterItem.setEndDay("종료일: " + endDate);
        adapterItem.setLeftDay("남은일: D-" + leftDay);
        adapterItem.setAutoUpdate(itemInfo.isAutoUpdate());
        adapterItem.setSummery(itemInfo.getSummery());
        adapterItem.setId(itemInfo.getId());

        if (MonthPercent < 100) {
            adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", MonthPercent));
        } else {
            adapterItem.setPercentString("100");
        }

        return adapterItem;
    }

}
