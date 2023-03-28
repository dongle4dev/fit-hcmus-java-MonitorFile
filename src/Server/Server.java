package Server;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Server
 * Created by lddong
 * Date 12/28/2022 - 2:49 PM
 * Description: ...
 */
class UserThread extends Thread {
    private Socket socket;
    private Server server;
    public HashMap<String, Double> listOfFile = new HashMap<>();
    public Set<String> listOfFolder = new HashSet<>();
    private boolean isSupervised;
    private String username;
    private Server.UI ui;
    private DataInputStream reader;
    private DataOutputStream sender;
    private int numberOfFolder = -1;
    private int numberOfFile = -1;
    private long sizeOfFile = -1;
    JButton btn;

    public void refreshHistory() {
        this.history = new ArrayList<>();
        this.numberOfFolder = -1;
        this.numberOfFile = -1;
        this.sizeOfFile = -1;
    }

    private ArrayList<String> history = new ArrayList<>();
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    private String path;
    private String[] convertArrayListToArray(ArrayList<String> arrayList) {
        String[] arr = new String[arrayList.size()];
        arr = arrayList.toArray(arr);

        return arr;
    }
    public String[] getHistory() {
        return convertArrayListToArray(history);
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public boolean isSupervised() {
        return isSupervised;
    }
    public void setSupervised(boolean supervised) {
        try {
            isSupervised = supervised;
            if (isSupervised)
                sender.writeUTF("1");
            else sender.writeUTF("2");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }
    public void disconnect() {
        try {
            sender.writeUTF("0");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    public void updateSupervisedFolder(String path) {
        try {
            sender.writeUTF(path);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    public void checkStatus(int numberOfFolder, int numberOfFile, long sizeOfFile) {
        if (this.numberOfFolder == -1) {
            this.numberOfFolder = numberOfFolder;
            this.numberOfFile = numberOfFile;
            this.sizeOfFile = sizeOfFile;
        }
        else {
            if (this.numberOfFolder > numberOfFolder) {
                history.add("Client đã xóa folder vào lúc " + new Date());
            } else if (this.numberOfFolder < numberOfFolder) {
                history.add("Client đã thêm folder vào lúc " + new Date());
            }

            if (this.numberOfFile > numberOfFile) {
                history.add("Client đã xóa file vào lúc " + new Date());
            } else if (this.numberOfFile < numberOfFile) {
                history.add("Client đã thêm file vào lúc " + new Date());
            } else if (this.sizeOfFile != sizeOfFile) {
                history.add("Client đã chỉnh sửa file vào lúc " + new Date());
            }
            this.numberOfFolder = numberOfFolder;
            this.numberOfFile = numberOfFile;
            this.sizeOfFile = sizeOfFile;
        }
    }
    public UserThread(Socket socket, Server server, Server.UI ui, String username, JButton btn) {
        this.socket = socket;
        this.server = server;
        this.ui = ui;
        this.username = username;
        this.btn = btn;

        try {
            reader = new DataInputStream(socket.getInputStream());
            sender = new DataOutputStream(socket.getOutputStream());
        } catch(IOException ex) {
            System.out.println("Error in UserThread: " + ex.getMessage());
        }
    }
    public void run() {
        while (true) {
            try {
                String received = reader.readUTF();

                if (received.equals("quit")) {
                    ui.displayInfo(username + " is quited.");
                    server.removeUser(username, this, btn);
                }
                else {
                    String part[] = received.split(":");

                    if (part.length > 2) {
                        String type = part[0];
                        String name = part[1];
                        Double size = Double.parseDouble(part[2]);

                        listOfFile.put(name, size);
                        if (type.equals("Dir"))
                            listOfFolder.add(name);
                    } else {
                        part = received.split("-");
                        checkStatus(Integer.parseInt(part[0]), Integer.parseInt(part[1]), Integer.parseInt(part[2]));
                    }
                }
            } catch (Exception e) {
                System.out.println("There is some errors");
                server.removeUser(username, this, btn);
                try {
                    socket.close();
                    break;
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

        }
    }
}
public class Server {
    private int port;
    private Set<String> userNames = new HashSet<>();
    private Set<UserThread> userThreads = new HashSet<>();
    Set<JButton> userBtn = new HashSet<>();
    private UI ui;
    public Server(int port) {
        this.port = port;
    }
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ui = new UI();
                }
            });

            System.out.println("Server is listening on port " + port);
            while(true) {
                Socket socket = server.accept();
                System.out.println("New user connected");

                String username = "Client " + (userNames.size() + 1) + "";
                JButton btn = new JButton(username);
                UserThread newUser = new UserThread(socket,this, ui, username, btn);
                userThreads.add(newUser);
                userNames.add(username);

                newUser.start();

                addClientBtn(btn);
                ui.initUI();
            }
        } catch(IOException ex) {
            System.out.println("Error in the server" + ex.getMessage());
        }
    }
    void addUserName(String userName) {
        userNames.add(userName);
    }

    void removeUser(String userName, UserThread user, JButton btn) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            userThreads.remove(user);
            removeBtn(btn);
            System.out.println("The user " + user + " quited");
            ui.initUI();
        }
    }
    public void addClientBtn(JButton btn) {
        btn.setPreferredSize(new Dimension(150, 30));
        btn.setActionCommand(btn.getText());
        btn.addActionListener(ui);
        userBtn.add(btn);
    }
    public void removeBtn(JButton btn) {
        userBtn.remove(btn);
    }

    Set<String> getUserNames() {
        return this.userNames;
    }

    boolean hasUsers() {
        return !this.userNames.isEmpty();
    }

    class UI implements ActionListener {
        final private JFrame frame = new JFrame("SERVER");
        ActionListener _this = this;
        private UserThread curUser;
        private String[] history;
        JList historyList;
        boolean isSupervisingUI = false;
        public UI() {
            //Create and set up the window.
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.pack();

            //Create a nd set up the content pane.
            initUI();
            //Display the window.
            frame.setSize(600,400);
            frame.setVisible(true);
        }
        public void initUI() {
            Thread t = new Thread() {
                public void run() {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                JPanel main = new JPanel();
                                main.setLayout(new BorderLayout());

                                JPanel heading = new JPanel();
                                JLabel l = new JLabel("Choose client to supervise");
                                heading.add(l);

                                main.add(heading, BorderLayout.PAGE_START);
                                if (userBtn.size() > 0) {
                                    JPanel client = new JPanel();
                                    client.setLayout(new BoxLayout(client, BoxLayout.Y_AXIS));
                                    Iterator<JButton> btn = userBtn.iterator();
                                    while (btn.hasNext()) {
                                        JPanel center = new JPanel();
                                        center.add(btn.next());
                                        client.add(center);
                                    }
                                    main.add(client, BorderLayout.CENTER);
                                }
                                else {
                                    JPanel center = new JPanel();
                                    JLabel empty = new JLabel("DONT HAVE ANY USER CONNECTING");
                                    empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                                    empty.setAlignmentY(Component.CENTER_ALIGNMENT);
                                    center.add(empty);
                                    main.add(center, BorderLayout.CENTER);
                                }
                                frame.getContentPane().removeAll();
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
        public void run() {
            Thread t = new Thread() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (isSupervisingUI) {
                                        if (curUser != null) {
                                            if (history != curUser.getHistory() && curUser.getHistory().length != 0) {
                                                for (String s : curUser.getHistory()) {
                                                    System.out.println(s);
                                                }
                                                historyList.setListData(history);
                                                history = curUser.getHistory();
                                            }
                                        }
                                    }

                                }
                            });

                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };

            t.start();
        }
        public void initChooseUI(String username, HashMap<String, Double> listOfFile, Set<String> listOfFolder) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(1000);
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {

                                JPanel main = new JPanel();
                                main.setLayout(new BorderLayout());

                                JPanel heading = new JPanel();
                                heading.setLayout(new BoxLayout(heading, BoxLayout.X_AXIS));
                                JLabel l = new JLabel("Supervising " + username);
                                JButton backBtn = new JButton("BACK");
                                JButton stopBtn = new JButton("STOP");
                                backBtn.setActionCommand("BACKTOHOME");
                                stopBtn.setActionCommand("STOP");
                                backBtn.addActionListener(_this);
                                stopBtn.addActionListener(_this);
                                backBtn.setPreferredSize(new Dimension(100, 30));
                                stopBtn.setPreferredSize(new Dimension(100, 30));

                                heading.add(backBtn);
                                heading.add(Box.createRigidArea(new Dimension(150, 0)));
                                heading.add(l);
                                heading.add(Box.createRigidArea((new Dimension(150, 0))));
                                heading.add(stopBtn);
                                main.add(heading, BorderLayout.PAGE_START);

                                JPanel center = new JPanel();
                                JScrollPane pane  = new JScrollPane();

                                for (HashMap.Entry<String, Double> set :listOfFile.entrySet()) {
                                    JButton temp;

                                    if (listOfFolder.contains(set.getKey())) {
                                        try {
                                            temp = new JButton(set.getKey());
                                            temp.setPreferredSize(new Dimension(150,30));
                                            Image img = ImageIO.read(new File("image/folder.png")).getScaledInstance(20, 25, Image.SCALE_DEFAULT);
                                            temp.setIcon(new ImageIcon(img));
                                            center.add(temp);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                        temp.setActionCommand(set.getKey());
                                        temp.addActionListener(_this);
                                    }
                                    else {
                                        try {
                                            temp = new JButton(set.getKey());
                                            temp.setPreferredSize(new Dimension(150,30));
                                            Image img = ImageIO.read(new File("image/file.png")).getScaledInstance(20, 25, Image.SCALE_DEFAULT);
                                            temp.setIcon(new ImageIcon(img));

                                            center.add(temp);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                                center.setPreferredSize(new Dimension(500,350));
                                pane.setPreferredSize(new Dimension(500, 350));
                                pane.setViewportView(center);
                                main.add(pane, BorderLayout.CENTER);

                                frame.getContentPane().removeAll();
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
        public void initSuperviseUI(String username, String folderPath) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                JPanel main = new JPanel();
                                main.setLayout(new BorderLayout());

                                JPanel heading = new JPanel();
                                heading.setLayout(new BoxLayout(heading, BoxLayout.X_AXIS));
                                JLabel l = new JLabel("Supervising " + username + " at folder: " + folderPath);
                                JButton backBtn = new JButton("BACK");
                                backBtn.setActionCommand("BACK");
                                backBtn.addActionListener(_this);
                                backBtn.setPreferredSize(new Dimension(100, 30));

                                heading.add(backBtn);
                                heading.add(Box.createRigidArea(new Dimension(150, 0)));
                                heading.add(l);
                                main.add(heading, BorderLayout.PAGE_START);

                                historyList = new JList(history);
                                JScrollPane pane = new JScrollPane(historyList);
                                pane.setPreferredSize(new Dimension(500, 350));

                                main.add(pane, BorderLayout.CENTER);

                                frame.getContentPane().removeAll();
                                frame.setContentPane(main);
                                frame.revalidate();
                                frame.repaint();
                            }
                        });
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            };

            t.start();
        }
        public void displayInfo(String info) {
            JOptionPane op = new JOptionPane();
            op.showMessageDialog(frame,info);
        }
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("BACKTOHOME")) {
                initUI();
                curUser.setSupervised(false);
            }
            else if (e.getActionCommand().equals("BACK")) {
                isSupervisingUI = false;
                curUser.refreshHistory();
                curUser.updateSupervisedFolder("");
                history = null;
                initChooseUI(curUser.getUsername(), curUser.listOfFile, curUser.listOfFolder);
            }
            else if (e.getActionCommand().equals("STOP")) {
                curUser.disconnect();
                removeUser(curUser.getUsername(), curUser, curUser.btn);
                initUI();
            }
            else {
                for (UserThread t : userThreads) {
                    if (e.getActionCommand() == t.getUsername()) {

                        curUser = t;
                        if (!t.isSupervised()) {
                            t.setSupervised(true);
                            System.out.println("Choose" + t.getUsername());
                            initChooseUI(t.getUsername(), t.listOfFile, t.listOfFolder);
                        }
                        else {
                            initChooseUI(t.getUsername(), t.listOfFile, t.listOfFolder);
                        }
                    }
                    else {
                        try {
                            System.out.println(e.getActionCommand());
                            t.setPath(e.getActionCommand());
                            t.updateSupervisedFolder(t.getPath());
                            history = t.getHistory();
                            initSuperviseUI(t.getUsername(), t.getPath());
                            isSupervisingUI = true;
                            run();
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                        }
                    }
                }
            }
        }
    }
    public static void main(String[] args)  {
        Server server = new Server(8989);
        server.run();
    }
}
