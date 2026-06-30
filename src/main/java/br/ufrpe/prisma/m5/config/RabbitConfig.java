package br.ufrpe.prisma.m5.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_DENUNCIAS = "denuncias";
    public static final String EXCHANGE_DLX       = "denuncias.dlx";
    public static final String FILA_ROTEAMENTO    = "m5.roteamento";
    public static final String FILA_DLQ           = "m5.roteamento.dlq";
    public static final String ROUTING_KEY_IN     = "denuncia.priorizada";
    public static final String ROUTING_KEY_OUT    = "denuncia.encaminhada";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE_DENUNCIAS, true, false);
    }

    // Dead-letter exchange + fila — mensagens rejeitadas vão aqui em vez de desaparecer
    @Bean
    public TopicExchange dlx() {
        return new TopicExchange(EXCHANGE_DLX, true, false);
    }

    @Bean
    public Queue dlq() {
        return QueueBuilder.durable(FILA_DLQ).build();
    }

    @Bean
    public Binding dlqBinding(Queue dlq, TopicExchange dlx) {
        return BindingBuilder.bind(dlq).to(dlx).with("#");
    }

    // Fila própria do M5 com dead-letter configurado
    @Bean
    public Queue filaRoteamento() {
        return QueueBuilder.durable(FILA_ROTEAMENTO)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .build();
    }

    @Bean
    public Binding filaBinding(Queue filaRoteamento, TopicExchange topicExchange) {
        return BindingBuilder.bind(filaRoteamento).to(topicExchange).with(ROUTING_KEY_IN);
    }
}
