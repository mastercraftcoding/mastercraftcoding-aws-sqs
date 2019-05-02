package com.mastercraftcoding.aws.sqs;

import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.regions.Region;

public class TestConfiguration_MessageHandlerTest1 {

    @Bean
    public QueueManager createQueueManager() {

        // Create the QueueManager instance we want to use.
        return new QueueManager(Region.US_EAST_1, RuntimeEnvironment.IntegrationTesting, "IntegrationTestSendReceiveX");
    }

    @Bean("QueueConfiguration")
    public QueueConfiguration createQueue1Configuration() {

        return QueueConfiguration.builder()
                .queueBaseName("TestQueue1")
                .isFifoQueue(false)
                .createDeadLetterQueue(false)
                .receiveMessageWaitTimeSeconds(20)
                .build();
    }

}
