package com.mastercraftcoding.aws.sqs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;

@ExtendWith(SpringExtension.class)
@ContextConfiguration( classes = {TestConfiguration_MessageHandlerTest1.class, TestMessageHandler.class} )
@TestPropertySource("classpath:Testing.properties")
public class MessageHandlerTest1 {

    @Autowired
    @Qualifier("QueueConfiguration")
    private QueueConfiguration queueConfiguration;

    @Autowired
    private QueueManager queueManager;

    @Autowired
    private TestMessageHandler messageHandler;

    private HashSet<String> messageSendSet = new HashSet<>();

    @Test
    public void testSendReceiveSpringAutoConfiguration() throws InterruptedException {

        Assertions.assertNotNull(queueConfiguration);
        Assertions.assertNotNull(queueManager);

        messageSendSet.add("m1");
        messageSendSet.add("m2");
        messageSendSet.add("m3");
        messageSendSet.add("m4");
        messageSendSet.add("m5");

        synchronized ( messageHandler )
        {
            for (String nextMessage : messageSendSet) {
                MessageSendContext sendContext = MessageSendContext.builder()
                        .queueConfiguration(queueConfiguration)
                        .messageText(nextMessage)
                        .build();

                sendContext.sendMessage();
            }

            messageHandler.wait();

            Assertions.assertEquals(5, messageHandler.messageReceiveSet.size());
            for( String nextString : messageSendSet ) {
                Assertions.assertTrue(messageHandler.messageReceiveSet.contains(nextString));
            }

            Thread.sleep(3000);

            QueueStatistics queueStatistics = queueManager.currentMessageCount(queueConfiguration);
            Assertions.assertEquals(0, queueStatistics.getMessageCount());

            queueManager.deleteQueue(queueConfiguration);
        }
    }
}
