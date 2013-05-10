package server;

import java.net.SocketException;

/**
 * User: aolx
 * Date: 5/9/13
 * Time: 10:44 PM
 * Description:
 */
public class NodeServer extends ServerBase{


    protected NodeServer(int bufSize, int maxChildrenNum, int listenPort) throws SocketException, InterruptedException {
        super(bufSize, maxChildrenNum, listenPort);
    }

    private class NodeReader extends Thread{
        public void run(){

        }
    }
}
