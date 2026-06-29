package com.orderbook.entity;

import com.orderbook.utils.CommonUtil;
import com.orderbook.utils.IDGenerator;

import java.util.UUID;

public final class IcebergOrderOnBook {
    private final UUID id;
    private LimitOrder childOrder;
    private long remainingQuantity;

    public IcebergOrderOnBook(UUID id, LimitOrder childOrder, long remainingQuantity) {
        this.id = id;
        this.childOrder = childOrder;
        this.remainingQuantity = remainingQuantity;
    }

    public LimitOrder replenish() {
        remainingQuantity -= childOrder.generalOrderInfo()
                .quantity();
        long quantity = Math.min(remainingQuantity, childOrder.generalOrderInfo()
                .quantity());
        GeneralOrderInfo childInfo = childOrder.generalOrderInfo();
        GeneralOrderInfo newInfo = new GeneralOrderInfo(IDGenerator.get(), childInfo.type(), childInfo.side(), quantity, childInfo.userId(), CommonUtil.now());
        childOrder = new LimitOrder(newInfo, childOrder.price());
        return childOrder;
    }

    public UUID getId() {
        return id;
    }

    public LimitOrder getChildOrder() {
        return childOrder;
    }

    public void setChildOrder(LimitOrder childOrder) {
        this.childOrder = childOrder;
    }

    public long getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(long remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }
}
