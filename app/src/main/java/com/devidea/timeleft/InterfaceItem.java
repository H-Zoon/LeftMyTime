package com.devidea.timeleft;

public interface InterfaceItem {
    AdapterItem timeItem();
    AdapterItem yearItem();
    AdapterItem monthItem();
    AdapterItem customTimeItem(EntityItemInfo itemInfo);
    AdapterItem customMonthItem(EntityItemInfo itemInfo);


}
