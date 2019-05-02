package com.mastercraftcoding.aws.sqs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;

@Configuration
public class TestConfiguration_FifoSendReceive {

    @Bean
    public QueueManager createQueueManager() {

        // Create the QueueManager instance we want to use.
        return new QueueManager(Region.US_EAST_1, RuntimeEnvironment.IntegrationTesting, "IntegrationTestSendReceive1");
    }

    @Bean
    public QueueConfiguration createQueue1Configuration() {

        return QueueConfiguration.builder()
                .queueBaseName("TestQueue1")
                .isFifoQueue(true)
                .createDeadLetterQueue(false)
                .receiveMessageWaitTimeSeconds(20)
                .build();
    }
}
