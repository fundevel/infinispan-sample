package player.service;

import java.util.ConcurrentModificationException;

import mailbox.exception.AlreadyCreatedException;
import mailbox.exception.NotFoundException;

import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.commons.util.concurrent.NotifyingFuture;

import player.entity.Player;

public interface PlayerService {

    /**
     * 신규 Player를 만들어 준다. 기본 재화, 캐릭터 등의 지급이 이루어 지며 최초 계정 생성시 이루어 져야 한다.
     * 
     * @param me
     * @return
     * @throws AlreadyCreatedException
     */
    public Player createPlayer(String me) throws AlreadyCreatedException;

    /**
     * 나의 Player 정보를 가지고 온다.
     * 
     * @param me
     * @return
     * @throws NotFoundException
     */
    public Player getPlayer(String me) throws NotFoundException;

    /**
     * 나의 Player 정보를 비동기로 가지고온다. Future를 가지고 온다.
     * 
     * @param me
     * @return
     */
    public NotifyingFuture<Player> asyncGetPlayer(String me);

    /**
     * CompareAndSet 동작을 위해 Player의 VersionedValue를 가지고 온다. VersionedValue에는 version과 value가 있다.
     * 이 API는 save와 pair로 동작할 가능성이 크다.
     * 
     * @param me
     * @return
     * @throws NotFoundException
     */
    public VersionedValue<Player> getVersionedPlayer(String me) throws NotFoundException;

    /**
     * CompareAndSet 연산으로 newPlayer의 값을 레파지토리에 저장한다.
     * 연산이 실패할 경우 ConcurrentModificationException을 발생시킨다.
     * 
     * @param me
     * @param newPlayer
     * @param version
     * @return
     * @throws ConcurrentModificationException
     */
    public Player compareAndSave(String me, Player newPlayer, long version) throws ConcurrentModificationException;

}