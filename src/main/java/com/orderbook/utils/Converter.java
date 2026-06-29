package com.orderbook.utils;

import com.orderbook.entity.*;

public final class Converter {
    public static OrderOnBook toOrderOnBook(LimitOrder order) {
        return new OrderOnBook(order.generalOrderInfo(), order.price());
    }

    public static OrderOnBook toOrderOnBook(PostOnlyOrder order) {
        return new OrderOnBook(order.generalOrderInfo(), order.price());
    }

    public static IcebergOrderOnBook toOrderOnBook(IcebergOrder order) {
        return new IcebergOrderOnBook(order.id(), order.childOrder(), order.totalQuantity());
    }

    public static GeneralOrderInfo clone(GeneralOrderInfo orderInfo) {
        return new GeneralOrderInfo(IDGenerator.get(), orderInfo.type(), orderInfo.side(), orderInfo.quantity(), orderInfo.userId(), CommonUtil.now());
    }
}
