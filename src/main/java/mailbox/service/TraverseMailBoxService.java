package mailbox.service;

import mailbox.entity.Mail;

public interface TraverseMailBoxService {

    /**
     * MailBoxList의 크기
     * 
     * @return
     */
    public int size();

    /**
     * MailBox를 순회하면서 size가 max가 아닌 첫번 째 MailBox의 마지막에 Mail을 추가한다. 만약에 모든 MailBox가 가득찬 상태라면 새로운 MailBox를 생성하고 mailBoxMap에 put 해준다.
     * 
     * @param mail
     * @return 추가된 Mail
     */
    public Mail traverseAndAdd(Mail mail);

    /**
     * MailBox를 순회하면서 mailId와 일치하는 Mail을 해당 MailBox에서 제거한다. 만약에 그 Mail이 마지막 Mail인 경우에는 MailBox를 mailBoxMap에서 제거해준다.
     * 
     * @param mailId
     * @return 지워진 Mail
     */
    public Mail traverseAndRemove(String mailId);

}