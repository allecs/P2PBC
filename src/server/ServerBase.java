package server;


import protocol.ResponseHeader;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Vector;

public abstract class ServerBase {
    protected Status status;
    protected ByteBuffer buf;
    protected byte[] bufLock;
    protected long current;

    protected Vector<InetSocketAddress> children;
    protected byte[] childrenLock;
    protected int maxChildrenNum;
    protected int listenPort;

    protected ServerBase(int bufSize, int maxChildrenNum, int listenPort) throws SocketException, InterruptedException {
        status = Status.NO_DATA;
        buf = ByteBuffer.allocate(bufSize);
        bufLock = new byte[0];
        current = -1;
        children = new Vector<InetSocketAddress>();
        childrenLock = new byte[0];
        this.maxChildrenNum = maxChildrenNum;
        this.listenPort = listenPort;
        start();
    }

    protected void start() throws SocketException, InterruptedException {
        Thread l = new Listener();
        l.start();
        Thread b = new Broadcaster();
        b.start();
    }

    private class Listener extends Thread{
        DatagramSocket socket;
        DatagramPacket packet;
        byte[] buffer;

        private Listener() throws SocketException {
            socket = new DatagramSocket(listenPort);
        }

        public void run(){
            while (true){
                try {
                    buffer = new byte[1024];
                    packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    new Handler(packet).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private class Handler extends Thread{
            private DatagramPacket packet;
            public Handler(DatagramPacket packet) {
                this.packet = packet;
            }

            public void run(){
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = null;
                try {
                    oos = new ObjectOutputStream(baos);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (inChildren(packet.getSocketAddress())){//seems client missed a SYN_ACK
                    try {
                        assert oos != null;
                        oos.writeObject(new ResponseHeader(ResponseHeader.retCode.SYN_ACK, current, "", "", 0, 0));
                        oos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    boolean full;
                    InetSocketAddress child1 = null;
                    InetSocketAddress child2 = null;
                    synchronized (childrenLock){
                        if (children.size() < maxChildrenNum){
                            full = false;
                            children.add((InetSocketAddress) packet.getSocketAddress());
                        }
                        else{
                            full = true;
                            child1 = children.firstElement();
                            child2 = children.lastElement();
                        }
                    }
                    if (full){
                        try {
                            assert oos != null;
                            oos.writeObject(new ResponseHeader(ResponseHeader.retCode.RECURSION, current,
                                    child1.getHostName(), child2.getHostName(), child1.getPort(), child2.getPort()));
                            oos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                makeAndSend(baos.toByteArray(), packet.getSocketAddress());
            }

            private void appendCurrent(ObjectOutputStream oos) throws IOException {
                oos.write(buffer);
            }
        }
    }


    private class Broadcaster extends Thread{
        private long lastSent;
        private byte[] toBeSent;

        private Broadcaster() {
            lastSent = -1;
        }

        public void run(){
            InetSocketAddress[] curList = new InetSocketAddress[0];
            while (true){
                if (current != lastSent){
                    synchronized (bufLock){
                        lastSent = current;
                        toBeSent = buf.array().clone();
                    }
                    synchronized (childrenLock){
                        curList = children.toArray(curList);
                    }
                    for (Object child: curList){
                        makeAndSend(toBeSent, new InetSocketAddress(((InetSocketAddress)child).
                                getHostName(), ((InetSocketAddress)child).getPort()+1));
                    }
                }
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void makeAndSend(byte[] bytes, SocketAddress socketAddress) {
        try {
            DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, socketAddress);
            new DatagramSocket().send(datagramPacket);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean inChildren(SocketAddress inetSocketAddress){
        synchronized (childrenLock){
            for (InetSocketAddress addr :children){
                if (addr.getHostName().equals(((InetSocketAddress)inetSocketAddress).getHostName()) &&
                        addr.getPort() == ((InetSocketAddress)inetSocketAddress).getPort()){
                    return true;
                }
            }
        }
        return false;
    }
}
