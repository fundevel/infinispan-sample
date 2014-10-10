package mailbox.entity;

import java.util.UUID;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class Mail {

    private String id;
    private String fromChannelUserId;
    private int type;
    private int attachment;
    private int quantity;
    private long sent;
    private long expiring;

    public Mail() {
        super();
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromChannelUserId() {
        return fromChannelUserId;
    }

    public void setFromChannelUserId(String fromChannelUserId) {
        this.fromChannelUserId = fromChannelUserId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getAttachment() {
        return attachment;
    }

    public void setAttachment(int attachment) {
        this.attachment = attachment;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getSent() {
        return sent;
    }

    public void setSent(long sent) {
        this.sent = sent;
    }

    public long getExpiring() {
        return expiring;
    }

    public void setExpiring(long expiring) {
        this.expiring = expiring;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
