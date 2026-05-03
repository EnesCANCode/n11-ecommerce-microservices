package dev.enes.order.config;

import dev.enes.common.event.EventConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class OrderConfig {

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EventConstants.EXCHANGE);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(EventConstants.ORDER_CREATED_QUEUE).build();
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(exchange).with(EventConstants.ORDER_CREATED_KEY);
    }

    @Bean
    public Queue paymentResultOrderQueue() {
        return QueueBuilder.durable(EventConstants.PAYMENT_RESULT_QUEUE + ".order").build();
    }

    @Bean
    public Binding paymentResultOrderBinding(Queue paymentResultOrderQueue, TopicExchange exchange) {
        return BindingBuilder.bind(paymentResultOrderQueue).to(exchange).with(EventConstants.PAYMENT_RESULT_KEY);
    }

    @Bean
    public Queue stockResultOrderQueue() {
        return QueueBuilder.durable(EventConstants.STOCK_RESULT_QUEUE + ".order").build();
    }

    @Bean
    public Binding stockResultOrderBinding(Queue stockResultOrderQueue, TopicExchange exchange) {
        return BindingBuilder.bind(stockResultOrderQueue).to(exchange).with(EventConstants.STOCK_RESULT_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }
}
