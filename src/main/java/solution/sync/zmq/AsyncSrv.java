package solution.sync.zmq;

// The main thread of the Java program, it starts three clients then starts a
// server
public class AsyncSrv {
    public static void main(String args[]) {
        // starting three clients
        for (int i = 0; i < 3; i++)
            (new Thread(new ClientTask(i))).start();

        // starting server
        new Thread(new ServerTask()).start();
    }
}
