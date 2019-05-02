package com.mastercraftcoding.aws.sqs;

import com.mastercraftcoding.aws.sqs.messages.EmailPasswordResetMessage;
import com.mastercraftcoding.aws.sqs.messages.NewUserEmailMessage;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.regions.Region;

import java.util.ArrayList;

public class TestConfiguration_JsonBasedMessageHandler1 {
    @Bean
    public QueueManager createQueueManager() {

        // Create the QueueManager instance we want to use.
        return new QueueManager(Region.US_EAST_1, RuntimeEnvironment.IntegrationTesting, "IntegrationTestSendReceiveJSON");
    }

    @Bean("QueueConfiguration")
    public QueueConfiguration createQueue1Configuration() {

        return QueueConfiguration.builder()
                .queueBaseName("TestQueueJson")
                .isFifoQueue(false)
                .createDeadLetterQueue(false)
                .receiveMessageWaitTimeSeconds(20)
                .build();
    }

    @Bean
    public TestJsonMessageHandler jsonMessageHandler() {
        TestJsonMessageHandler returnHandler = new TestJsonMessageHandler();
        returnHandler.setJsonJavaTypeFieldName("javaType");

        ArrayList<Class> validMessageClasses = new ArrayList<>();
        validMessageClasses.add(NewUserEmailMessage.class);
        validMessageClasses.add(EmailPasswordResetMessage.class);

        return returnHandler;
    }

}
