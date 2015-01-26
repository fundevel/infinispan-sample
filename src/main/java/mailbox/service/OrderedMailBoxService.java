package mailbox.service;

import java.util.ConcurrentModificationException;
import java.util.List;

import mailbox.entity.Mail;
import mailbox.entity.MailBox;
import mailbox.exception.NotFoundException;

/**
 * @author seoi
 */
public interface OrderedMailBoxService {

    /**
     * 계정생성될 때 호출 되어야 함
     * 
     * @param me
     */
    void createMailBox(String me);

    /**
     * MailBox List를 가지고 와서 MailBox들의 키의 Set을 가지고 온다.
     * 해당 키의 Set 을 통해 MailBox들을 각각 Iteration하고 MailBox에서
     * 실제 mailId가 있는지 비교 검증하여 mail이 있으면 return 한다.
     * 부가적으로, MaliBox가 비어있을 경우에는 MailBoxList에서 key 삭제를 시도하지만 실패해도 무방(언젠가 지워지면 됨)
     * MailBox List는 계정생성 할 때 생성되므로 비어있을 수 없다. createMailBox 가 계정생성 시 호출되어야 함
     * 
     * @param me
     * @param mailId
     * @return mailId에 대한 Mail
     * @throws NotFoundException
     */
    Mail readOne(String me, String mailId) throws NotFoundException, ConcurrentModificationException;

    /**
     * 메일을 받을 사람의 MailBoxList를 가지고 온 후, 그 사람의 메일 박스에서 마지막 메일 박스를 가지고 온다.
     * 마지막 메일 박스의 크기가 가득 찼으면, 새로운 메일 박스를 생성해주고 MailBoxList에는 새로운 메일 박스의 키를 추가
     * 그리고 MailBoxCache에는 새로운 MailBox를 추가 해 준다.
     * 크기가 가득 차지 않았으면, 기존의 메일 박스에 새로 받은 메일을 추가 해준다.
     * 
     * @param receiver
     * @param mail
     * @return
     */
    Mail send(String receiver, Mail mail) throws ConcurrentModificationException;

    /**
     * MailBoxList에 있는 키리스트를 이용해 그 키에 해당 하는 MailBox를 루프를 돌면서 가지고 온 후 ,
     * 다시 MailBox에 있는 Mail들을 루프를 돌면서 필요한 로직을 검증하고 반환할 객체에 add 해준다.
     * 
     * @param me
     * @return 모든 메일 들
     */
    List<Mail> getMails(String me);

    /**
     * MailBox 목록들을 가지고 온다.
     * 
     * @param me
     * @return
     */
    List<MailBox> getMailBoxes(String me);
}
