//package org.cms.chatbot_service.config;
//import org.springframework.amqp.core.*;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.amqp.support.converter.MessageConverter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RabbitConfig {
//
//    public static final String QUEUE_NAME = "ai_knowledge_queue";
//    public static final String EXCHANGE_NAME = "ai_exchange";
//    public static final String ROUTING_KEY = "content.update";
//
//    @Bean
//    public TopicExchange aiExchange() {
//        return new TopicExchange(EXCHANGE_NAME);
//    }
//
//    @Bean
//    public Queue aiQueue() {
//        return new Queue(QUEUE_NAME);
//    }
//
//    @Bean
//    public Binding binding(Queue queue, TopicExchange exchange) {
//        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
//    }
//
////    @Bean
////    public MessageConverter jsonMessageConverter() {
////        return new Jackson2JsonMessageConverter();
////    }
//@Bean
//public MessageConverter jsonMessageConverter() {
//    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
//
//    // ⚠️ CRITICAL FIX: "INFERRED" tells Jackson to look at the Method Argument type
//    // (PostEventDto) and force map the JSON to it, ignoring the sender's class path.
//    converter.setTypePrecedence(Jackson2JsonMessageConverter.TypePrecedence.INFERRED);
//
//    return converter;
//}
//}
//package org.cms.chatbot_service.config;
//
//import org.springframework.amqp.core.*;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter; // Ensure this import
//import org.springframework.amqp.support.converter.MessageConverter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RabbitConfig {
//
//    public static final String QUEUE_NAME = "ai_knowledge_queue";
//    public static final String EXCHANGE_NAME = "ai_exchange";
//    public static final String ROUTING_KEY = "content.update";
//
//    @Bean
//    public TopicExchange aiExchange() {
//        return new TopicExchange(EXCHANGE_NAME);
//    }
//
//    @Bean
//    public Queue aiQueue() {
//        return new Queue(QUEUE_NAME);
//    }
//
//    @Bean
//    public Binding binding(Queue queue, TopicExchange exchange) {
//        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
//    }
//
////    @Bean
////    public MessageConverter jsonMessageConverter() {
////        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
////
////        // FIX: Access TypePrecedence via the Class Name explicitly
////        converter.setTypePrecedence(Jackson2JsonMessageConverter.TypePrecedence.INFERRED);
////
////        // Security: Allow mapping even if packages are different (e.g. PostService vs ChatbotService)
////        converter.setTrustedPackages("*");
////
////        return converter;
////    }
//
//    @Bean
//    public MessageConverter jsonMessageConverter() {
//        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
//        // This nuclear option trusts everything and usually solves mismatch issues
//        converter.setTrustedPackages("*");
//        return converter;
//    }
//}

package org.cms.chatbot_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper; // Correct Import
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE_NAME = "ai_knowledge_queue";
    public static final String EXCHANGE_NAME = "ai_exchange";
    public static final String ROUTING_KEY = "content.update";

    @Bean
    public TopicExchange aiExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue aiQueue() {
        return new Queue(QUEUE_NAME);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();


        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);

        // NOTE: setTrustedPackages is removed as it's not needed with INFERRED

        return converter;
    }
}