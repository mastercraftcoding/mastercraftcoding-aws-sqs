package com.mastercraftcoding.aws.sqs.messages;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "javaType")
public class EmailPasswordResetMessage {

    private String accountUsername;

    public EmailPasswordResetMessage() {
    }

    public EmailPasswordResetMessage(String accountUsername) {
        this.accountUsername = accountUsername;
    }

    public String getAccountUsername() {
        return accountUsername;
    }

    public void setAccountUsername(String accountUsername) {
        this.accountUsername = accountUsername;
    }
}
