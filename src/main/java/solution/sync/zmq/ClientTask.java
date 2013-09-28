package solution.sync.zmq;

import org.jeromq.ZMQ;

/**
 * Asynchronous client-to-server (DEALER to ROUTER)
 *
 * This example contains clients and server in order to easily start and stop
 * the demo.
 *
 * They are working independently and communicate only by using the tcp
 * connections. They conceptually act as separate processes.
 *
 * @author Raphaël P. Barazzutti
 */

// The client task connects to the server, then sends a request every second
// while printing incoming messages as they arrive
class ClientTask implements Runnable {
    final private int id;

    public ClientTask(int id) {
        this.id = id;
    }

    public void run() {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket client = context.socket(ZMQ.DEALER);
        String identity = "worker-" + id;
        client.setIdentity(identity.getBytes());
        client.connect("tcp://localhost:5555");
        System.out.println("Client " + identity + " started");
        ZMQ.Poller poller = context.poller(1);
        poller.register(client, ZMQ.Poller.POLLIN);
        int requestNbr = 0;
        while (true)
            for (int i = 0; i < 100; i++) {
                poller.poll(10000);
                if (poller.pollin(0)) {
                    byte msg[] = client.recv(0);
                    System.out.println("Client " + identity + " received "
                            + new String(msg));
                }
                System.out.println("Req #" + (++requestNbr) + " sent");
                client.send(("request " + requestNbr).getBytes(), 0);
            }
    }

}

