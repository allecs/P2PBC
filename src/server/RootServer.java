package server;

import java.net.SocketException;

/**
 * User: aolx
 * Date: 5/9/13
 * Time: 10:44 PM
 * Description:
 */
public class RootServer extends ServerBase{


    public RootServer(int bufSize, int childrenNum, int listenPort) throws SocketException, InterruptedException {
        super(bufSize, childrenNum, listenPort);
        new RootReader().start();
    }

    private class RootReader extends Thread{
        public void run(){
            while (true){
                synchronized (bufLock){
                    current++;

                }
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
