package solution;

import com.google.inject.Inject;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import solution.dao.QuotasDAO;
import tester.ITestTask;
import tester.QuotaExceededException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.fest.assertions.api.Assertions.assertThat;

public class TestTaskImplTest extends BasicTest{
    @Inject private final QuotasDAO backend=null;
    @Inject private final ITestTask iTestTask = null;

    private final List<Integer> createdUserQuotas = new CopyOnWriteArrayList<Integer>();

    @After
    public void cleanUpStorage(){
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
    public void testGetQuota() throws Exception {
       int existingUserId = 4;
       long existingUserQuota = 100l;
       createTestUserWithQuota(existingUserId,existingUserQuota);
       assertThat(iTestTask.getQuota(existingUserId)).isEqualTo(existingUserQuota);
    }
    @Ignore
    @Test
    public void testDestroy() throws Exception {

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
        userN= numOfUsers;
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

    private void createTestUserWithQuota(int userId, long quota) {
        assertThat(createdUserQuotas).doesNotContain(userId);
        assertThat(backend.getQuota(userId)).isNull();
        backend.incrQuota(userId, quota);
        registerNewUser(userId);
    }

    private boolean registerNewUser(int userId) {
        return createdUserQuotas.add(userId);
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
