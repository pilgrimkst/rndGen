/*
 * Created by IntelliJ IDEA.
 * User: User
 * Date: 10.08.13
 * Time: 12:30
 */
package solution.config;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import solution.TestTaskImpl;
import solution.dao.NodeDAO;
import solution.dao.QuotasDAO;
import solution.sync.SyncService;
import solution.sync.SyncServiceNode;
import tester.ITestTask;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApplicationModule extends AbstractModule {
    public static final int NUM_OF_APPLICATION_THREADS = Runtime.getRuntime().availableProcessors()-1;
    private final ExecutorService systemThreadPool = Executors.newFixedThreadPool(NUM_OF_APPLICATION_THREADS);
    private Properties systemSettingsProperties = new Properties();
    public ApplicationModule(){
        try {
            systemSettingsProperties.load(ApplicationModule.class.getResourceAsStream("application_config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void configure() {
        Names.bindProperties(binder(), systemSettingsProperties);
        bindConstant().annotatedWith(Names.named("nodeId")).to(UUID.randomUUID().toString());
        bind(QuotasDAO.class).in(Singleton.class);
        bind(NodeDAO.class).in(Singleton.class);
        bind(ExecutorService.class).toInstance(systemThreadPool);
        bind(ITestTask.class).to(TestTaskImpl.class).in(Singleton.class);
        bind(SyncService.class).to(SyncServiceNode.class).in(Singleton.class);
    }
}
