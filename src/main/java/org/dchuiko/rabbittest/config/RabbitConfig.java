package org.dchuiko.rabbittest.config;

import org.aopalliance.intercept.Interceptor;
import org.dchuiko.rabbittest.rabbit.RabbitSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.StatefulRetryOperationsInterceptor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableTransactionManagement
public class RabbitConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    public final static String slowQueueName = "rt-slow-queue";
    public final static String errorQueueName = "rt-error-queue";
    public final static String transactionalQueueName = "rt-transactional-queue";
    public final static String rpcQueueName = "rpc-queue";
    public final static String rpcReplyQueueName = "rpc-reply-queue";
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
//        factory.setAdviceChain(statefulRetryInterceptor());

        // number of threads for RabbitListener
        factory.setConcurrentConsumers(10);
        return factory;
    }


     // A ChainedTransactionManager needed to commit or rollback transactions in
     // postgres and rabbitmq during one call of PlatformTransactionManager.{commit(), rollback()}
     // This tx manager is not a XA tx manager. So it's possible situation when transaction committed
     // in database and rolled back in rabbitmq. In this case a HeuristicCompletionException will raised.
     @Bean
     public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory, ConnectionFactory connectionFactory) {
         ChainedTransactionManager c = new ChainedTransactionManager(
                 new RabbitTransactionManager(connectionFactory), new JpaTransactionManager(entityManagerFactory)
         );
         return c;
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
    DirectExchange rtDirectExchange() {
        return new DirectExchange("rt-direct-spring-boot-exchange");
    }


    @Bean
    Queue rpcQueue() {
        return new Queue(rpcQueueName);
    }

    @Bean
    Binding rpcQueueBinding(@Qualifier("rpcQueue") Queue queue, @Qualifier("rtDirectExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(rpcQueueName);
    }

    @Bean
    Queue rpcReplyQueue() {
        return new Queue(rpcReplyQueueName);
    }

    @Bean
    Binding rpcReplyQueueBinding(@Qualifier("rpcReplyQueue") Queue queue, @Qualifier("rtDirectExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(rpcReplyQueueName);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(Exchange rtExchange,
                                         ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange(rtExchange.getName());
        template.setChannelTransacted(true);
        RetryTemplate rt = new RetryTemplate();
        rt.setBackOffPolicy(new ExponentialBackOffPolicy());
        template.setRetryTemplate(rt);
        return template;
    }

    @Bean
    public RabbitTemplate rpcRabbitTemplate(@Qualifier("rtDirectExchange") DirectExchange rtExchange,
                                            Queue rpcQueue,
                                            ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange(rtExchange.getName());
        template.setRoutingKey(rpcQueue.getName());
        template.setChannelTransacted(false);
        return template;
    }


}
