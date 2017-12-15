package org.dchuiko.rabbittest.rabbit;

import org.dchuiko.rabbittest.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class TransactionalSender {
    private final Logger log = LoggerFactory.getLogger(TransactionalSender.class);

    private final RabbitSender rabbitSender;

    public TransactionalSender(RabbitSender rabbitSender) {
        this.rabbitSender = rabbitSender;
    }

    @Transactional
    public void sendTransactional() {
        log.warn("Sending Transactional Error message");
        rabbitSender.send(RabbitConfig.transactionalQueueName, "Hello Transactional Error !");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        if (true) {
//            throw new RuntimeException();
//        }
    }


}
