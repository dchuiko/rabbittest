package org.dchuiko.rabbittest.config;

import org.aopalliance.intercept.Interceptor;
import org.dchuiko.rabbittest.rabbit.RabbitSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.StatefulRetryOperationsInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableTransactionManagement
public class RabbitConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    public final static String slowQueueName = "rt-slow-queue";
    public final static String errorQueueName = "rt-error-queue";
    public final static String transactionalQueueName = "rt-transactional-queue";
    public final static String errorSecondStepQueueName = "rt-error-second-step-queue";

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            PlatformTransactionManager platformTransactionManager)  {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory() {
            @Override
            protected SimpleMessageListenerContainer createContainerInstance() {
                SimpleMessageListenerContainer result = super.createContainerInstance();
                // need to set this property to false in order to
                // force amqp framework to respect retry policy
                // in case of rollback
                result.setAlwaysRequeueWithTxManagerRollback(false);
                return result;
            }
        };
        factory.setConnectionFactory(connectionFactory);
//        factory.setRecoveryBackOff(listenerBackOff());
//        factory.setMessageConverter(messageConverter);
        factory.setTransactionManager(platformTransactionManager);
        factory.setChannelTransacted(true);
        factory.setAdviceChain(statefulRetryInterceptor());

        // number of threads for RabbitListener
        factory.setConcurrentConsumers(10);
        return factory;
    }

    /**
     * For stateless interceptor works only initial
     * backoff interval and maxAttempts
     */
    @Bean
    public Interceptor statelessRetryInterceptor() {
        return RetryInterceptorBuilder.stateless().maxAttempts(3)
                                      .backOffOptions(
                                              TimeUnit.SECONDS.toMillis(3),
                                              2,
                                              TimeUnit.HOURS.toMillis(1)
                                      )
                                      .recoverer(messageRecoverer())
                                      .build();
    }

    /**
     * Multiplier for backoff options works only with stateful interceptor
     * To make stateful interceptor works you should set messageId during
     * sending a message:
     * @see RabbitSender.RtMessagePostProcessor
     */
    @Bean
    public StatefulRetryOperationsInterceptor statefulRetryInterceptor() {
        return RetryInterceptorBuilder.stateful()
                                      .maxAttempts(3)
                                      .backOffOptions(
                                              TimeUnit.SECONDS.toMillis(3),
                                              2.0,
                                              TimeUnit.HOURS.toMillis(1)
                                      )
                                      .recoverer(messageRecoverer())
                                      .build();
    }

    private MessageRecoverer messageRecoverer() {
        return (args, cause) -> {
            final String msg = "Retry Policy Exhausted";
            log.error(msg, cause);
            throw new AmqpRejectAndDontRequeueException(msg, cause);
        };
    }

    @Bean
    Queue rtSlowQueue() {
        return new Queue(slowQueueName, false);
    }

    @Bean
    Binding rtSlowBinding(@Qualifier("rtSlowQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(slowQueueName);
    }

    @Bean
    Queue rtErrorQueue() {
        return new Queue(errorQueueName, false);
    }

    @Bean
    Binding rtErrorBinding(@Qualifier("rtErrorQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(errorQueueName);
    }

    @Bean
    Queue rtTransactionalQueue() {
        return new Queue(transactionalQueueName, false);
    }

    @Bean
    Binding rtTransactionalBinding(@Qualifier("rtTransactionalQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(transactionalQueueName);
    }

    @Bean
    Queue rtErrorSecondStepQueue() {
        return new Queue(errorSecondStepQueueName, false);
    }

    @Bean
    Binding rtErrorSecondStepBinding(@Qualifier("rtErrorSecondStepQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(errorSecondStepQueueName);
    }

    @Bean
    TopicExchange rtExchange() {
        return new TopicExchange("rt-spring-boot-exchange");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(Exchange rtExchange,
                                         ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange(rtExchange.getName());
        template.setChannelTransacted(true);
        return template;
    }


}
