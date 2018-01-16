package org.dchuiko.rabbittest.rabbit;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RabbitSender {
    private final RabbitTemplate rabbitTemplate;
    private final RabbitTemplate rpcRabbitTemplate;

    @Autowired
    public RabbitSender(RabbitTemplate rabbitTemplate,
                        RabbitTemplate rpcRabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rpcRabbitTemplate = rpcRabbitTemplate;
    }

    public void send(String queueName, String msg) {
        rabbitTemplate.convertAndSend(queueName, (Object) msg, MESSAGE_POST_PROCESSOR);
    }

    @Transactional
    public String sendAndReceive(String queueName, String msg) {
        Message message = rpcRabbitTemplate.getMessageConverter().toMessage(msg, new MessageProperties());

        Message result = rpcRabbitTemplate.sendAndReceive(message);
        return (String) rpcRabbitTemplate.getMessageConverter().fromMessage(result);
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
