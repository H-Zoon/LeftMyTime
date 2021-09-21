package com.devidea.timeleft;

public class AdapterItem {
    private String summery;
    private String percent;
    private String startDay;
    private String endDay;
    private String leftDay;
    private boolean autoUpdate;

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    private int id;

    public String getStartDay() {
        return startDay;
    }

    public void setStartDay(String startDay) {
        this.startDay = startDay;
    }

    public String getEndDay() {
        return endDay;
    }

    public void setEndDay(String endDay) {
        this.endDay = endDay;
    }

    public String getLeftDay() {
        return leftDay;
    }

    public void setLeftDay(String leftDay) {
        this.leftDay = leftDay;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSummery(String summery) {
        this.summery = summery;
    }

    public void setPercentString(String percent) {
        this.percent = percent;
    }

    public String getSummery() {
        return summery;
    }

    public String getPercentString() {
        return percent;
    }

}
