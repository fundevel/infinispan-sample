/**
 * 
 */
package mailbox.service.local;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import mailbox.entity.Mail;
import mailbox.entity.MailBox;
import mailbox.entity.MailBoxList;
import mailbox.service.OrderedMailBoxService;

import org.infinispan.Cache;
import org.jgroups.util.UUID;

import common.exception.NotFoundException;

/**
 * @author seoi
 */
public class LocalOrderedMailBoxService implements OrderedMailBoxService {

    private final static String MAILBOX_LIST_KEY_PREFIX = "MBL_";
    private final static String MAILBOX_KEY_PREFIX = "MB_";

    private Cache<String, MailBoxList> mailBoxListCache = null;
    private Cache<String, MailBox> mailBoxCache = null;

    private final int maximumMailBoxSize;

    public LocalOrderedMailBoxService(Cache<String, MailBoxList> mailBoxListCache, Cache<String, MailBox> mailBoxCache, int maximumMailBoxSize) {
        super();
        this.mailBoxListCache = mailBoxListCache;
        this.mailBoxCache = mailBoxCache;
        this.maximumMailBoxSize = maximumMailBoxSize;
    }

    /**
     * MailBoxList Key 생성 규칙 MAILBOX_LIST_KEY_PREFIX + channelUserId
     * 
     * @param channelUserId
     * @return
     */
    private String generateMailBoxListKey(String channelUserId) {
        return MAILBOX_LIST_KEY_PREFIX + channelUserId;
    }

    /**
     * MailBox Key 생성 규칙 MAILBOX_KEY_PREFIX + channelUserId
     * 
     * @param channelUserId
     * @param mailBoxKey
     * @return
     */
    private String generateMailBoxKey(String channelUserId, String mailBoxKey) {
        return MAILBOX_KEY_PREFIX + channelUserId + "_" + mailBoxKey;
    }

    /**
     * 신규 MailBox Key 발급
     * 
     * @param me
     * @return
     */
    private String newMailBoxKey(String me) {
        return generateMailBoxKey(me, UUID.randomUUID().toString());
    }

    @Override
    public void createMailBox(String me) {
        String newMailBoxKey = newMailBoxKey(me);
        mailBoxListCache.put(generateMailBoxListKey(me), new MailBoxList(newMailBoxKey));
        mailBoxCache.put(newMailBoxKey, new MailBox());
    }

    @Override
    public Mail readOne(String me, String mailId) throws NotFoundException {

        String mailBoxListKey = generateMailBoxListKey(me);

        MailBoxList oldMailBoxList = mailBoxListCache.get(mailBoxListKey);
        MailBoxList newMailBoxList = oldMailBoxList;

        Iterator<String> mailBoxKeysIterator = newMailBoxList.getMailBoxKeys().iterator();

        while (mailBoxKeysIterator.hasNext()) {
            String mailBoxKey = mailBoxKeysIterator.next();

            MailBox oldMailBox = mailBoxCache.get(mailBoxKey);

            if (oldMailBox == null) {
                mailBoxKeysIterator.remove();

                mailBoxListCache.replace(mailBoxListKey, oldMailBoxList, newMailBoxList);
                continue;
            }

            MailBox newMailBox = oldMailBox;
            List<Mail> mails = newMailBox.getMails();

            Iterator<Mail> iterator = mails.iterator();

            while (iterator.hasNext()) {
                Mail mail = iterator.next();

                if (mail.getId().equals(mailId)) {
                    iterator.remove();
                    boolean isRemoved = mailBoxCache.replace(mailBoxKey, oldMailBox, newMailBox);
                    if (isRemoved) {
                        return mail;
                    } else {
                        throw new ConcurrentModificationException();
                    }
                }
            }
        }

        throw new NotFoundException("Can't find mail");
    }

    @Override
    public Mail send(String receiver, Mail mail) {

        String mailBoxListKey = generateMailBoxListKey(receiver);

        MailBoxList oldMailBoxList = mailBoxListCache.get(mailBoxListKey);
        MailBoxList newMailBoxList = oldMailBoxList;

        List<String> mailBoxKeys = newMailBoxList.getMailBoxKeys();

        //MailBoxKey 들 중 제일 마지막 key를 가지고 온다. 없는 경우는 없음(계정생성 때 create 해줌)
        String lastMailBoxKey = mailBoxKeys.get(mailBoxKeys.size() - 1);

        MailBox oldMailBox = mailBoxCache.get(lastMailBoxKey);
        MailBox newMailBox = oldMailBox;

        if (newMailBox.getMails().size() >= maximumMailBoxSize) {

            // MailBox 버켓이 감당할 수 있는 사이즈를 넘었으므로 새로운 버켓 생성
            String newMailBoxKey = newMailBoxKey(receiver);

            mailBoxKeys.add(newMailBoxKey);

            //mailbox key add 하는 cas 연산 실패했을 때 미리 만들어 둔 mailBox는 지워야 함 그 사이에 다른 요청으로 이 메일 mailbox에 접근은 불가
            mailBoxCache.put(newMailBoxKey, new MailBox(mail));

            boolean isAddedNewMailBoxKey = mailBoxListCache.replace(mailBoxListKey, oldMailBoxList, newMailBoxList);
            if (isAddedNewMailBoxKey == false) {
                // MailBoxList에 Key add 하는 작업이 실패했으므로, 만들어뒀던 MailBox도 제거. uuid가 unique 하므로 경쟁상태는 없음
                mailBoxCache.remove(newMailBoxKey);
                throw new ConcurrentModificationException();
            }

        } else {
            newMailBox.getMails().add(mail);
            boolean isAddedMailBox = mailBoxCache.replace(lastMailBoxKey, oldMailBox, newMailBox);
            if (!isAddedMailBox) {
                throw new ConcurrentModificationException();
            }
        }

        return mail;
    }

    @Override
    public List<Mail> getMails(String me) {

        String mailBoxListKey = generateMailBoxListKey(me);

        List<Mail> mails = new ArrayList<>();

        MailBoxList oldMailBoxList = mailBoxListCache.get(mailBoxListKey);
        MailBoxList newMailBoxList = oldMailBoxList;
        Iterator<String> mailBoxKeysIterator = newMailBoxList.getMailBoxKeys().iterator();

        while (mailBoxKeysIterator.hasNext()) {
            String mailBoxKey = mailBoxKeysIterator.next();

            MailBox mailBox = mailBoxCache.get(mailBoxKey);
            if (mailBox == null) {
                mailBoxKeysIterator.remove();

                mailBoxListCache.replace(mailBoxListKey, oldMailBoxList, newMailBoxList);
                continue;
            }

            Iterator<Mail> mailBoxIterator = mailBox.getMails().iterator();

            //mailBox.getMails().iterator()을 가져와서 루프를 돌면서, 만료시간이 지난 element만 remove하는게 더 효율적이지 않을까?
            // ArrayList
            // add(E), add(int, E), addAll(List<E>), addAll(int, List<E>) -> O(n)
            // remove(int) -> O(n)
            // remove(Object) -> O(n)
            // removeAll(Collection), retainAll(Collection) -> O(n^2)
            // @see http://java-performance.info/arraylist-performance/
            while (mailBoxIterator.hasNext()) {
                Mail m = mailBoxIterator.next();

                //TODO 필요한 검증 로직을 추가해준다. 여기서는 만료시간 체크
                if (m.getExpiring() < Calendar.getInstance().getTimeInMillis()) {
                    mailBoxIterator.remove();
                }
            }

            mails.addAll(mailBox.getMails());
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
