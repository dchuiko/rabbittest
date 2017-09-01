package org.dchuiko.rabbittest;

import org.dchuiko.rabbittest.rabbit.Sender;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import javax.annotation.PostConstruct;

@Configuration
@RabbitListenerTest(spy = false, capture = true)
@EnableAutoConfiguration
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {Sender.class,
                                                                                                  Application.class}))
public class RabbitITConfig {

//    @PostConstruct
//    public void waitt() {
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//        }
//    }
}
