package dev.enes.order.entity;

public enum OrderStatus {
    PENDING,
    PAYMENT_PROCESSING,
    PAYMENT_COMPLETED,
    STOCK_RESERVING,
    CONFIRMED,
    CANCELLED,
    DELIVERED
}
