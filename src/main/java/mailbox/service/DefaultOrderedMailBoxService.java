/**
 * 
 */
package mailbox.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import mailbox.entity.Mail;
import mailbox.entity.MailBox;
import mailbox.entity.MailBoxList;

import org.jgroups.util.UUID;

/**
 * @author seoi
 */
public class DefaultOrderedMailBoxService implements OrderedMailBoxService {

    private ConcurrentHashMap<String, MailBoxList> mailBoxListCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, MailBox> mailBoxCache = new ConcurrentHashMap<>();

    private final int maximumMailBoxSize;

    public DefaultOrderedMailBoxService(int maximumMailBoxSize) {
        super();
        this.maximumMailBoxSize = maximumMailBoxSize;
    }

    private String generateMailBoxListKey(String me) {
        return "MBL_" + me;

    }

    /*
     * (non-Javadoc)
     * @see net.netmarble.m.runningmon.service.mailbox.OrderedMailBoxService#createMailBox(java.lang.String)
     */
    @Override
    public void createMailBox(String me) {
        String newMailBoxKey = UUID.randomUUID().toString();
        mailBoxListCache.putIfAbsent(generateMailBoxListKey(me), new MailBoxList(newMailBoxKey));
        mailBoxCache.putIfAbsent(newMailBoxKey, new MailBox());
    }

    /*
     * (non-Javadoc)
     * @see net.netmarble.m.runningmon.service.mailbox.OrderedMailBoxService#readOne(java.lang.String, java.lang.String)
     */
    @Override
    public Mail readOne(String me, String mailId) {
        MailBoxList mailBoxList = mailBoxListCache.get(generateMailBoxListKey(me));

        Iterator<String> mailBoxKeysIterator = mailBoxList.getMailBoxKeys().iterator();

        while (mailBoxKeysIterator.hasNext()) {
            String mailBoxKey = mailBoxKeysIterator.next();
            MailBox mailBox = mailBoxCache.get(mailBoxKey);

            // mailbox가 null 이면 mailbox key list에서 key를 지워준다.
            if (mailBox == null) {
                //mailBox 삭제 하는 로직 필요, 실패하면 무시해도 될 듯 언젠가 지워지면 됨
                mailBoxKeysIterator.remove();
                continue;
            }

            List<Mail> mails = mailBox.getMails();

            Iterator<Mail> iterator = mails.iterator();

            while (iterator.hasNext()) {

                Mail mail = iterator.next();

                if (mail.getId().equals(mailId)) {
                    iterator.remove();
                    return mail;
                }

            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * @see net.netmarble.m.runningmon.service.mailbox.OrderedMailBoxService#send(java.lang.String, net.netmarble.m.runningmon.repository.mailbox.entity.Mail)
     */
    @Override
    public Mail send(String receiver, Mail mail) {

        MailBoxList mailBoxList = mailBoxListCache.get(generateMailBoxListKey(receiver));

        List<String> mailBoxKeys = mailBoxList.getMailBoxKeys();

        String lastMailBoxKey = mailBoxKeys.get(mailBoxKeys.size() - 1);

        MailBox mailBox = mailBoxCache.get(lastMailBoxKey);

        if (mailBox.getMails().size() >= maximumMailBoxSize) {

            // MailBox 버켓이 감당할 수 있는 사이즈를 넘었으므로 새로운 버켓 생성
            String newMailBoxKey = UUID.randomUUID().toString();

            boolean isAdded = mailBoxKeys.add(newMailBoxKey);

            //키 추가가 성공했다면 mailBoxCache에 새로운 메일을 가진 메일박스 생성
            if (isAdded) {
                MailBox newMailBox = new MailBox(mail);
                mailBoxCache.put(newMailBoxKey, newMailBox);

            }
        } else {
            mailBox.getMails().add(mail);
            mailBoxCache.put(lastMailBoxKey, mailBox);
        }
        return mail;

    }

    /*
     * (non-Javadoc)
     * @see net.netmarble.m.runningmon.service.mailbox.OrderedMailBoxService#getMails(java.lang.String)
     */
    @Override
    public List<Mail> getMails(String me) {
        List<Mail> mails = new ArrayList<>();

        MailBoxList mailBoxList = mailBoxListCache.get(generateMailBoxListKey(me));

        for (String mailBoxKey : mailBoxList.getMailBoxKeys()) {
            MailBox mailBox = mailBoxCache.get(mailBoxKey);
            for (Mail m : mailBox.getMails()) {

                //TODO 필요한 검증 로직을 추가해준다. 여기서는 만료시간 체크
                if (m.getExpiring() < Calendar.getInstance().getTimeInMillis()) {
                    continue;
                }

                mails.add(m);
            }
        }
        return mails;
    }

    public int size(String me) {
        return mailBoxListCache.get(generateMailBoxListKey(me)).getMailBoxKeys().size();
    }

    @Override
    public List<MailBox> getMailBoxes(String me) {
        List<MailBox> mailBoxes = new ArrayList<>();

        MailBoxList mailBoxList = mailBoxListCache.get(generateMailBoxListKey(me));

        for (String mailBoxKey : mailBoxList.getMailBoxKeys()) {
            MailBox mailBox = mailBoxCache.get(mailBoxKey);
            mailBoxes.add(mailBox);
        }
        return mailBoxes;
    }
}
