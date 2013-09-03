package solution.dao;

import com.lambdaworks.redis.pubsub.RedisPubSubAdapter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class LoggerListener extends RedisPubSubAdapter<String, String> {
    Logger logger = Logger.getLogger(LoggerListener.class.getName());
    private final AtomicLong requests = new AtomicLong(0);

    @Override
    public void message(String channel, String message) {
        logger.fine(String.format("Channel: {%s},  message: {%s}", channel, message));
        requests.incrementAndGet();
    }

    @Override
    public void message(String channel, String pattern, String message) {
        logger.info(String.format("Channel: {%s}, pattern: {%s}  message: {%s} reqsServed: %d", channel, pattern, message, requests.incrementAndGet()));
    }
}
