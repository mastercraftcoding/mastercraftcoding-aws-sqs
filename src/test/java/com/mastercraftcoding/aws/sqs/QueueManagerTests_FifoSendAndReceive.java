package com.mastercraftcoding.aws.sqs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@ContextConfiguration( classes = {TestConfiguration_FifoSendReceive.class} )
@TestPropertySource("classpath:Testing.properties")
public class QueueManagerTests_FifoSendAndReceive {

    @Autowired
    private QueueManager queueManager;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DirtiesContext
    public void testQueueManagerSetupNoFifoNoDeadLetter() throws Exception {
        // Make sure the Spring Context was set up correctly.
        Assertions.assertNotNull(queueManager);
        Assertions.assertNotNull(applicationContext);

        // Find all of the queue configurations
        Map<String, QueueConfiguration> queueConfigurations = applicationContext.getBeansOfType(QueueConfiguration.class);
        Assertions.assertNotNull(queueConfigurations);
        Assertions.assertEquals(1, queueConfigurations.size());

        // Count how many queues we should have...
        int totalQueues = queueConfigurations.values().stream()
                .mapToInt( nextQueueConfiguration -> nextQueueConfiguration.isCreateDeadLetterQueue() ? 2 : 1)
                .sum();
        // There should be several queues created...verify that it is so.
        List<String> queueUrlList = queueManager.queueUrlList();
        Assertions.assertNotNull(queueUrlList);
        Assertions.assertEquals(totalQueues, queueUrlList.size());

        // Make sure the URLs that we got back from SQS are represented in
        // our current configurations.
        queueConfigurations.values().forEach(
                nextQueueConfiguration -> {
                    if (!queueUrlList.contains(nextQueueConfiguration.getQueueUrl())) {
                        Assertions.assertTrue(false, "No url found: " + nextQueueConfiguration.getQueueUrl());
                    }
                }
        );

        QueueConfiguration targetQueue = queueConfigurations.values().iterator().next();
        Assertions.assertNotNull(targetQueue);

        // Send a message
        MessageSendContext sendMessage = MessageSendContext.builder()
                .messageFifoDeduplicationId(UUID.randomUUID().toString())
                .messageFifoGroupId(UUID.randomUUID().toString())
                .messageText("Hello World")
                .customAttribute("TheAnswer" , MessageAttributeValue.builder().stringValue("42").dataType("String").build())
                .queueConfiguration(targetQueue)
                .build();

        sendMessage.sendMessage();;

        // Determine the number of messages
        QueueStatistics queueStatistics = queueManager.currentMessageCount(targetQueue);
        Assertions.assertEquals(1, queueStatistics.getMessageCount());
        Assertions.assertEquals(0, queueStatistics.getDelayedMessageCount());
        Assertions.assertEquals(0, queueStatistics.getInvisibleMessageCount());

        // Try to read the same message back
        MessageReceiveContext[] messagesToProcess = queueManager.readMessages(targetQueue, new String[] { "TheAnswer" },10);
        Assertions.assertNotNull(messagesToProcess);
        Assertions.assertEquals(1, messagesToProcess.length);

        Assertions.assertEquals("Hello World", messagesToProcess[0].getMessageText());
        Assertions.assertTrue(messagesToProcess[0].hasCustomAttribute("TheAnswer"));
        Assertions.assertEquals("42", messagesToProcess[0].getCustomAttribute("TheAnswer").stringValue());

        // Delete the queues
        queueConfigurations.values().forEach(
                nextQueueConfiguration -> queueManager.deleteQueue(nextQueueConfiguration)
        );
    }
}
