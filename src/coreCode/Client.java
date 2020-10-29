package coreCode;
/*
此程序的拓展思路，服务器端程序已经可以在支持并发连接了，并且本程序还可以多开窗口
那么可以：以此为基础建立一个多人聊天室，只需要添加几点功能：
窗体上显示在线的所有人，点击某人向服务器发送与该用户连接的消息，服务器转接给被选的用户的窗体，问其是否愿意连接
然后同意的话，就建立了两者间虚拟的连接，即两者间的发送的中转站是服务器！！
当然还要增加选择发送消息对象的窗体组件，是发送给服务器还是另一个用户呢

服务器端当然要增加几点功能，并且发送给客户端的消息，还要附带身份标识，即是服务器本身发送的，还是其他用户发来的
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

/**
 * @program: NewJavaProject
 * @description: sa
 * @author: Mr.Hu
 * @create: 2020-10-08 12:03
 */
public class Client {
    public static void main(String[] args) {
        ClientDisplay t = new ClientDisplay();
        t.initGui();
        t.initSocket();
    }
}

class ClientDisplay {
    static String serverAddress = "127.0.0.1";
    static int serverPort = 4242;
    static int frameInstance = 0; // 同时运行的窗口的数量
    static String initSuccessMsg = " Socket Init Done";
    static String initFailMsg = " Socket CONNECT Failed, auto close...";
    Socket socket;
    JTextField clientMessage; // 客户端发送信息框
    PrintWriter writer; // 发送流
    JTextArea serverResponse; // 来自服务器的回复
    BufferedReader reader; // 读取流
    Thread threadListener; // 负责监听服务器的回复
    String frameID; // 窗体标识
    JFrame jframe;

    public void initGui() {
        frameInstance++;
        frameID = "Chat Room" + frameInstance;
        jframe = new JFrame(frameID);
        JPanel jPanel = new JPanel(); // 用来放零散组件
        clientMessage = new JTextField(20);
        serverResponse = new JTextArea(15, 50);
        serverResponse.setEditable(false); // 不可编辑

        JButton btnSend = new JButton("send"); // 发送信息按钮
        Font font = new Font("微软雅黑", Font.PLAIN, 20); // 字体
        btnSend.setFont(font);
        clientMessage.setFont(font);
        btnSend.addActionListener(new BtnListener()); // 发送给服务器

        JButton btnNewChatRoom = new JButton("Create New Chat Room"); // 打开新的聊天窗口
        btnNewChatRoom.addActionListener(new NewFrame());
        btnNewChatRoom.setFont(font);

        JScrollPane jScrollPane = new JScrollPane(serverResponse); // 滚动框
        jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        jPanel.add(jScrollPane);
        jPanel.add(clientMessage);
        jPanel.add(btnSend);
        jPanel.add(btnNewChatRoom);
        jframe.getContentPane().add(BorderLayout.CENTER, jPanel);

        jframe.setSize(650, 500);
        jframe.setLocationRelativeTo(null);
        jframe.setVisible(true);
        jframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // 关闭窗口后还要给服务器发送结束标志
        jframe.addWindowListener(new MyFrameListener());
    }

    public void initSocket() {
        boolean isConnected = true;
        try {
            // 与特定IP的计算机的指定端口程序连接
            socket = new Socket(serverAddress, serverPort);
            // socket连接的读取和输入跟文件连接本质上一样的，只不过流的源头不一样
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            System.out.println(frameID + initSuccessMsg);
        } catch (IOException e) {
            System.out.println(frameID + initFailMsg);
            isConnected = false;
        }
        if (isConnected) {
            createListenerThread();
        } else {
            jframe.dispose();
            frameInstance--;
        }
    }

    private void createListenerThread() {
        // 开启监听服务器的回复线程
        Runnable runnable = new ResponseListener();
        threadListener = new Thread(runnable);
        threadListener.start();
    }

    private void closeConnection(String msg) {
        if (socket != null && socket.isConnected()) {
            try {
                socket.close();
                socket = null;
                System.out.println(frameID + " " + msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ResponseListener implements Runnable { // 监听线程，实现Runnable的run方法
        @Override
        public void run() { // 这段代码和服务器的监听输入一样的
            String message = null;
            boolean isClose = false; // 退出循环
            while (!isClose) {
                try {
                    // 读取来自服务器的回复，该线程一般会一直卡在这个地方等待回复，其他部分执行很快的
                    message = reader.readLine();
                } catch (IOException e) {
                    // 有异常的话，只有两种情况：客户端socket关闭了、服务器端关闭了
                    // 因为一般会停在reader.readline，连接关了，所以读取必有问题
                    isClose = true;
                    closeConnection("Socket Thread Exception Exit"); // 这条消息输出说明服务器端有问题
                }
                if (message != null && !isClose) {
                    System.out.println(
                            Arrays.toString(socket.getInetAddress().getAddress()) // socket另一端的IP和端口
                                    + ":"
                                    + socket.getPort()
                                    + " says to "
                                    + frameID
                                    + " : "
                                    + message);
                    serverResponse.append(message + "\n"); // 加载JTextArea里面
                }
            }
        }
    }

    class BtnListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 发送消息个服务器
            //每次发送一行字，这时候选择Writer是个不错的选择
            if (!"".equals(clientMessage.getText())) {
                // writer的方法和平常的用sys.out一样, 注意读取的时候是reader.readLine，所以必须换行输出
                writer.println(clientMessage.getText());
                writer.flush(); // 清空缓冲区, 如果之前构造方法设置了true，那么每次print会自动刷新
                clientMessage.setText(null); // 输入框清空
                clientMessage.requestFocus();
            }
        }
    }

    class MyFrameListener extends WindowAdapter {
        String exitCodeStr = "bye";

        @Override
        // 电话已JFrame右上角关闭时候的事件
        public void windowClosing(WindowEvent e) {
            super.windowClosing(e);
            writer.println(exitCodeStr.hashCode()); // 给给服务器发送"bye"的hashCode,那边会自动断开连接
            // 如果单纯发送"bye"就关闭的话，就可能不小心发送bye,意外关闭
            writer.flush();
            closeConnection("Client Socket Exit"); // 这条消息输出说明客户端socket关了
            ClientDisplay.frameInstance--;
            if (ClientDisplay.frameInstance == 0) {
                System.exit(0); // 退出程序
            }
        }
    }
}

class NewFrame implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        // 新建窗体
        ClientDisplay newFrame = new ClientDisplay();
        newFrame.initGui();
        newFrame.initSocket();
    }
}
