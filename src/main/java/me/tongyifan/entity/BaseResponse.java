package me.tongyifan.entity;

import java.io.Serializable;

public class BaseResponse<T> implements Serializable {
    private static final long serialVersionUID = 5129295129131156905L;

    private int status;
    private long timestamp;
    private String msg;
    private T data;

    public BaseResponse() {
    }

    public BaseResponse(int status, long timestamp, String msg, T data) {
        this.status = status;
        this.timestamp = timestamp;
        this.msg = msg;
        this.data = data;
    }

    public BaseResponse(boolean status, long timestamp, String msg, T data) {
        this.status = status ? 1 : 0;
        this.timestamp = timestamp;
        this.msg = msg;
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Response{" +
                "status=" + status +
                ", timestamp=" + timestamp +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
