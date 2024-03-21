

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class SSLThreadPooledServer implements Runnable{

    protected int          serverPort   = 8443;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    
    protected ExecutorService threadPool =
        Executors.newFixedThreadPool(20);

    public SSLThreadPooledServer(int port){
        this.serverPort = port;
    }

    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        System.setProperty("javax.net.ssl.keyStore","keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword","password");
        openServerSocket();
        while(! isStopped()){
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException(
                    "Error accepting client connection", e);
            }
            
            this.threadPool.execute(new WorkerRunnable(clientSocket));
            
        }
        this.threadPool.shutdown();
        System.out.println("Server Stopped.") ;
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop()
    {
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try
        {
        	//Create a SSLServersocket
            SSLServerSocketFactory factory=(SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            this.serverSocket=(SSLServerSocket) factory.createServerSocket(this.serverPort);
            
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port ", e);
        }
    }
}