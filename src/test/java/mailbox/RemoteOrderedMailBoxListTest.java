/**
 * 
 */
package mailbox;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import mailbox.entity.Mail;
import mailbox.entity.MailBox;
import mailbox.entity.MailBoxList;
import mailbox.marshaller.MailBoxListMarshaller;
import mailbox.marshaller.MailBoxMarshaller;
import mailbox.marshaller.MailMarshaller;
import mailbox.service.remote.RemoteOrderedMailBoxService;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author seoi
 */
public class RemoteOrderedMailBoxListTest {

    private static final Logger logger = LogManager.getLogger(RemoteOrderedMailBoxListTest.class);

    private static final int NUMBER_OF_MAIL = 1000;
    private static final int CAPACITY_OF_BUCKET = 100;

    private static String SOMEONE;
    private static final String ME = RandomStringUtils.randomAlphanumeric(5);

    private static final String REMOTE_INFINISPAN_SERVER_IP = "10.100.23.230:11222";

    private static RemoteOrderedMailBoxService remoteOrderedMailBoxService;

    private static Set<String> friendChannelUserIdSet = new HashSet<>(NUMBER_OF_MAIL);

    // 경쟁상태를 구현하는데 필요한 변수
    private static final int NUMBER_OF_COMPETITOR_THREAD = 10;
    private static ExecutorService competitors;
    private static ConcurrentHashMap<Long, String> friendsChannelIdMap;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        //Log4j 설정
        String logConfigurationFile = RemoteOrderedMailBoxListTest.class.getClassLoader().getResource("log4j2.xml").getPath();
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
            serializationContext.registerProtoFiles("/proto/mailbox.proto");

