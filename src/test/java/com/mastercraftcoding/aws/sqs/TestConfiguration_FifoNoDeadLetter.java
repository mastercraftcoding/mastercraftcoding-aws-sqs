package com.mastercraftcoding.aws.sqs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;

@Configuration
public class TestConfiguration_FifoNoDeadLetter {

    @Bean
    public QueueManager createQueueManager() {

        // Create the QueueManager instance we want to use.
        return new QueueManager(Region.US_EAST_1, RuntimeEnvironment.IntegrationTesting, "IntegrationTestNDL");
    }

    @Bean
    public QueueConfiguration createQueue1Configuration() {

        return QueueConfiguration.builder()
                .queueBaseName("TestQueue1")
                .isFifoQueue(true)
                .createDeadLetterQueue(false)
                .build();
    }

    @Bean
    public QueueConfiguration createQueue2Configuration() {

        return QueueConfiguration.builder()
                .queueBaseName("TestQueue2")
                .isFifoQueue(true)
                .createDeadLetterQueue(false)
                .build();
    }
}
