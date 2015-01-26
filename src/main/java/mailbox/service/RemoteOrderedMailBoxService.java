/**
 * 
 */
package mailbox.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import mailbox.entity.Mail;
import mailbox.entity.MailBox;
import mailbox.entity.MailBoxList;
import mailbox.exception.NotFoundException;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;

/**
 * @author seoi
 */
public class RemoteOrderedMailBoxService implements OrderedMailBoxService {

    private final static String MAILBOX_LIST_KEY_PREFIX = "MBL_";
    private final static String MAILBOX_KEY_PREFIX = "MB_";

    private RemoteCache<String, MailBoxList> mailBoxListCache = null;
    private RemoteCache<String, MailBox> mailBoxCache = null;

    private final int maximumMailBoxSize;

    public RemoteOrderedMailBoxService(RemoteCache<String, MailBoxList> mailBoxListCache, RemoteCache<String, MailBox> mailBoxCache, int maximumMailBoxSize) {
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
        
        // UUID 가 충분히 Unique 하기 때문에 putIfAbsent로 검증할 필요는 없음
        mailBoxListCache.put(generateMailBoxListKey(me), new MailBoxList(newMailBoxKey));
        mailBoxCache.put(newMailBoxKey, new MailBox());
    }

    @Override
    public Mail readOne(String me, String mailId) throws NotFoundException, ConcurrentModificationException {

        String mailBoxListKey = generateMailBoxListKey(me);

        VersionedValue<MailBoxList> mailBoxListVersioned = mailBoxListCache.getVersioned(mailBoxListKey);

        MailBoxList mailBoxList = mailBoxListVersioned.getValue();
        Iterator<String> mailBoxKeysIterator = mailBoxList.getMailBoxKeys().iterator();

        while (mailBoxKeysIterator.hasNext()) {
            String mailBoxKey = mailBoxKeysIterator.next();
            VersionedValue<MailBox> mailBoxVersioned = mailBoxCache.getVersioned(mailBoxKey);

            //mailbox가 null이면 mailbox key list 에서 key를 지워줘야 한다.
            if (mailBoxVersioned == null) {
                mailBoxKeysIterator.remove();

                //실패해도 괜찮음 언젠가 지워지기만 하면 됨
                mailBoxListCache.replaceWithVersion(mailBoxListKey, mailBoxList, mailBoxListVersioned.getVersion());
                continue;
            }

            MailBox mailBox = mailBoxVersioned.getValue();
            List<Mail> mails = mailBox.getMails();

            Iterator<Mail> iterator = mails.iterator();

            while (iterator.hasNext()) {
                Mail mail = iterator.next();

                if (mail.getId().equals(mailId)) {
                    iterator.remove();
                    boolean isRemoved = mailBoxCache.replaceWithVersion(mailBoxKey, mailBox, mailBoxVersioned.getVersion());
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
    public Mail send(String receiver, Mail mail) throws ConcurrentModificationException {

        String mailBoxListKey = generateMailBoxListKey(receiver);

        VersionedValue<MailBoxList> mailBoxListVersioned = mailBoxListCache.getVersioned(mailBoxListKey);

        MailBoxList mailBoxList = mailBoxListVersioned.getValue();
        List<String> mailBoxKeys = mailBoxList.getMailBoxKeys();

        //MailBoxKey 들 중 제일 마지막 key를 가지고 온다. 없는 경우는 없음(계정생성 때 create 해줌)
        String lastMailBoxKey = mailBoxKeys.get(mailBoxKeys.size() - 1);

        VersionedValue<MailBox> mailBoxVersioned = mailBoxCache.getVersioned(lastMailBoxKey);

        MailBox mailBox = mailBoxVersioned.getValue();

        if (mailBox.getMails().size() >= maximumMailBoxSize) {

            // MailBox 버켓이 감당할 수 있는 사이즈를 넘었으므로 새로운 버켓 생성
            String newMailBoxKey = newMailBoxKey(receiver);

            mailBoxKeys.add(newMailBoxKey);

            //mailbox key add 하는 cas 연산 실패했을 때 미리 만들어 둔 mailBox는 지워야 함 그 사이에 다른 요청으로 이 메일 mailbox에 접근은 불가
            MailBox newMailBox = new MailBox(mail);
            mailBoxCache.put(newMailBoxKey, newMailBox);

            boolean isAddedNewMailBoxKey = mailBoxListCache.replaceWithVersion(mailBoxListKey, mailBoxList, mailBoxListVersioned.getVersion());
            if (isAddedNewMailBoxKey == false) {
                // MailBoxList에 Key add 하는 작업이 실패했으므로, 만들어뒀던 MailBox도 제거. uuid가 unique 하므로 경쟁상태는 없음
                mailBoxCache.remove(newMailBoxKey);
                // MailBoxList에 새로운 Key 추가가 실패하면 Throw. 경우에 따라 루프를 통한 retry도 가능
                throw new ConcurrentModificationException();
            }
        } else {
            mailBox.getMails().add(mail);
            boolean isAddedMailBox = mailBoxCache.replaceWithVersion(lastMailBoxKey, mailBox, mailBoxVersioned.getVersion());
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

        VersionedValue<MailBoxList> mailBoxListVersioned = mailBoxListCache.getVersioned(mailBoxListKey);

        MailBoxList mailBoxList = mailBoxListVersioned.getValue();
        Iterator<String> mailBoxKeysIterator = mailBoxList.getMailBoxKeys().iterator();

        while (mailBoxKeysIterator.hasNext()) {
            String mailBoxKey = mailBoxKeysIterator.next();
            VersionedValue<MailBox> mailBoxVersioned = mailBoxCache.getVersioned(mailBoxKey);

            //mailbox가 null이면 mailbox key list 에서 key를 지워줘야 한다.
            if (mailBoxVersioned == null) {
                mailBoxKeysIterator.remove();

                //실패해도 괜찮음 언젠가 지워지기만 하면 됨
                mailBoxListCache.replaceWithVersion(mailBoxListKey, mailBoxList, mailBoxListVersioned.getVersion());
                continue;
            }

            MailBox mailBox = mailBoxVersioned.getValue();

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
