package com.mastercraftcoding.aws.sqs;

import com.mastercraftcoding.aws.sqs.messages.EmailPasswordResetMessage;
import com.mastercraftcoding.aws.sqs.messages.NewUserEmailMessage;
import com.mastercraftcoding.aws.sqs.utility.JsonUtility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@ExtendWith(SpringExtension.class)
@ContextConfiguration( locations = { "classpath:JsonMessageHandler2TestConfig.xml" })
public class JsonBasedMessageHandlerTest2 {

    @Autowired
    @Qualifier("QueueConfiguration")
    private QueueConfiguration queueConfiguration;

    @Autowired
    private QueueManager queueManager;

    @Autowired
    private TestJsonMessageHandler messageHandler;

    private final List<String> jsonMessageStream = new ArrayList<>();

    @Test
    public void testSendReceiveJsonMessageSpringXmlConfiguration() throws Exception {

        Assertions.assertNotNull(queueConfiguration);
        Assertions.assertNotNull(queueManager);

        for(int i = 0 ; i < 10 ; i++ ) {

            if( i % 2 == 0 ) {
                NewUserEmailMessage message = new NewUserEmailMessage();
                message.setNewUserAccountUsername("user" + i);
                message.setNewUserNickname("User Nickname " + i);

                jsonMessageStream.add(JsonUtility.objectToJson(message));
            }
            else {
                EmailPasswordResetMessage message = new EmailPasswordResetMessage();
                message.setAccountUsername("user" + i);

                jsonMessageStream.add(JsonUtility.objectToJson(message));
            }
        }

        synchronized (messageHandler) {

            for (String nextMessage : jsonMessageStream) {

                MessageSendContext sendContext = MessageSendContext.builder()
                        .messageText(nextMessage)
                        .queueConfiguration(queueConfiguration)
                        .messageDelaySeconds(0)
                        .build();

                sendContext.sendMessage();
            }

            messageHandler.wait();

            Assertions.assertEquals(10, messageHandler.getReceivedMessages().size());
        }

        queueManager.deleteQueue(queueConfiguration);
    }

}