            serializationContext.registerMarshaller(new MailMarshaller());
            serializationContext.registerMarshaller(new MailBoxMarshaller());
            serializationContext.registerMarshaller(new MailBoxListMarshaller());
        } catch (IOException e) {
            e.printStackTrace();
        }

        RemoteCache<String, MailBoxList> mailBoxListCache = cacheManager.getCache();
        RemoteCache<String, MailBox> mailBoxCache = cacheManager.getCache();

        //RemoteOrderMailBoxService 종속성 주입 및 인스턴스 생성
        remoteOrderedMailBoxService = new RemoteOrderedMailBoxService(mailBoxListCache, mailBoxCache, CAPACITY_OF_BUCKET);
        remoteOrderedMailBoxService.createMailBox(ME);
        remoteOrderedMailBoxService.createMailBox(SOMEONE);

        for (int i = 0; i < NUMBER_OF_MAIL; i++) {
            String friendChannelUserId = RandomStringUtils.randomAlphanumeric(32);
            friendChannelUserIdSet.add(friendChannelUserId);
        }

        // 경쟁 상태를 재현할 쓰레드 풀 생성
        competitors = Executors.newFixedThreadPool(NUMBER_OF_COMPETITOR_THREAD);

        //쓰레드풀의 쓰레드마다 channelUserId 할당
        assignCompetitorsAsFriend();

    }

    private static void assignCompetitorsAsFriend() throws InterruptedException, ExecutionException {

        friendsChannelIdMap = new ConcurrentHashMap<>(NUMBER_OF_COMPETITOR_THREAD);

        List<Callable<String>> callables = new ArrayList<>(NUMBER_OF_COMPETITOR_THREAD);

        Callable<String> callable = new Callable<String>() {

            @Override
            public String call() throws Exception {
                long threadId = Thread.currentThread().getId();
                String channelUserId = RandomStringUtils.randomAlphanumeric(32);
                synchronized (friendsChannelIdMap) {
                    friendsChannelIdMap.put(threadId, channelUserId);
                }
                return channelUserId;
            }

        };

        for (int i = 0; i < NUMBER_OF_COMPETITOR_THREAD; i++) {
            callables.add(callable);
        }

        List<Future<String>> invokeAll = competitors.invokeAll(callables);
        for (Future<String> f : invokeAll) {
            f.get();
        }
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
    public void setUp() throws Exception {
        SOMEONE = RandomStringUtils.randomAlphanumeric(5);
        remoteOrderedMailBoxService.createMailBox(SOMEONE);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

    /**
     * 여러명의 친구들이 나에게 메일을 보내는 시나리오
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testSend() throws InterruptedException {

        for (String channelUserId : friendChannelUserIdSet) {
            Mail mail = new Mail();

            mail.setFromChannelUserId(channelUserId);
            mail.setAttachment(1);
            mail.setQuantity(100);
            Calendar calendar = Calendar.getInstance();
            mail.setSent(calendar.getTimeInMillis());
            calendar.add(Calendar.HOUR, 360);
            mail.setExpiring(calendar.getTimeInMillis());
            mail.setType(0);
            remoteOrderedMailBoxService.send(ME, mail);
        }

        assertEquals((int) Math.ceil((float) NUMBER_OF_MAIL / (float) CAPACITY_OF_BUCKET), remoteOrderedMailBoxService.getMailBoxes(ME).size());
    }

    /**
     * 멀티쓰레드 환경에서 여러명의 친구들이 SOMEONE에게 메일을 보내는 시나리오
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testConcurrentSend() throws InterruptedException, ExecutionException {

        List<Callable<Mail>> tasks = new ArrayList<>(NUMBER_OF_MAIL);
        Callable<Mail> e = new Callable<Mail>() {

            @Override
            public Mail call() throws Exception {
                Mail mail = new Mail();

                mail.setFromChannelUserId(friendsChannelIdMap.get(Thread.currentThread().getId()));
                mail.setAttachment(1);
                mail.setQuantity(100);
                Calendar calendar = Calendar.getInstance();
                mail.setSent(calendar.getTimeInMillis());
                calendar.add(Calendar.HOUR, 360);
                mail.setExpiring(calendar.getTimeInMillis());
                mail.setType(0);

                while (true) {
                    try {
                        Mail addedMail = remoteOrderedMailBoxService.send(SOMEONE, mail);
                        return addedMail;
                    } catch (ConcurrentModificationException e) {
                        continue;
                    }
                }
            }
        };

        for (int i = 0; i < NUMBER_OF_MAIL; i++) {
            tasks.add(e);
        }

        List<Future<Mail>> futures = competitors.invokeAll(tasks);

        for (Future<Mail> f : futures) {
            f.get();
        }

        //        System.out.println(remoteOrderedMailBoxService.getMails(SOMEONE));
        //        System.out.println(remoteOrderedMailBoxService.getMailBoxes(SOMEONE));

        assertEquals(NUMBER_OF_MAIL, remoteOrderedMailBoxService.getMails(SOMEONE).size());
        assertEquals((int) Math.ceil((float) NUMBER_OF_MAIL / (float) CAPACITY_OF_BUCKET), remoteOrderedMailBoxService.getMailBoxes(SOMEONE).size());
    }

    @Test
    public void testGetList() {
        List<Mail> mails = remoteOrderedMailBoxService.getMails(ME);
        logger.info("내 메일들:{}", mails);
        assertEquals(NUMBER_OF_MAIL, mails.size());
    }

    /**
     * 나에게 온 메일들을 순서를 섞어서 읽어보는 시나리오
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testConcurrentlyReadOne() throws InterruptedException, ExecutionException {

        Set<String> mailIdSet = new HashSet<>(NUMBER_OF_MAIL);
        for (Mail m : remoteOrderedMailBoxService.getMails(ME)) {
            mailIdSet.add(m.getId());
        }

        String[] shuffledMailIds = shuffle(mailIdSet);

        List<Callable<Mail>> tasks = new ArrayList<>(NUMBER_OF_MAIL);

        for (final String mailId : shuffledMailIds) {
            Callable<Mail> e = new Callable<Mail>() {

                @Override
                public Mail call() throws Exception {
                    while (true) {
                        try {
                            Mail addedMail = remoteOrderedMailBoxService.readOne(ME, mailId);
                            return addedMail;
                        } catch (ConcurrentModificationException e) {
                            continue;
                        }
                    }
                }

            };

            tasks.add(e);
        }

        List<Future<Mail>> futures = competitors.invokeAll(tasks);

        for (Future<Mail> f : futures) {
            f.get();
        }

        //        for (String mailId : shuffledMailIds) {
        //            Mail readOne = remoteOrderedMailBoxService.readOne(ME, mailId);
        //            System.out.println("[읽은메일ID]" + readOne.getId());
        //        }

        assertEquals(0, remoteOrderedMailBoxService.getMails(ME).size());
    }

    private String[] shuffle(Set<String> stringSet) {
        Object[] objectArray = stringSet.toArray();
        String[] array = Arrays.copyOf(objectArray, objectArray.length, String[].class);

        Random random = new Random();

        int randomIndex1, randomIndex2;
        int i;
        String tmp;

        for (i = 0; i < array.length; i++) {
            randomIndex1 = random.nextInt(array.length) % array.length;
            randomIndex2 = random.nextInt(array.length) % array.length;
            tmp = array[randomIndex1];
            array[randomIndex1] = array[randomIndex2];
            array[randomIndex2] = tmp;
        }

        return array;
    }
}
