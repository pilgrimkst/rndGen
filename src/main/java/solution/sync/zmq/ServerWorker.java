package solution.sync.zmq;


import org.jeromq.ZMQ;

import java.util.Random;

// The server worker receives messages and replies by re-sending them a random
// number of times (with random delays between replies)
class ServerWorker implements Runnable {
    private ZMQ.Context context;
    final private int id;

    public ServerWorker(ZMQ.Context context, int id) {
        super();
        this.id = id;
        this.context = context;
    }

    public void run() {
        Random randomGenerator = new Random();
        ZMQ.Socket worker = context.socket(ZMQ.DEALER);
        worker.connect("inproc://backend");
        System.out.println("Server worker " + id + " started");
        while (true) {
            byte id[] = worker.recv(0);
            byte msg[] = worker.recv(0);
            System.out.println("Server worker " + id + " received "
                    + new String(msg) + " from " + new String(id));
            // sending 0..4 replies
            for (int i = 0; i < randomGenerator.nextInt(5); i++) {
                try {
                    // sleeping 1s or 1s/2 or 1s/3 .. 1s/9
                    Thread.sleep(1000 / (1 + randomGenerator.nextInt(8)));
                    worker.send(id, ZMQ.SNDMORE);
                    worker.send(msg, 0);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }

        }

    }
}

