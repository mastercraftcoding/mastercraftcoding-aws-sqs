package com.mastercraftcoding.aws.sqs;

import com.mastercraftcoding.aws.sqs.messages.EmailPasswordResetMessage;
import com.mastercraftcoding.aws.sqs.messages.NewUserEmailMessage;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class TestJsonMessageHandler extends JsonMessageHandlerAbstractBase {

    @Autowired
    private QueueConfiguration queueConfiguration;

    private List<Object> receivedMessages = new ArrayList<>();

    public TestJsonMessageHandler() {
        super(EmailPasswordResetMessage.class, NewUserEmailMessage.class);

    }

    @Override
    protected MessageHandlerStatus handleObjectMessage(MessageReceiveContext messageReceiveContext, Object messageObject) {

        Assertions.assertNotNull(messageObject);
        Assertions.assertTrue( messageObject.getClass() == EmailPasswordResetMessage.class || messageObject.getClass() == NewUserEmailMessage.class);

        receivedMessages.add(messageObject);

        if(receivedMessages.size() == 10 ) {
            synchronized (this) {
                this.notify();
            }
        }

        return MessageHandlerStatus.Message_Processed_Successfully;
    }

    public List<Object> getReceivedMessages() {
        return receivedMessages;
    }

    @Override
    public QueueConfiguration getTargetQueueConfiguration() {
        return queueConfiguration;
    }
}
