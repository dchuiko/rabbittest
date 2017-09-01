package org.dchuiko.rabbittest.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class RabbitAdminConfig {

    public RabbitAdmin rabbitAdmin() {
        ConnectionFactory connectionFactory = new CachingConnectionFactory();

        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        Queue queue = new Queue("rt-admin-queue");
        admin.declareQueue(queue);

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.convertAndSend("myqueue", "foo");
        String foo = (String) template.receiveAndConvert("myqueue");

        return admin;
    }
}
