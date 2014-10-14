/**
 * 
 */
package player.service;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import mailbox.exception.AlreadyCreatedException;
import mailbox.exception.NotFoundException;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.commons.util.concurrent.NotifyingFuture;

import player.entity.Character;
import player.entity.Player;
import player.entity.Team;

/**
 * @author seoi
 */
public class RemotePlayerService implements PlayerService {

    private final static String PLAYER_KEY_PREFIX = "PLAYER_";
    private static final int DEFAULT_CASH = 30;
    private static final int DEFAULT_POINT = 1000;
    private static final int DEFAULT_TICKET = 5;
    private RemoteCache<String, Player> playerCache = null;

    public RemotePlayerService(RemoteCache<String, Player> playerCache) {
        super();
        this.playerCache = playerCache;
    }

    /**
     * 플레이어 키를 생성하는 방법
     * 
     * @param me
     * @return
     */
    private String generatePlayerKey(String me) {
        return PLAYER_KEY_PREFIX + me;
    }

    /*
     * (non-Javadoc)
     * @see net.netmarble.m.runningmon.service.player.PlayerService#createPlayer(java.lang.String)
     */
    @Override
    public Player createPlayer(String me) throws AlreadyCreatedException {
        Player player = new Player();
        player.setCash(DEFAULT_CASH);
        player.setPoint(DEFAULT_POINT);
        player.setTicket(DEFAULT_TICKET);
        player.setLevel(1);
        player.setExp(0);

        Character character = new Character();
        character.setExp(0);
        character.setArmor(1);
        character.setAccessory(1);
        character.setArmor(1);
        character.setForce(0);
        character.setLevel(1);
        character.setWeapon(1);

        ArrayList<Character> characters = new ArrayList<Character>();
        characters.add(character);

        player.setLeaderCharacterId(character.getId());
        player.setCharacters(characters);

        Team team = new Team();
        team.setTeamNo(0);
        team.setFormation(1);

        ArrayList<String> characterIds = new ArrayList<>(1);
        characterIds.add(character.getId());
        team.setCharacterIds(characterIds);

        ArrayList<Team> teams = new ArrayList<>(1);
        teams.add(team);

        player.setTeams(teams);

        Player addedPlayer = playerCache.putIfAbsent(generatePlayerKey(me), player);

        if (addedPlayer != null) {
            throw new AlreadyCreatedException();
        }

        return player;
    }

    /*
     * (non-Javadoc)
     * @see net.netmarble.m.runningmon.service.player.PlayerService#getPlayer(java.lang.String)
     */
    @Override
    public Player getPlayer(String me) throws NotFoundException {
        Player player = playerCache.get(generatePlayerKey(me));
        if (player == null) {
            throw new NotFoundException("Player를 찾을 수 없습니다.");
        }
        return player;
    }

    @Override
    public NotifyingFuture<Player> asyncGetPlayer(String me) {
        return playerCache.getAsync(generatePlayerKey(me));
    }

    /*
     * (non-Javadoc)
     * @see net.netmarble.m.runningmon.service.player.PlayerService#getVersionedPlayer(java.lang.String)
     */
    @Override
    public VersionedValue<Player> getVersionedPlayer(String me) throws NotFoundException {
        String key = generatePlayerKey(me);
        VersionedValue<Player> versioned = playerCache.getVersioned(key);
        if (versioned == null) {
            throw new NotFoundException("Player를 찾을 수 없습니다.");
        }
        return versioned;
    }

    /*
     * (non-Javadoc)
     * @see net.netmarble.m.runningmon.service.player.PlayerService#save(java.lang.String, net.netmarble.m.runningmon.repository.player.entity.Player, long)
     */
    @Override
    public Player compareAndSave(String me, Player newPlayer, long version) throws ConcurrentModificationException {
        String key = generatePlayerKey(me);

        boolean replaceWithVersion = playerCache.replaceWithVersion(key, newPlayer, version);
        if (replaceWithVersion) {
            return newPlayer;
        } else {
            throw new ConcurrentModificationException();
        }

    }

}
