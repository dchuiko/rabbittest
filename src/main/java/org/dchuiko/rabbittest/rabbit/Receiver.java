package org.dchuiko.rabbittest.rabbit;

import org.dchuiko.rabbittest.model.Customer;
import org.dchuiko.rabbittest.model.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.dchuiko.rabbittest.config.RabbitConfig.errorSecondStepQueueName;

@Component
public class Receiver {
    private final Logger log = LoggerFactory.getLogger(Receiver.class);

    private final int slowCount = 0;
    private final int errorCount = 0;

    private final int count = slowCount + errorCount;
    private final CountDownLatch latch = new CountDownLatch(count);

    private final CustomerService customerService;
    private final RabbitSender rabbitSender;

    @Autowired
    public Receiver(CustomerService customerService,
                    RabbitSender rabbitSender) {
        this.customerService = customerService;
        this.rabbitSender = rabbitSender;
    }

    @RabbitListener(id = "slow", queues = {"#{rtSlowQueue}"}, containerFactory = "rabbitListenerContainerFactory")
    public void receiveSlow(String msg, Message message) {
        try {
            log.warn("Received Slow <{}>, rabbit message:\n<{}>", msg, message);
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException ie) {
            log.error("Interrupted!");
        } catch (Exception e) {
            log.error("Queue processing ended with exception {}", e);
            throw e;
        }
    }

    @Transactional
    @RabbitListener(id = "error", queues = {"#{rtErrorQueue}"}, containerFactory = "rabbitListenerContainerFactory")
    public void receiveError(String msg, Message message) {
        log.warn("Received <{}>, rabbit message:\n<{}>", msg, message);

        try {
            // causes unique constraint exception and transaction rollback
            customerService.createCustomers(asList(
                    new Customer("aa", "bb"),
                    new Customer("aa", "bb")
            ));

        } catch (Exception e) {
            log.warn("Catching exception");
            throw new IllegalArgumentException("aaaa", e);
//            throw new AmqpRejectAndDontRequeueException(e);
        } finally {
            log.warn("Sending Second Step message...");
            // ensure we fire message but it is not sent because of transaction rollback
            rabbitSender.send(errorSecondStepQueueName, "Second step of: \"" + msg + "\"");
        }

        int a = 0;
    }

    @RabbitListener(id = "secondStep", queues = {"#{rtErrorSecondStepQueue}"}, containerFactory =
            "rabbitListenerContainerFactory")
    public void receiveSecondStep(String msg, Message message) {
        log.warn("Received Second Step <{}>, rabbit message:\n<{}>", msg, message);
    }

    @Transactional
    @RabbitListener(id = "transactional", queues = {"#{rtTransactionalQueue}"}, containerFactory =
            "rabbitListenerContainerFactory")
    public void receiveTransactional(String msg, Message message) {
        log.warn("--- COUNT: " + customerService.count());
        log.warn("Received Transactional <{}>, rabbit message:\n<{}>", msg, message);
    }

    @RabbitListener(id="rpc", queues = {"#{rpcQueue}"}, containerFactory = "rabbitListenerContainerFactory")
    @SendTo("#{rpcReplyQueue.name}")
    public String rpc(String msg, Message message) {
        return "RPC reply";
    }

    CountDownLatch getLatch() {
        return latch;
    }

    int getSlowCount() {
        return slowCount;
    }

    int getErrorCount() {
        return errorCount;
    }
}
