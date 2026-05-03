package dev.enes.inventory.config;

import dev.enes.common.event.EventConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EventConstants.EXCHANGE);
    }

    @Bean
    public Queue paymentResultQueue() {
        return QueueBuilder.durable(EventConstants.PAYMENT_RESULT_QUEUE).build();
    }

    @Bean
    public Binding paymentResultBinding(Queue paymentResultQueue, TopicExchange exchange) {
        return BindingBuilder.bind(paymentResultQueue).to(exchange).with(EventConstants.PAYMENT_RESULT_KEY);
    }

    @Bean
    public Queue stockResultQueue() {
        return QueueBuilder.durable(EventConstants.STOCK_RESULT_QUEUE).build();
    }

    @Bean
    public Binding stockResultBinding(Queue stockResultQueue, TopicExchange exchange) {
        return BindingBuilder.bind(stockResultQueue).to(exchange).with(EventConstants.STOCK_RESULT_KEY);
    }

    // Yeni ürün oluşturulduğunda stok kaydı otomatik açılması için kuyruk
    @Bean
    public Queue productCreatedQueue() {
        return QueueBuilder.durable("q.product.created").build();
    }

    @Bean
    public Binding productCreatedBinding(Queue productCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(productCreatedQueue).to(exchange).with("product.created");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
