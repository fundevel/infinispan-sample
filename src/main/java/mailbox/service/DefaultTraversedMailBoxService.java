/**
 * 
 */
package mailbox.service;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mailbox.entity.Mail;
import mailbox.entity.MailBox;
import mailbox.service.TraverseMailBoxService;

/**
 * K,V Storage에서 LinkedList 형태의 자료구조로 데이터를 관리해야 할 경우
 * 
 * @author seoi
 */
public class DefaultTraversedMailBoxService implements TraverseMailBoxService {

    private ConcurrentHashMap<String, MailBox> mailBoxes = new ConcurrentHashMap<>();

    private final int maximumSize;

    public DefaultTraversedMailBoxService(int maximumSize) {
        super();
        this.maximumSize = maximumSize;
    }

    /*
     * (non-Javadoc)
     * @see net.netmarble.m.runningmon.service.mailbox.MailBoxService#size()
     */
    @Override
    public int size() {
        return mailBoxes.size();
    }

    /*
     * (non-Javadoc)
     * @see net.netmarble.m.runningmon.service.mailbox.MailBoxService#traverseAndAdd(net.netmarble.m.runningmon.repository.mailbox.entity.Mail)
     */
    @Override
    public Mail traverseAndAdd(Mail mail) {

        for (MailBox mailBox : mailBoxes.values()) {
            List<Mail> mails = mailBox.getMails();
            if (mails.size() < maximumSize) {
                mails.add(mail);
                return mail;
            }
        }

        MailBox newMailBox = new MailBox(mail);
        String newMailBoxId = UUID.randomUUID().toString();

        mailBoxes.put(newMailBoxId, newMailBox);

        return mail;

    }

    /*
     * (non-Javadoc)
     * @see net.netmarble.m.runningmon.service.mailbox.MailBoxService#traverseAndRemove(java.lang.String)
     */
    @Override
    public Mail traverseAndRemove(String mailId) {

        for (String key : mailBoxes.keySet()) {

            MailBox mailBox = mailBoxes.get(key);

            List<Mail> mails = mailBox.getMails();

            Iterator<Mail> iterator = mails.iterator();

            while (iterator.hasNext()) {

                Mail mail = iterator.next();

                if (mail.getId().equals(mailId)) {
                    if (mails.size() == 1) {
                        mailBoxes.remove(key);
                    } else {
                        iterator.remove();
                    }
                    return mail;
                }

            }
        }

        return null;
    }
}
