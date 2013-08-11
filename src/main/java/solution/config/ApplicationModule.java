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
import solution.Backend;
import solution.TestTaskImpl;
import tester.ITestTask;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApplicationModule extends AbstractModule {
    public static final int NUM_OF_APPLICATION_THREADS = Runtime.getRuntime().availableProcessors();
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
//add configuration logic here
        Names.bindProperties(binder(), systemSettingsProperties);
        bind(Backend.class).in(Singleton.class);
        bind(ExecutorService.class).toInstance(systemThreadPool);
        bind(ITestTask.class).to(TestTaskImpl.class).in(Singleton.class);
    }
}
