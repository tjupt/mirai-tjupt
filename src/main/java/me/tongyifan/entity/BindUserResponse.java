package me.tongyifan.entity;

import java.io.Serializable;

/**
 * @author tongyifan
 */
public class BindUserResponse implements Serializable {
    private static final long serialVersionUID = 8241014348208999603L;
    private JoinGroupEventResponseAction action;
    private String responseToUser;
    private String messagesToAdmin;

    public BindUserResponse(JoinGroupEventResponseAction action, String responseToUser, String messagesToAdmin) {
        this.action = action;
        this.responseToUser = responseToUser;
        this.messagesToAdmin = messagesToAdmin;
    }

    public JoinGroupEventResponseAction getAction() {
        return action;
    }

    public String getResponseToUser() {
        return responseToUser;
    }

    public String getMessagesToAdmin() {
        return messagesToAdmin;
    }

    @Override
    public String toString() {
        return "BindUserResponse{" +
                "action=" + action +
                ", responseToUser='" + responseToUser + '\'' +
                ", messagesToAdmin='" + messagesToAdmin + '\'' +
                '}';
    }
}
