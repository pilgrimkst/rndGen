package solution;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tester.ITestTask;
import tester.QuotaExceededException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static org.fest.assertions.api.Assertions.assertThat;

public class TestTaskImplTest extends BasicTest{
    public static final int WAIT_TO_SYNC = 1000;
    @Inject
    private Logger logger;

    @Inject private ITestTask iTestTask;

    private final List<Integer> createdUserQuotas = new CopyOnWriteArrayList<Integer>();

    @Before
    public void cleanUp() {
        clearStorage();
    }

    @After
    public void tearDown(){
        clearStorage();
    }
    @Test
    public void getRandomShouldReturnNonNegativeNumberForUserWithQuota() throws Exception {
        int userId = 1;
        long quota = 10;
        createTestUserWithQuota(userId, quota);
        int numOfRequests = 10;
        while(numOfRequests-->0){
            assertValidRandomNumber(userId);
            assertQuotaEquals(userId,numOfRequests);
        }
        assertQuotaEquals(userId, 0l);
    }

    @Test
    public void getRandomShouldCreateNewClientForNonExistentUserId() throws Exception {
        int nonExistentUser = 0;
        assertValidRandomNumber(nonExistentUser);
    }

    @Test
    public void getRandomShouldThrowExceptionForUserWithZeroQuota() throws Exception{
        int userId = 2;
        createTestUserWithQuota(userId,1);
        assertValidRandomNumber(userId);
        assertQuotaEquals(userId,0);
        assertExceptionOnGetRandomQuery(userId);
    }

    @Test
    public void testAddQuota() throws Exception {
        int userId = 3;
        createTestUserWithQuota(userId,0);
        assertExceptionOnGetRandomQuery(userId);
        assertQuotaExceededOnAddQuota(userId, 2);
        assertValidRandomNumber(userId);
    }

    @Test
    public void testCreateUserWithQuota() throws Exception{
        int userId = 5;
        int newUserQuota = 15;
        createTestUserWithQuota(userId,newUserQuota);
        assertThat(iTestTask.getQuota(userId)).isEqualTo(newUserQuota);
        createTestUserWithQuota(6,3);
        assertThat(iTestTask.getQuota(6)).isEqualTo(3);

    }

    @Test
    public void testGetQuota() throws Exception {
       int existingUserId = 4;
       long existingUserQuota = 100l;
       createTestUserWithQuota(existingUserId,existingUserQuota);
       assertThat(iTestTask.getQuota(existingUserId)).isEqualTo(existingUserQuota);
    }

    @Test
    public void testClearAll() throws Exception {
        int defaultQuotaForNewUser = 10;
        int numOfUsers = 100;
        int userIdFrom = 1000;
        int userN = numOfUsers;
        List<Integer> userIds = new ArrayList<Integer>(numOfUsers);
        Random r = new Random();
        while(userN-->0){
            int userId = userIdFrom+userN;
            long quota = r.nextInt(1000);
            userIds.add(userId);
            createTestUserWithQuota(userId,quota);
        }
        iTestTask.clearAll();
        Thread.sleep(WAIT_TO_SYNC);
        for(Integer userId:userIds){
            assertThat(iTestTask.getQuota(userId)).isEqualTo(defaultQuotaForNewUser);
        }
    }

    private void assertExceptionOnGetRandomQuery(int userId) {
        try{
            iTestTask.getRandom(userId);
            assertThat(false);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(QuotaExceededException.class);
        }
    }

    private void createTestUserWithQuota(int userId, long newQuota) {
        long userQuota = iTestTask.getQuota(userId);
        iTestTask.addQuota(userId, -1*(userQuota - newQuota));
    }

    private void assertQuotaExceededOnAddQuota(int userId, int incrementBy) {
        long currentQuota = iTestTask.getQuota(userId);
        iTestTask.addQuota(userId,incrementBy);
        assertThat(iTestTask.getQuota(userId)).isEqualTo(currentQuota+incrementBy);
    }

    private void assertQuotaEquals(int userId, long quota) {
        assertThat(iTestTask.getQuota(userId)).isEqualTo(quota);
    }

    private void assertValidRandomNumber(int userId) throws QuotaExceededException {
        try{
            assertThat(iTestTask.getRandom(userId)).isGreaterThanOrEqualTo(0);
        }catch (Exception e){
            assertThat(false);
        }
    }
}
