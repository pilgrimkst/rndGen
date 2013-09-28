package solution.sync.zmq;

import org.jeromq.ZMQ;

// The server task uses a pool of workers to handle the messages coming from
// clients.
//
// The main server thread forwards messages between the front-end (connected to
// clients) and the back-end (connected to workers)
//
// The workers handle one request at a time, but a client might have its
// messages handled by more than one worker
class ServerTask implements Runnable {
    @Override
    public void run() {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket frontend = context.socket(ZMQ.ROUTER);
        frontend.bind("tcp://*:5555");

        ZMQ.Socket backend = context.socket(ZMQ.DEALER);
        backend.bind("inproc://backend");

        for (int i = 0; i < 5; i++)
            (new Thread(new ServerWorker(context, i))).start();

        ZMQ.Poller poller = context.poller(2);
        poller.register(frontend, ZMQ.Poller.POLLIN);
        poller.register(backend, ZMQ.Poller.POLLIN);

        // It is possible to easily connect frontend to backend using a queue
        // device
        // ZMQQueue queue = new ZMQQueue(context, frontend, backend);
        // queue.run();
        //
        // Doing it manually gives a better understanding of the mechanisms
        // (it's a tuto) and might be useful in debugging
        while (true) {
            poller.poll();
            if (poller.pollin(0)) {
                byte[] id = frontend.recv(0);
                byte[] msg = frontend.recv(0);
                System.out.println("Server received " + new String(msg)
                        + " id " + new String(id));

                backend.send(id, ZMQ.SNDMORE);
                backend.send(msg, 0);
            }
            if (poller.pollin(1)) {
                byte[] id = backend.recv(0);
                byte[] msg = backend.recv(0);
                System.out.println("Sending to frontend " + new String(msg)
                        + " id " + new String(id));
                frontend.send(id, ZMQ.SNDMORE);
                frontend.send(msg, 0);
            }
        }
    }
}
