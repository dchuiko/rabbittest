package org.dchuiko.rabbittest;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RabbitITConfig.class})
public abstract class BaseRabbitTest {
//    public static final DockerComposeContainer RABBIT;

    static {
//        RABBIT = new DockerComposeContainer(composeFile());
//        RABBIT.starting(null);
    }

    private static File composeFile() {
        try {
            URL url = BaseRabbitTest.class.getClassLoader().getResource("rabbit-docker-compose.yml");
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException();
        }
    }

}