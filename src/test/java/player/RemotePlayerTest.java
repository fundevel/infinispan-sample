/**
 * 
 */
package player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import mailbox.exception.NotFoundException;

import org.apache.commons.lang.RandomStringUtils;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.util.concurrent.FutureListener;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.protostream.SerializationContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import player.entity.Player;
import player.marshaller.CharacterMarshaller;
import player.marshaller.PlayerMarshaller;
import player.marshaller.TeamMarshaller;
import player.service.PlayerService;
import player.service.RemotePlayerService;

/**
 * @author seoi
 */
public class RemotePlayerTest {

    private static final String REMOTE_INFINISPAN_SERVER_IP = "10.100.23.230:11222";

    private static final String ME = RandomStringUtils.randomAlphanumeric(5);

    private static PlayerService playerService;

    // 경쟁상태를 구현하는데 필요한 변수
    private static final int NUMBER_OF_COMPETITOR_THREAD = 100;
    private static ExecutorService competitors;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        //Log4j 설정
        String logConfigurationFile = RemotePlayerTest.class.getClassLoader().getResource("log4j2.xml").getPath();
        System.setProperty("log4j.configurationFile", logConfigurationFile);

        //Infinispan 관련 설정
        ConfigurationBuilder builder = new ConfigurationBuilder();

        builder.addServers(REMOTE_INFINISPAN_SERVER_IP);
        builder.forceReturnValues(false);
        builder.tcpNoDelay(true);
        builder.pingOnStartup(true);
        builder.valueSizeEstimate(1024);
        builder.marshaller(new ProtoStreamMarshaller());

        RemoteCacheManager cacheManager = new RemoteCacheManager(builder.create());

        SerializationContext serializationContext = ProtoStreamMarshaller.getSerializationContext(cacheManager);

        try {
            serializationContext.registerProtoFiles("/proto/player.proto");

            serializationContext.registerMarshaller(new PlayerMarshaller());
            serializationContext.registerMarshaller(new CharacterMarshaller());
            serializationContext.registerMarshaller(new TeamMarshaller());
        } catch (IOException e) {
            e.printStackTrace();
        }

        RemoteCache<String, Player> playerCache = cacheManager.getCache();

        //RemoteOrderMailBoxService 종속성 주입 및 인스턴스 생성

        playerService = new RemotePlayerService(playerCache);
        playerService.createPlayer(ME);

        // 경쟁 상태를 재현할 쓰레드 풀 생성
        competitors = Executors.newFixedThreadPool(NUMBER_OF_COMPETITOR_THREAD);

    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

    /**
     * Player 정보를 Get 하는 시나리오
     * 
     * @throws InterruptedException
     * @throws NotFoundException
     * @throws ExecutionException
     */
    @Test
    public void testGetPlayer() throws InterruptedException, NotFoundException {
        Player player = playerService.getPlayer(ME);
        assertNotNull(player);
    }

    @Test
    public void testAsyncGetPlayer() throws InterruptedException, ExecutionException {
        NotifyingFuture<Player> asyncGetPlayer = playerService.asyncGetPlayer(ME);
		
        FutureListener<Player> listener = new FutureListener<Player>() {
			
			@Override
			public void futureDone(Future<Player> future) {
				
				try {
					System.out.println(future.get().toString());
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				System.out.println("asyncGetPlayer가 완료 되었음");
			}
		};

		asyncGetPlayer.attachListener(listener);
		
        while (!asyncGetPlayer.isDone()) {
            System.out.println("asyncGet은 역시 blocking이 안되는걸? 이 시간에 뭔가 다른 로직 처리할 수 있는 여유...");
        }

        Player player = asyncGetPlayer.get();
        
        Thread.sleep(1000);	// FutureDone 제대로 동작할 시간이 필요
        
        assertNotNull(player);
    }

    /**
     * Player 정보를 가지고 온 후 Value를 업데이트 하는 시나리오
     * 
     * @throws NotFoundException
     */
    @Test
    public void testSave() throws NotFoundException {
        VersionedValue<Player> versionedPlayer = playerService.getVersionedPlayer(ME);
        Player versionedPlayerValue = versionedPlayer.getValue();

        versionedPlayerValue.getCharacters().get(0).setLevel(1);

        playerService.compareAndSave(ME, versionedPlayerValue, versionedPlayer.getVersion());

        Player lastPlayer = playerService.getPlayer(ME);

        assertEquals(versionedPlayerValue.getCharacters().get(0).getId(), lastPlayer.getCharacters().get(0).getId());

    }

    /**
     * 멀티쓰레드 환경에서 Save 하는 시나리오
     * 쓰레드당 액션은 캐시 + 1, 경험치 + 1000
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws NotFoundException
     */
    @Test
    public void testConcurrentlySave() throws InterruptedException, ExecutionException, NotFoundException {

        List<Callable<Player>> tasks = new ArrayList<>(NUMBER_OF_COMPETITOR_THREAD);

        Callable<Player> e = new Callable<Player>() {

            @Override
            public Player call() throws Exception {
                while (true) {
                    try {
                        VersionedValue<Player> versionedPlayer = playerService.getVersionedPlayer(ME);
                        Player player = versionedPlayer.getValue();

                        player.setCash(player.getCash() + 1);
                        player.setExp(player.getExp() + 1000);

                        Player save = playerService.compareAndSave(ME, player, versionedPlayer.getVersion());
                        return save;
                    } catch (ConcurrentModificationException ex) {
                        continue;
                    }
                }
            }
        };

        for (int i = 0; i < NUMBER_OF_COMPETITOR_THREAD; i++) {
            tasks.add(e);
        }

        List<Future<Player>> futures = competitors.invokeAll(tasks);

        for (Future<Player> f : futures) {
            f.get();
        }

        Player player = playerService.getPlayer(ME);

        assertEquals(player.getExp(), NUMBER_OF_COMPETITOR_THREAD * 1000);
        assertEquals(player.getCash(), NUMBER_OF_COMPETITOR_THREAD + 30);
    }
}
