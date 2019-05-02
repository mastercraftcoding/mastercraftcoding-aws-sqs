package com.mastercraftcoding.aws.sqs;

public interface MessageHandler {
    QueueConfiguration getTargetQueueConfiguration();
    String[] getDesiredMessageAttributes();
    MessageHandlerStatus handleMessage(MessageReceiveContext messageReceiveContext);
}
