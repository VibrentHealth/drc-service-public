package com.vibrent.drc.enumeration;

public enum DRCSupplyMessageStatusType {
    CREATED(0),
    FULFILLMENT(1),
    SHIPPED(2),
    PARTICIPANT_SHIPPED(3),
    PARTICIPANT_DELIVERY(4),
    BIOBANK_SHIPPED(5),
    BIOBANK_DELIVERY(6),
    COMPLETED(7),
    CANCELLED(8);

    private Integer statusOrder;

    DRCSupplyMessageStatusType(int statusOrder) {
        this.statusOrder = statusOrder;
    }

    public boolean isEqualOrBefore(DRCSupplyMessageStatusType other) {
        return this.statusOrder <= other.statusOrder;
    }

}
