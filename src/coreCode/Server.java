package coreCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @program: NewJavaProject
 * @description: s
 * @author: Mr.Hu
 * @create: 2020-10-07 21:29
 */
public class Server {
    static int serverPort = 4242;

    public void activeServer() {
        try {
            // 创建4242端口服务程序
            ServerSocket serverSock = new ServerSocket(serverPort);
            System.out.println("server ready");
            // 创建线程池，将线程的创建和运行交给它处理
            ExecutorService executorService = Executors.newCachedThreadPool();
            // 不断接受新的连接请求
            while (true) {
                // 如果有client请求连接，则返回两者间的socket对象
                Socket socket = serverSock.accept();
                System.out.println(SocketTool.getIpAndPort(socket) + " connect");
                // 新建线程，处理该client的所有事务，主线程还要接收新的处理问题
                executorService.submit(new clientSocket(socket));
            }
        } catch (IOException e) {
            System.out.println("server Init failed or client accept failed");
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.activeServer();
    }
}

class clientSocket implements Runnable {
    // 该线程处理对client的输入和输出
    Socket socket;
    PrintWriter writer;
    BufferedReader reader;
    static int exitCode = "bye".hashCode();
    static String unNormalExitMsg = "Socket Exception Exit";
    static String normalExitMsg = "normal exit";

    public clientSocket(Socket socket) {
        this.socket = socket;
        try {
            // 初始化读取流和输入流
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        String message = null;
        boolean isClose = false;
        int clientExitCode = -1;
        // 不断监听来自客户端的额消息
        while (!isClose) {
            try {
                //如果要传文件，就新开一个线程专门传（字节流），传完就结束，read在流结束时会读取到-1
                // 线程一般卡在这里, 这是阻塞函数，不像其他read方法一样当到达流的末尾时返回-1，而readline到达末尾也不会返回null，很奇怪
                //即客户发完一个数据，read方法会在接收完后不阻塞，返回-1，但readline只有当数据流异常或另一端close时才返回null
                message = reader.readLine();  //或者先判定  reader.ready()

                //至于网上说的发送 UrgentData，会在连续发送一定次数时报错

		/* 这串代码的reader是dataInputStream，可以实现不阻塞，调用了avaliable， 如果不阻塞，那么可以通过上次发消息的时间间隔判断客户端已退出，System有个方法
        message = null;
        byte[] buffer;
        buffer = new byte[reader.available()];
        if (buffer.length != 0) {
          // 读取缓冲区
          reader.read(buffer);
          // 转换字符串
          message = new String(buffer).trim();
        }*/
            } catch (IOException e) {
                // 进入该异常有两种情况：客户端连接异常断开，服务器连接断开（不太可能）
                // 并且我在客户端程序中已经编写了，关闭程序回复服务器"bye"的信息，所以此处只可能是客户端异常关闭
                isClose = true;
                message = unNormalExitMsg;
            }
            if (message != null && !isClose) {
                // 客户端发送"bye"过来就代表连接结束
                try {
                    // 看看是不是"bye"的hashCode
                    clientExitCode = Integer.parseInt(message.trim());
                } catch (NumberFormatException e) {
                    // 可能会有异常
                    clientExitCode = -1;
                }
                if (clientExitCode == exitCode) {
                    isClose = true;
                    message = normalExitMsg;
                } else {
                    System.out.println(SocketTool.getIpAndPort(socket) + " says " + message);
                    // 给客户端一条建议
                    writer.println(Response.getAdvice());
                    writer.flush();
                }
            }
        }
        finish(message);
    }

    private void finish(String msg) { // 关闭连接
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(SocketTool.getIpAndPort(socket) + " " + msg);
    }
}

class Response { // 调用该类的方法返回一个随机字符串，即advise
    static String[] adviceList = {
            "Take smaller bites",
            "Go for the tight jeans. No they do NOT make you look fat",
            "One word: inappropriate",
            "Just for today, be honest.  Tell your boss what you *really* think",
            "You might want to rethink that haircut"
    };

    public static String getAdvice() {
        int random = (int) (Math.random() * adviceList.length);
        return adviceList[random];
    }
}

class SocketTool {
    public static String getIpAndPort(Socket socket) {
        return Arrays.toString(socket.getInetAddress().getAddress()) + ":" + socket.getPort();
    }
}
