package dev.enes.common.event;

public final class EventConstants {

    private EventConstants() {}

    public static final String EXCHANGE = "n11.exchange";
    public static final String ORDER_CREATED_QUEUE = "q.order.created";
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String PAYMENT_RESULT_QUEUE = "q.payment.result";
    public static final String PAYMENT_RESULT_KEY = "payment.result";
    public static final String STOCK_RESULT_QUEUE = "q.stock.result";
    public static final String STOCK_RESULT_KEY = "stock.result";
    public static final String NOTIFICATION_QUEUE = "q.notification";
    public static final String NOTIFICATION_KEY = "notification.#";
    public static final String ORDER_CONFIRMED_KEY = "notification.order.confirmed";
    public static final String ORDER_CANCELLED_KEY = "notification.order.cancelled";
}
