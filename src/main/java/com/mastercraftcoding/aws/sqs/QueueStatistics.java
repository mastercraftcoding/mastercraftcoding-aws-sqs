package com.mastercraftcoding.aws.sqs;

public class QueueStatistics {

    private final int messageCount;
    private final int delayedMessageCount;
    private final int invisibleMessageCount;

    public QueueStatistics(int messageCount, int delayedMessageCount, int invisibleMessageCount) {
        this.messageCount = messageCount;
        this.delayedMessageCount = delayedMessageCount;
        this.invisibleMessageCount = invisibleMessageCount;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public int getDelayedMessageCount() {
        return delayedMessageCount;
    }

    public int getInvisibleMessageCount() {
        return invisibleMessageCount;
    }
}
