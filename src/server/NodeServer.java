package server;

import protocol.Request;
import protocol.Response;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * User: aolx
 * Date: 5/9/13
 * Time: 10:44 PM
 * Description:
 */
public class NodeServer extends ServerBase{
    public NodeReader nodeReader;
    public Connector connector;
    private DatagramSocket controlSocket = null;
    private DatagramSocket dataSocket = null;
    boolean connected = false;
    private InetSocketAddress server = null;

    protected NodeServer(int bufSize, int maxChildrenNum, int listenPort, InetSocketAddress server, int controlPort, int dataPort) throws SocketException, InterruptedException {
        super(bufSize, maxChildrenNum, listenPort);
        this.controlSocket = new DatagramSocket(controlPort);
        this.dataSocket = new DatagramSocket(controlPort + 1);//TODO ungrade
        this.server = server;
    }

    public NodeServer(int bufSize, int maxChildrenNum, int listenPort, InetSocketAddress server, int controlPort) throws SocketException, InterruptedException {
        this(bufSize, maxChildrenNum, listenPort, server, controlPort, controlPort + 1);
    }

    public void run(){
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                connector.interrupt();
            }
        }));
        connector = new Connector();
        connector.start();
        nodeReader = new NodeReader();
        nodeReader.start();



    }

    protected class NodeReader extends Thread{
        private byte[] data;
        private DatagramSocket socket;
        private DatagramPacket packet;

        private NodeReader() {
            this.socket = dataSocket;
        }

        public void run(){
            while (true){
                data = new byte[1024];
                packet = new DatagramPacket(data, data.length);
                try {
                    socket.receive(packet);
                    synchronized (bufLock){
                        current++;
                        buf = ByteBuffer.wrap(data);
                    }
                    System.out.println("recv data on "+socket.getLocalAddress()+":"+socket.getLocalPort()+
                            " from "+packet.getAddress()+":"+packet.getPort());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected class Connector extends Thread{
        InetSocketAddress inetSocketAddress = null;

        public Connector(){
            this.inetSocketAddress = server;
        }

        public void start(){
            try {
                connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        protected void connect() throws IOException {
            Queue<InetSocketAddress> queue = new LinkedList<InetSocketAddress>();
            queue.add(inetSocketAddress);

            while(!queue.isEmpty()){
                InetSocketAddress[] children = connectInter(queue.poll());
                if(connected) return;
                for (InetSocketAddress address: children){
                    queue.add(address);
                }
            }
            throw new IOException("Connect to server failed");
        }

        protected InetSocketAddress[] connectInter(InetSocketAddress addr) throws IOException {
            byte[] bufsend = null;
            byte[] bufrecv = new byte[1024];

            Request request = new Request(Request.reqCode.SYN, 0);
            ByteArrayOutputStream baos =
                    new ByteArrayOutputStream(1024);
            ObjectOutputStream out =
                    new ObjectOutputStream(baos);
            out.writeObject(request);
            out.close();
            baos.close();

            bufsend = baos.toByteArray();

            DatagramPacket sendPacket = new DatagramPacket(bufsend, bufsend.length, addr);
            controlSocket.send(sendPacket);

            DatagramPacket recvPacket = new DatagramPacket(bufrecv, bufrecv.length, addr);
            controlSocket.receive(recvPacket);

            Response response = null;
            ByteArrayInputStream bais =
                    new ByteArrayInputStream(recvPacket.getData());
            ObjectInputStream in = new ObjectInputStream(bais);
            try {
                response = (Response) in.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            in.close();
            bais.close();

            if (response.ret == Response.retCode.SYN_ACK){
                connected = true;
                server = addr;
                return null;
            }

            if (response.ret == Response.retCode.RECURSION){
                return response.children;
            }

            if (response.ret == Response.retCode.RST){
                throw new IOException("Connect to server failed");
            }

            return null;
        }

        protected void disconnect() throws IOException {
            if (connected){
                byte[] bufsend = null;

                Request request = new Request(Request.reqCode.RST, 0);
                ByteArrayOutputStream baos =
                        new ByteArrayOutputStream(1024);
                ObjectOutputStream out =
                        new ObjectOutputStream(baos);
                out.writeObject(request);
                out.close();
                baos.close();

                bufsend = baos.toByteArray();

                DatagramPacket sendPacket = new DatagramPacket(bufsend, bufsend.length, server);
                controlSocket.send(sendPacket);
            }
        }

        public void run(){
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() {
                    try {
                        disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }));

        }
    }
}
