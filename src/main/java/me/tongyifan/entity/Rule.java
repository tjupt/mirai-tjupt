package me.tongyifan.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 规则实体类
 *
 * @author tongyifan
 */
public class Rule {
    private String target;
    private List<String> keywords;
    @SerializedName("reply_text")
    private String replyText;
    @SerializedName("reply_type")
    private String replyType;

    public Rule() {
    }

    public Rule(String target, List<String> keywords, String replyText, String replyType) {
        this.target = target;
        this.keywords = keywords;
        this.replyText = replyText;
        this.replyType = replyType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public String getReplyType() {
        return replyType;
    }

    public void setReplyType(String replyType) {
        this.replyType = replyType;
    }

    @Override
    public String toString() {
        return "Rule{" +
                "target='" + target + '\'' +
                ", keywords=" + keywords +
                ", replyText='" + replyText + '\'' +
                ", replyType='" + replyType + '\'' +
                '}';
    }
}
