package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Client
 * Created by lddong
 * Date 12/28/2022 - 4:09 PM
 * Description: ...
 */

public class Client {
    private int port;
    private String hostName;
    private String userName;
    static File[] listOfFile;
    private String path = ".";
    private Client.UI ui;
    boolean isChoosed, isDisconnected = false;
    private String curPath = path;

    public Client() {
        this.isChoosed = false;
    }
    public boolean isChoosed() {
        return this.isChoosed;
    }
    public void setChoosed(boolean choosed) {
        this.isChoosed = choosed;
    }
    public void execute() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ui = new UI();
            }
        });

    }
    void run() {
        try {
            Socket socket = new Socket(hostName, port);
            isDisconnected = false;
            System.out.println("Connected to the chat server");
            setInfo(path);
            new ReceivedThread(socket, this).start();
            new SendThread(socket, this).start();
        } catch(UnknownHostException ex) {
            System.out.println("Server is not found: " + ex.getMessage());
        } catch(IOException ex) {
            System.out.println("I/O Error: " + ex.getMessage());
        }
    }
    void setUserName(String username) {
        this.userName = username;
    }
    String getUserName() {
        return this.userName;
    }

    class UI implements ActionListener {
        final private JFrame frame;
        ActionListener _this = this;
        JTextField p, p2;
        public UI() {
            frame = new JFrame("CLIENT");
            //Create and set up the window.
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();

            //Create a nd set up the content pane.
            initUI();

            //Display the window.
            frame.setVisible(true);
        }
        void initUI() {
            Thread t = new Thread() {
                public void run() {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                JPanel main = new JPanel();
                                main.setLayout(new BorderLayout());

                                JPanel heading = new JPanel();
                                JLabel h = new JLabel("ENTER INFO OF SERVER TO CONNECT");
                                heading.add(h);
                                main.add(heading);

                                JPanel center = new JPanel();
                                center.setLayout(new BoxLayout(center, BoxLayout.PAGE_AXIS));

                                JPanel port = new JPanel();
                                port.setLayout(new BoxLayout(port, BoxLayout.LINE_AXIS));
                                JLabel l = new JLabel("PORT");
                                p = new JTextField();
                                p.setPreferredSize(new Dimension(200, 10));
                                port.add(Box.createRigidArea(new Dimension(10, 0)));
                                port.add(l);
                                port.add(Box.createRigidArea(new Dimension(43, 0)));
                                port.add(p);
                                port.add(Box.createRigidArea(new Dimension(10, 0)));

                                JPanel hostname = new JPanel();
                                hostname.setLayout(new BoxLayout(hostname, BoxLayout.LINE_AXIS));
                                JLabel l2 = new JLabel("HOSTNAME");
                                p2 = new JTextField();
                                p2.setPreferredSize(new Dimension(200, 20));
                                hostname.add(Box.createRigidArea(new Dimension(10, 0)));
                                hostname.add(l2);
                                hostname.add(Box.createRigidArea(new Dimension(10, 0)));
                                hostname.add(p2);
                                hostname.add(Box.createRigidArea(new Dimension(10, 0)));

                                center.add(port);
                                center.add(Box.createRigidArea(new Dimension(0, 10)));
                                center.add(hostname);
                                center.add(Box.createRigidArea(new Dimension(0, 10)));

                                JPanel bot = new JPanel();
                                bot.setLayout(new BoxLayout(bot, BoxLayout.LINE_AXIS));
                                JButton btn = new JButton("CONNECT");
                                btn.setActionCommand("connect");
                                btn.addActionListener(_this);
                                bot.add(Box.createRigidArea(new Dimension(280, 0)));
                                bot.add(btn);

                                main.add(heading, BorderLayout.PAGE_START);
                                main.add(center, BorderLayout.CENTER);
                                main.add(bot, BorderLayout.PAGE_END);

                                frame.getContentPane().removeAll();
                                frame.setContentPane(main);
                                frame.setSize(400,180);
                                frame.revalidate();
                                frame.repaint();
                            }
                        });
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            t.start();
        }
        void connectUI() {
            Thread t = new Thread() {
                public void run() {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                JPanel main = new JPanel();
                                main.setLayout(new BorderLayout());

                                JPanel heading = new JPanel();
                                JLabel h = new JLabel("CONNECTING TO PORT: " + port + " - HOSTNAME: " + hostName);
                                heading.add(h);
                                main.add(heading);

                                JPanel bot = new JPanel();
                                JButton btn = new JButton("STOP");
                                btn.setActionCommand("stop");
                                btn.addActionListener(_this);
                                bot.add(btn);

                                main.add(heading, BorderLayout.PAGE_START);
                                main.add(bot, BorderLayout.CENTER);

                                frame.getContentPane().removeAll();
                                frame.setSize(400,130);
                                frame.setContentPane(main);
                                frame.revalidate();
                                frame.repaint();
                            }
                        });
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            t.start();
        }
        private void displayInfo(String info) {
            JOptionPane op = new JOptionPane();
            op.showMessageDialog(frame,info);
        }
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == "connect") {
                if (p.getText().isEmpty() || p2.getText().isEmpty()) {
                    displayInfo("PORT OR HOSTNAME CANNOT BE EMPTY");
                }
                else {
                    port = Integer.parseInt(p.getText());
                    hostName = p2.getText();
                    connectUI();
                    run();
                }
            }
            else if (e.getActionCommand() == "stop"){
                initUI();
                isDisconnected = true;
            }
        }
    }
    private static void setInfo(String path) {
        listOfFile = (new File(path)).listFiles();
    }
    public long getSize(File folder) {
        long length = 0;

        File[] files = folder.listFiles();

        int count = files.length;

        for (int i = 0; i < count; i++) {
            if (files[i].isFile()) {
                length += files[i].length();
            }
            else {
                length += getSize(files[i]);
            }
        }
        return length;
    }
    public int countFolder(File[] listOfFile) {
        int count = 0;
        for (File f : listOfFile) {
            if (f.isDirectory()) count++;
        }

        return count;
    }
    public long countSizeOfFile(File[] listOfFile) {
        int size = 0;
        for (File f : listOfFile) {
            if (f.isFile()) {
                size += f.length();
            }
        }

        return size;
    }
    class ReceivedThread extends Thread{
        private DataInputStream reader;
        private Socket socket;
        private Client client;
        public ReceivedThread(Socket socket, Client client) {
            this.socket = socket;
            this.client = client;

            try {
                reader = new DataInputStream(socket.getInputStream());

            } catch(IOException ex) {
                System.out.println("Error getting input stream: " + ex.getMessage());
            }
        }
        public void run() {
            while (true) {
                try {
                    String response = reader.readUTF();
                    System.out.println("Response::::" + response);

                    if (response.equals("1")) {
                        setChoosed(true);
                    }
                    else if (response.equals("2")) {
                        setChoosed(false);
                    }
                    else if (response.equals("0")) {
                        try {
                            socket.close();
                            isDisconnected = true;
                            client.ui.initUI();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                    else if (isChoosed()){
                        if (!response.isEmpty())
                            curPath = path + "\\" + response;
                        else curPath = path;
                    }

                } catch (IOException ex) {
                    System.out.println("Error reading from server: " + ex.getMessage());
                    break;
                }
            }
        }
    }
    class SendThread extends Thread {
        private DataOutputStream sender;
        private Socket socket;
        private Client client;

        public SendThread(Socket socket, Client client) {
            this.socket = socket;
            this.client = client;

            try {
                sender = new DataOutputStream(socket.getOutputStream());
            } catch (IOException ex) {
                System.out.println("Error getting output stream: " + ex.getMessage());
            }
        }

        public void run() {
            while(true) {
                if (isDisconnected) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
                else if (isChoosed() && curPath == path) {
                    for (File f : listOfFile) {
                        String res = null;
                        if (f.isDirectory()) {
                            double size = getSize(new File(path + "\\" + f.getName()));
                            res = "Dir:" + f.getName() + ":" + size;
                            System.out.println(res);
                        }
                        else if (f.isFile()) {
                            res = "File:" + f.getName() + ":" + f.length();
                            System.out.println(res);
                        }
                        
                        try {
                            sender.writeUTF(res);
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
                else if (isChoosed() && curPath != path){
                    File[] subListOfFile = (new File(curPath)).listFiles();

                    try {
                        int numberOfFolder = countFolder(subListOfFile);
                        int numberOfFile = subListOfFile.length - numberOfFolder;
                        sender.writeUTF(numberOfFolder + "-" + numberOfFile + "-" + countSizeOfFile(subListOfFile));
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        break;
                    }

                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.execute();
    }
}
