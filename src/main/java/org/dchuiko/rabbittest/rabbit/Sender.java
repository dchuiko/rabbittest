package org.dchuiko.rabbittest.rabbit;

import org.dchuiko.rabbittest.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class Sender implements CommandLineRunner {
    private final Logger log = LoggerFactory.getLogger(Sender.class);

    private final RabbitSender rabbitSender;
    private final TransactionalSender transactionalSender;
    private final ConfigurableApplicationContext context;
    private final Receiver receiver;

    public Sender(Receiver receiver,
                  RabbitSender rabbitSender,
                  TransactionalSender transactionalSender,
                  ConfigurableApplicationContext context) {
        this.receiver = receiver;
        this.rabbitSender = rabbitSender;
        this.transactionalSender = transactionalSender;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        send();
//        try {
//            transactionalSender.sendTransactional();
//        } catch (Exception e) {
//            // ignore
//        }
        receiver.getLatch().await(20000, TimeUnit.MILLISECONDS);
        context.close();
    }

    private void send() {
        for (int i = 0; i < receiver.getSlowCount(); i++) {
            log.warn("Sending Slow message " + i + "...");
            rabbitSender.send(RabbitConfig.slowQueueName, "Hello Slow #" + i + "!");
        }

        for (int i = 0; i < receiver.getErrorCount(); i++) {
            log.warn("Sending Error message");
            rabbitSender.send(RabbitConfig.errorQueueName, "Hello Error !");
        }
    }


}
