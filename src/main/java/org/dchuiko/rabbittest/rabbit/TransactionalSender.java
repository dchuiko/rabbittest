package org.dchuiko.rabbittest.rabbit;

import org.dchuiko.rabbittest.config.RabbitConfig;
import org.dchuiko.rabbittest.model.Customer;
import org.dchuiko.rabbittest.model.CustomerRepository;
import org.dchuiko.rabbittest.model.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionalSender {

    @Autowired
    TransactionTemplate tt;
    @Autowired
    CustomerService cs;

    private final Logger log = LoggerFactory.getLogger(TransactionalSender.class);

    private final RabbitSender rabbitSender;

    public TransactionalSender(RabbitSender rabbitSender) {
        this.rabbitSender = rabbitSender;
    }

//    @Transactional
    public void sendTransactional() {

        tt.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                log.warn("Sending Transactional Error message");

                rabbitSender.send(RabbitConfig.transactionalQueueName, "Hello Transactional Error !");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cs.createCustomers(Arrays.asList(new Customer("aaabbbccc", "cccbbbaaa")));

                //        if (true) {
                //            throw new RuntimeException();
                //        }
            }
        });


    }


}
