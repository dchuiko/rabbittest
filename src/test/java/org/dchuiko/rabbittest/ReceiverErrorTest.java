package org.dchuiko.rabbittest;

import org.dchuiko.rabbittest.config.RabbitConfig;
import org.dchuiko.rabbittest.rabbit.RabbitSender;
import org.dchuiko.rabbittest.rabbit.Receiver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class ReceiverErrorTest extends BaseRabbitTest {
    @Autowired
    private RabbitListenerTestHarness harness;
    @Autowired
    private RabbitSender rabbitSender;

    @Test
    public void testError() throws Exception {
        rabbitSender.send(RabbitConfig.errorQueueName, "Error Message");

        Receiver listener = this.harness.getSpy("error");
        assertNotNull(listener);

        try {
            verify(listener).receiveError(eq("Error Message"), any());
        } catch (Exception e) {

        }
    }
}
