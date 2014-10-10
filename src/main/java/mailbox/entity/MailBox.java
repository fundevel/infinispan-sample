package mailbox.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class MailBox {

    private List<Mail> mails;

    public MailBox() {
        super();
        mails = new ArrayList<Mail>(1);
    }

    public MailBox(Mail mail) {
        super();
        mails = new ArrayList<Mail>(1);
        mails.add(mail);
    }

    public List<Mail> getMails() {
        return mails;
    }

    public void setMails(List<Mail> mails) {
        this.mails = mails;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
