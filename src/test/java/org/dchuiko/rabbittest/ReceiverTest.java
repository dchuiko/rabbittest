package org.dchuiko.rabbittest;

import org.dchuiko.rabbittest.config.RabbitConfig;
import org.dchuiko.rabbittest.rabbit.RabbitSender;
import org.dchuiko.rabbittest.rabbit.Receiver;
import org.junit.Test;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class ReceiverTest extends BaseRabbitTest {
    @Autowired
    private RabbitListenerTestHarness harness;
    @Autowired
    private RabbitSender rabbitSender;

    @Test
    public void testSlow() throws Exception {
        rabbitSender.send(RabbitConfig.slowQueueName, "Very Slow");

        Receiver listener = this.harness.getSpy("slow");
        assertNotNull(listener);
        verify(listener).receiveSlow(eq("Very Slow"), any());
    }
}
