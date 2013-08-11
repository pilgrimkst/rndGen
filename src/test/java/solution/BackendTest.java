package solution;

import org.junit.AfterClass;
import org.junit.Test;
import solution.config.ApplicationContext;

import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class BackendTest {
    private static final Backend backend = ApplicationContext.getInstance().getInstance(Backend.class);
    private static List<Integer> testUserIds = Arrays.asList(1, 2, 3, 4, 5, 6);

    @Test
    public void getQuotaShouldGetLongValueForExistentUser() throws Exception {
        long existingUserQuota = 100;
        backend.incrQuota(testUserIds.get(0), existingUserQuota);
        long userQuota = backend.getQuota(testUserIds.get(0));
        assertThat(userQuota).isEqualTo(existingUserQuota);
    }

    @Test
    public void getQuotaShouldReturnNullForNonExistentUser() throws Exception {
        Long userQuota = backend.getQuota(testUserIds.get(1));
        assertThat(userQuota).isNull();
    }

    @Test
    public void incrQuotaShouldCreateKeyForNonExistentUserBeforeIncr() throws Exception {
        long quota = 10;
        int userId = testUserIds.get(2);
        long result = backend.incrQuota(userId, quota);
        assertThat(quota).isEqualTo(result);
        long valueFromDatabase = backend.getQuota(userId);
        assertThat(valueFromDatabase).isEqualTo(quota);

    }

    @Test
    public void incrQuotaShouldIncrementForExistentUser() throws Exception {
        long quota = 1;
        int userId = testUserIds.get(3);
        backend.incrQuota(userId, quota);
        long result = backend.incrQuota(userId, 2);
        assertThat(result).isEqualTo(quota + 2);
    }

    @Test
    public void incrQuotaShouldDecrementForNegativeIncrementValues() throws Exception {
        long quota = 10;
        int userId = testUserIds.get(4);
        backend.incrQuota(userId, quota);
        long result = backend.incrQuota(userId, -2);
        assertThat(result).isEqualTo(10 - 2);
        assertThat(backend.incrQuota(userId, -20)).isLessThan(0);
    }

    @Test
    public void removeShouldRemoveUserFromBase() {
        long result = backend.incrQuota(testUserIds.get(5), 1);
        assertThat(result).isEqualTo(1);
        backend.remove(testUserIds.get(5));
        Long nullResult = backend.getQuota(testUserIds.get(5));
        assertThat(nullResult).isNull();
    }

    @AfterClass
    public static void cleanUp() {
        for (Integer id : testUserIds) {
            backend.remove(id);
        }
    }

}
