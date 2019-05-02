package com.mastercraftcoding.aws.sqs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashSet;

public class TestMessageHandler implements MessageHandler {

    @Autowired
    @Qualifier("QueueConfiguration")
    private QueueConfiguration queueConfiguration;

    @Autowired
    private QueueManager queueManager;

    public HashSet<String> messageReceiveSet = new HashSet<>();


    @Override
    public QueueConfiguration getTargetQueueConfiguration() {
        return queueConfiguration;
    }

    @Override
    public String[] getDesiredMessageAttributes() {
        return new String[0];
    }

    @Override
    public MessageHandlerStatus handleMessage(MessageReceiveContext messageReceiveContext) {
        messageReceiveSet.add(messageReceiveContext.getMessageText());

        if( messageReceiveSet.size() == 5 ) {
            synchronized (this) {
                this.notify();
            }
        }

        return MessageHandlerStatus.Message_Processed_Successfully;
    }
}
