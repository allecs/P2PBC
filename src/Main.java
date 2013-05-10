import server.RootServer;

import java.net.SocketException;

public class Main {

    public static void main(String[] args) throws SocketException, InterruptedException {
        new RootServer(1024, 2, 6666);
    }
}
