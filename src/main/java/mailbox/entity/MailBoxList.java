/**
 * 
 */
package mailbox.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author seoi
 */
public class MailBoxList {

    private List<String> mailBoxKeys;

    public MailBoxList() {
        super();
        this.mailBoxKeys = new ArrayList<>(1);
    }

    public MailBoxList(String newMailBoxId) {
        super();
        mailBoxKeys = new ArrayList<>();
        mailBoxKeys.add(newMailBoxId);
    }

    public List<String> getMailBoxKeys() {
        return mailBoxKeys;
    }

    public void setMailBoxKeys(List<String> mailBoxKeys) {
        this.mailBoxKeys = mailBoxKeys;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
