package com.mastercraftcoding.aws.sqs.messages;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "javaType")
public class NewUserEmailMessage {

    private String newUserAccountUsername;
    private String newUserNickname;

    public NewUserEmailMessage() {
    }

    public NewUserEmailMessage(String newUserAccountName, String newUserNickname) {
        this.newUserAccountUsername = newUserAccountName;
        this.newUserNickname = newUserNickname;
    }

    public String getNewUserAccountUsername() {
        return newUserAccountUsername;
    }

    public void setNewUserAccountUsername(String newUserAccountUsername) {
        this.newUserAccountUsername = newUserAccountUsername;
    }

    public String getNewUserNickname() {
        return newUserNickname;
    }

    public void setNewUserNickname(String newUserNickname) {
        this.newUserNickname = newUserNickname;
    }
}
