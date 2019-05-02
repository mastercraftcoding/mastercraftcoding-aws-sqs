package com.mastercraftcoding.aws.sqs;

public class QueueStatistics {

    private int messageCount;
    private int delayedMessageCount;
    private int invisibleMessageCount;

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
