package org.cms.post_service.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitSenderConfig {

    @Bean
    public MessageConverter producerConverter() {
        // Just return the standard JSON converter.
        // It sends the data as JSON. The "filtering" happens on the Receiver side.
        return new Jackson2JsonMessageConverter();
    }
}