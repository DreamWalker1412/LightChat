import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;



public class Server extends Application {
    private int numOfUsers = 0;
    private Socket[] client = new Socket[1001];
    private ClientInfo[] clientInfos = new ClientInfo[1001];


    @Override
    public void start(Stage primaryStage) {
        TextArea ta=new TextArea();
        ta.setEditable(false);
        ta.setWrapText(true);

        Pane pane = new Pane(new ScrollPane(ta));
        Scene scene=new Scene(pane);

        primaryStage.setTitle("LightChat Server");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            System.exit(0);
        });



        //create a new thread to start server
        new Thread(()->{
            try {
                ServerSocket server=new ServerSocket(8000);
                Platform.runLater(()->
                        ta.appendText("Server is started at "+ new Date() + '\n'));


                //listen acceptRequest from client
                while (true) {
                    int clientId = numOfUsers + 1;
                    if ( numOfUsers++ <1000 ) {
                        client[clientId] = server.accept();
                        Platform.runLater(()->{
                          ta.appendText("Create a new thread for client "+clientId+'\n');
                        });
                    }
                    else {
                        Platform.runLater(()-> {
                            ta.appendText("Warning : Server's client number is over the maximum 1000 !\n");
                            ta.appendText("Refuse new client !\n");
                        });
                        return;
                    }

                    //create a new thread for each client
                    new Thread(()-> {
                        DataOutputStream toClient = null;
                        DataInputStream fromClient = null;
                        try {
                            fromClient = new DataInputStream(client[clientId].getInputStream());
                            toClient = new DataOutputStream(client[clientId].getOutputStream());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        int localPort = client[clientId].getLocalPort();
                        String ip = client[clientId].getInetAddress().getHostAddress();

                        try {
                            String nickName = fromClient.readUTF();
                            clientInfos[clientId] = new ClientInfo(nickName, ip, localPort, clientId,client[clientId],true);
                            Platform.runLater(() -> {
                                ta.appendText("Client " + clientId + "'s IP Address is " + ip + '\n');
                                ta.appendText("Client " + clientId + "'s localPort is " + localPort + '\n');
                            });


                            toClient.writeUTF("From Server : Welcome , "+clientInfos[clientId].getNickName()+".");
                            toClient.writeUTF("your ID is " + clientId + ".");
                            toClient.writeUTF("Online Users List: ");
                            for (int i = 1; i <= clientId; i++) {
                                if (clientInfos[i].isAlive())
                                 toClient.writeUTF("ID:" + i + "     Nickname :" + clientInfos[i].getNickName());}


                            toClient.writeUTF("/arg getId");
                            toClient.writeInt(clientId);

                            for (int j = 1; j <= clientId ;j++) {
                                DataOutputStream tempToClient = new DataOutputStream(client[j].getOutputStream());
                                tempToClient.writeUTF("/arg refreshAliveUsers");
                                tempToClient.flush();
                                for (int i = 1; i <= clientId; i++) {
                                    if (clientInfos[i].isAlive()) {
                                        tempToClient.writeUTF(clientInfos[i].getNickName());
                                        tempToClient.flush(); }
                                }
                                tempToClient.writeUTF("/end");
                                tempToClient.flush();
                                if (j!=clientId)
                                    tempToClient.writeUTF("from server : "+clientInfos[clientId].getNickName()+" is online!");
                            }

                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        //listen each client's arg

                        while (true) {
                            try {
                                if (!clientInfos[clientId].isInChat()) {
                                    String info = fromClient.readUTF();
                                    ta.appendText("from id " + clientId + ": " + info + '\n');
                                    if (info.startsWith("/arg")) {
                                        if (info.contains("connectRequest")) {
                                            int requestToId = fromClient.readInt();
                                            if (clientId==requestToId) {
                                                toClient.writeUTF("You can not connect yourself," + clientInfos[clientId].getNickName() + ".");
                                            } else if (!clientInfos[requestToId].isAlive()) {
                                                toClient.writeUTF("Sorry,"+clientInfos[requestToId].getNickName()+" is not online now!");
                                            } else if (clientInfos[requestToId].isInChat()) {
                                                toClient.writeUTF("Sorry, "+clientInfos[requestToId].getNickName()+" is busy now, please try later.");
                                                DataOutputStream tempOut = new DataOutputStream(client[requestToId].getOutputStream());
                                                tempOut.writeUTF("from server : "+clientInfos[clientId].getNickName()+" try to connect you just now, you can handle this later.");
                                            } else {
                                                clientInfos[clientId].setInChat(true);
                                                DataOutputStream tempOut = new DataOutputStream(client[requestToId].getOutputStream());
                                                tempOut.writeUTF("/arg preForConnection");
                                                Thread.sleep(500);
                                                new Thread(new HandleSession(client[clientId], client[requestToId], clientId, requestToId)).start();
                                            }
                                        } else if(info.contains("preForConnection")) {
                                            clientInfos[clientId].setInChat(true);
                                            toClient.writeUTF("/arg preOkay");
                                        } else {
                                            ta.appendText("Unknown arg from id " + clientId + ": " + info + '\n');
                                        }
                                        info=null;
                                    }
                                } else {
                                    Thread.sleep(1000);
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                clientInfos[clientId].setAlive(false);
                                return;
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }

                        }

                    }).start();


                    /*
                    Platform.runLater(()-> {
                        ta.appendText("Wait for Users to join session "+sessionNO+'\n');
                    });
                    Socket client1 = server.accept();
                    inputFromClient1 =new DataInputStream(client1.getInputStream());
                    outputToClient1 = new DataOutputStream(client1.getOutputStream());

                    Platform.runLater(()->{
                        ta.appendText("Client1 joined session "+ sessionNO +".\n");
                        ta.appendText("Client1's IP address : "+ client1.getInetAddress().getHostAddress() + '\n');
                    });

                    Socket client2 = server.accept();
                    inputFromClient2 = new DataInputStream(client2.getInputStream());
                    outputToClient2 = new DataOutputStream(client2.getOutputStream());

                    Platform.runLater(()->{
                        ta.appendText("Client2 joined session " + sessionNO +".\n");
                        ta.appendText("Client2's IP address : "+ client2.getInetAddress().getHostAddress() + '\n');
                        ta.appendText(new Date()+":start a thread for session "+ sessionNO++ +".\n");
                    });

                    new Thread(new HandleSession(client1,client2)).start();*/
                }

            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    class HandleSession implements Runnable {
        private Socket client1 = null;
        private Socket client2 = null;
        private int client1Id;
        private int client2Id;

        private DataInputStream fromClient1;
        private DataInputStream fromClient2;
        private DataOutputStream toClient1;
        private DataOutputStream toClient2;
        private Boolean continueToChat = true;

        HandleSession (Socket client1,Socket client2,int client1Id,int client2Id) {
            this.client1=client1;
            this.client2=client2;
            this.client1Id = client1Id;
            this.client2Id = client2Id;
        }



        public void run() {
            try {
                fromClient1 = new DataInputStream(client1.getInputStream());
                fromClient2 = new DataInputStream(client2.getInputStream());
                toClient1 = new DataOutputStream(client1.getOutputStream());
                toClient2 = new DataOutputStream(client2.getOutputStream());
                toClient1.writeUTF("from server : Now is trying connecting to "+clientInfos[client2Id].getNickName());
                toClient1.writeUTF("Please wait for at most 20s.");
                toClient2.writeUTF("/arg connectRequest");
                toClient2.writeUTF("from server : " + clientInfos[client1Id].getNickName()+"(ID:"+client1Id+") want to connect to you!\n");
                toClient2.writeUTF(clientInfos[client1Id].getNickName());
                toClient2.writeUTF("from server : Please Enter \"yes\" Or \"no\" in 10 seconds. ");
                boolean bool = true;
                client2.setSoTimeout(20000);
                while (bool) {
                    String flag = fromClient2.readUTF();
                    if (flag.contains("yes")) {
                        bool = false;
                        client2.setSoTimeout(0);
                        toClient1.writeUTF("/arg clear");
                        toClient1.writeUTF("/arg connecting");
                        toClient2.writeUTF("/arg clear");
                        toClient2.writeUTF("/arg connecting");
                        toClient1.writeUTF("from server : Connected to " + clientInfos[client2Id].getNickName() + " successfully !");
                        toClient2.writeUTF("from server : Connected to " + clientInfos[client1Id].getNickName() + " successfully !");
                    } else if (flag.contains("no")) {
                        bool = false;
                        client2.setSoTimeout(0);
                        toClient1.writeUTF("from server : Sorry, " + clientInfos[client2Id].getNickName() + " refused you .");
                        toClient2.writeUTF("from server : Connect request is refused.");
                        clientInfos[client1Id].setInChat(false);
                        clientInfos[client2Id].setInChat(false);
                        return;
                    } else {
                        toClient2.writeUTF("from server : Please Enter \"yes\" Or \"no\" in 10 seconds. ");
                    }
                }

            } catch (IOException ex) {
                try {
                    toClient1.writeUTF("from server : Sorry, connection request Time Out, please try later.");
                    toClient2.writeUTF("from server : Time Out !");
                    toClient2.writeUTF("/arg closeRequestStage");
                    clientInfos[client1Id].setInChat(false);
                    clientInfos[client2Id].setInChat(false);
                    client2.setSoTimeout(0);
                } catch (IOException ex2) {
                    ex.printStackTrace();
                }
                    continueToChat = false;
                    return;

            }
            new Thread(()->{
                while (continueToChat) {
                try {

                    String info = fromClient2.readUTF();
                    if (info.startsWith("/arg")) {
                        if (info.contains("disconnect")) {
                            toClient1.writeUTF("/arg disconnectTo");
                            toClient2.writeUTF("/arg disconnectFrom");
                            toClient2.writeUTF("from server : Disconnect successfully .");
                            toClient1.writeUTF("from server : "+clientInfos[client2Id].getNickName()+" has disconnected .");
                            clientInfos[client2Id].setInChat(false);
                            return;
                        } else if (info.contains("done")) {
                            clientInfos[client2Id].setInChat(false);
                            continueToChat = false;
                            return;
                        }
                    } else if (!info.contains("/arg")&&!info.equals("")) {
                        toClient1.writeUTF("from " + clientInfos[client2Id].getNickName() + ":" + info);
                        toClient1.flush();
                    }
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();
                        try {
                            toClient1.writeUTF("from server :"+clientInfos[client2Id].getNickName() + "is offLine!");
                        } catch (IOException ex2) {
                            clientInfos[client1Id].setAlive(false);
                            ex.printStackTrace();
                        }

                        continueToChat = false;
                        return;

                    }
                }
            }).start();

            new Thread(()->{
                while (continueToChat) {
                    try {
                        String info = fromClient1.readUTF();
                        if (info.startsWith("/arg")) {
                            if (info.contains("disconnect")) {
                                toClient1.writeUTF("/arg disconnectFrom");
                                toClient2.writeUTF("/arg disconnectTo");
                                toClient1.writeUTF("from server : Disconnect successfully .");
                                toClient2.writeUTF("from server : "+clientInfos[client1Id].getNickName()+" has disconnected .");
                                clientInfos[client1Id].setInChat(false);
                                return;
                            }  else if (info.contains("done")) {
                                clientInfos[client1Id].setInChat(false);
                                continueToChat = false;
                                return;
                            }
                        } else if (!info.contains("/arg")&&!info.equals("")) {
                            toClient2.writeUTF("from " + clientInfos[client1Id].getNickName() + ":" + info);
                            toClient2.flush();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        try {
                            toClient2.writeUTF("from server :"+clientInfos[client1Id].getNickName() + "is offLine!");
                        } catch (IOException ex2) {
                            ex.printStackTrace();
                            clientInfos[client2Id].setAlive(false);
                        }

                        continueToChat = false;
                        return;


                    }
                }
            }).start();
        }
    }

}

