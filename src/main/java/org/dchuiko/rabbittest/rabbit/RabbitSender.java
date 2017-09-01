package org.dchuiko.rabbittest.rabbit;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RabbitSender {
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public RabbitSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String queueName, String msg) {
        rabbitTemplate.convertAndSend(queueName, (Object) msg, MESSAGE_POST_PROCESSOR);
    }

    private static final class RtMessagePostProcessor implements MessagePostProcessor {
        @Override
        public Message postProcessMessage(Message message) throws AmqpException {
            message.getMessageProperties().setMessageId(UUID.randomUUID().toString());
            return message;
        }
    }

    private static final RtMessagePostProcessor MESSAGE_POST_PROCESSOR = new RtMessagePostProcessor();

}
