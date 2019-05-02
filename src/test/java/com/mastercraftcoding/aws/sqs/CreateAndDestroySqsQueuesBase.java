package com.mastercraftcoding.aws.sqs;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

public class CreateAndDestroySqsQueuesBase {

    @Autowired
    private QueueManager queueManager;

    @Autowired
    private ApplicationContext applicationContext;


    protected void performCreateAndDestroyTest() throws Exception {
        // Make sure the Spring Context was set up correctly.
        Assertions.assertNotNull(queueManager);
        Assertions.assertNotNull(applicationContext);

        // Find all of the queue configurations
        Map<String, QueueConfiguration> queueConfigurations = applicationContext.getBeansOfType(QueueConfiguration.class);
        Assertions.assertNotNull(queueConfigurations);
        Assertions.assertEquals(2, queueConfigurations.size());

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

        // Delete the queues
        queueConfigurations.values().forEach(
                nextQueueConfiguration -> queueManager.deleteQueue(nextQueueConfiguration)
        );
    }
}
