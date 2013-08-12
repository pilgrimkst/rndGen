package solution.dao;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class NodeDAO extends BaseDAO {
    public static final String MASTER_NODE_KEY = "masterNode";

    @Inject
    protected NodeDAO(@Named("db.redis.host") String redisHost, @Named("db.redis.port") int redisPort) {
        super(redisHost, redisPort);
    }

    public void setMasterNode(String nodeId) {
        connection.set(MASTER_NODE_KEY, nodeId);
    }

    public void getMasterNodeId() {
        connection.get(MASTER_NODE_KEY);
    }

    public boolean hasMasterNode() {
        return connection.get(MASTER_NODE_KEY) != null;
    }

    public int nodeCount() {
        return 0;  //To change body of created methods use File | Settings | File Templates.
    }
}
