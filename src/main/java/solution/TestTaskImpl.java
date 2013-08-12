package solution;

import com.google.inject.Inject;
import solution.sync.SyncService;
import tester.ITestTask;
import tester.QuotaExceededException;

import java.util.Random;

public class TestTaskImpl implements ITestTask {
    public static final int MAX_RANDOM_NUMBER = 999;
    //Set counter value, that will be sent to backend once a second
    // change this variable(or pool of variables) whenever request goes
    // We need to sync state with backend (maybe before sending data to server) to avoid
    // unconsistent data if some other node will change data
    // need to implement fast and simple way to notify nodes for exceeding limits

    //  Ваша задача ­ написать приложение, реализующее переданный во вложении интерфейс
//    Метод destroy()гарантированно вызывается перед завершением приложения.
//    Требования к системе:
//            1. Запросы к вашей реализации интерфейса будут идти в 100 потоков, и класс
//    должен безошибочно обрабатывать как можно больше запросов в секунду.
//    2. Информация о зарегистрированных клиентах и остатках их квот должна быть
//    персистентной, то есть сохраняться в базе данных между запусками программы.
//    При некорректном завершении программы допускается частичная потеря данных,
//    однако не более чем за последние 3 секунды. При корректном завершении
//    программы ошибки должны быть исключены.
//    3. Программа должна быть многопоточной и распределённой (то есть допускать
//            возможность запуска нескольких экземпляров программы на разных машинах,
//                                                             работающих с единой базой данных).
//            4. Допустимо отставание программы не более чем на 3 секунды (то есть возможна
//            отдача случайных чисел вместо исключений в течение 3 секунд после того, как
//                                                                                 была израсходована вся квота, а также отдача исключений вместо случайных
//                                                                                 чисел в течение 3 секунд после регистрации клиента или увеличения его квоты).
//    Статистика, выдаваемая методом getQuota(), также может отставать на
//    интервал до 3 секунд.
//    5. В базе может быть зарегистрировано до миллиона клиентов. Специфика запросов
//    такова, что 80% запросов будут совершаться от лица одного (самого крупного)
//    клиента, ещё 19% запросов будут совершаться от лица 100 клиентов “поменьше”, и
//    1% запросов будет совершаться от лица остальных клиентов. Интенсивность
//    запросов одного конкретного клиента может меняться в ходе работы сервиса.
//            6. Помимо интерфейса, вам передаётся приложение, совершающее запросы к вашей
//    реализации интерфейса в 100 потоков и выводящее в консоль производительность
//    системы за последнюю секунду.

    @Inject
    private final SyncService syncService = null;

    private final Random generator = new Random();

    @Override
    public int getRandom(final int userId) throws QuotaExceededException {
        if (getQuota(userId) > 0) {
            addQuota(userId, -1);
            return generator.nextInt(MAX_RANDOM_NUMBER);
        }
        throw new QuotaExceededException();
    }

    @Override
    public void addQuota(final int userId, final long quota) {
        syncService.addQuota(userId, quota);
    }

    @Override
    public long getQuota(final int userId) {
        return syncService.getQuota(userId);
    }

    @Override
    public void destroy() {
        syncService.persistLocalChanges();
    }

    @Override
    public void clearAll() {
        syncService.cleanUpUserQuotas();
    }   
}
