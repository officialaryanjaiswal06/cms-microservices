package com.cms.notification_service.config;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
public class RabbitMQConfig {
    public static final String QUEUE = "otp_queue";
    public static final String EXCHANGE = "otp_exchange";
    public static final String ROUTING_KEY = "otp_routing_key";

    // Define Queue
    @Bean
    public Queue queue() {
        return new Queue(QUEUE);
    }

    // Define Exchange
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    // Bind Queue to Exchange
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    // Handle JSON conversion automatically
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }
}
