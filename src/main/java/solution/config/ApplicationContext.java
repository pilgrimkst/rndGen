package solution.config;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class ApplicationContext {
    private static ApplicationContext context = new ApplicationContext();
    private final Injector injector = Guice.createInjector(new ApplicationModule());

    public static ApplicationContext getInstance(){
        return context;
    }

    public <T> T getInstance(Class<T> type){
        return injector.getInstance(type);
    }

    public void injectMembers(Object instance){
        injector.injectMembers(instance);
    }


}
