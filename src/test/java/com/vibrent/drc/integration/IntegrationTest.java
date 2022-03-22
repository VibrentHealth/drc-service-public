package com.vibrent.drc.integration;


import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@ComponentScan(basePackages = {"com.vibrent.vxp.drc.resource"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Category(IntegrationTest.class)
@RunWith(SpringRunner.class)
public abstract class IntegrationTest {
    public static void startAllRequired() {
        KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"))
                .withReuse(true);
        kafka.start();

        System.setProperty("spring.kafka.server", kafka.getBootstrapServers());
    }
}
