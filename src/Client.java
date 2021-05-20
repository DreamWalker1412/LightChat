import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class Client extends Application {
    private Socket socket;
    private DataInputStream fromServer=null;
    private DataOutputStream toServer=null;
    private String nickName = null;
    private ObservableList<String> users= FXCollections.observableArrayList();
    private String usersName[];
    private int id;
    @Override
    public void start(Stage primaryStage) {

        //welcome scene
        GridPane registerPane = new GridPane();
        TextField tfNickname = new TextField();
        Button btOk = new Button("Sign in");

        registerPane.add(new Label("Please Enter Your Nickname : "),1,1);
        registerPane.add(tfNickname,2,1);
        registerPane.add(btOk,2,2);
        registerPane.setPadding(new Insets(15,15,15,10));
        registerPane.setHgap(15);
        registerPane.setVgap(20);

        Scene welcomeScene = new Scene(registerPane);
        primaryStage.setScene(welcomeScene);


        // Main scene
        VBox vBox = new VBox();
        Scene mainScene = new Scene(vBox);

        HBox menu = new HBox();
        menu.setPadding(new Insets(5,5,5,5));
        menu.setSpacing(5);

        GridPane paneForMainBody = new GridPane();
        vBox.getChildren().addAll(menu,paneForMainBody);

        //* set menu */
        ComboBox cbMenu1 = new ComboBox();
        Button btConnect = new Button("Connect");
        btConnect.setDisable(true);
        btConnect.setOnAction (e-> {
            try {
                toServer.writeUTF("/arg connectRequest");
                toServer.writeInt(users.indexOf(cbMenu1.getValue())+1);
                toServer.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        Button btDisconnect = new Button("Disconnect");
        btDisconnect.setOnAction(e-> {
            try {
                toServer.writeUTF("/arg disconnect");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        btDisconnect.setDisable(true);
        cbMenu1.setValue("User List");
        users.add("null");
        cbMenu1.getItems().addAll(users);

        menu.getChildren().addAll(cbMenu1,btConnect,btDisconnect);


        //* set mainScene */
        TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setWrapText(true);
        ScrollPane paneForText = new ScrollPane(ta);





        // set TextField
        TextField textWantToSend = new TextField();
        textWantToSend.setOnAction(event -> {
            if (textWantToSend.getText().contains("/arg")) {
                ta.appendText("Warning: /arg is used by system, you can not include it in text.\n");
            } else {
                ta.appendText(nickName + ": " + textWantToSend.getText() + '\n');
                try {
                    toServer.writeUTF(textWantToSend.getText());
                    toServer.flush();
                } catch (Exception ex) {
                    ta.appendText(ex.toString() + '\n');
                }
            }
            textWantToSend.clear();
        });

        Button btSend = new Button("send");
        btSend.setOnAction(event -> {
            if (textWantToSend.getText().contains("/arg")) {
                ta.appendText("Warning: /arg is used by system, you can not include it in text.\n");
                ta.appendText(nickName + ": " + textWantToSend.getText() + '\n');
                try {
                    toServer.writeUTF(textWantToSend.getText());
                    toServer.flush();
                } catch (Exception ex) {
                    ta.appendText(ex.toString() + '\n');
                }
            }
            textWantToSend.clear();
        });

        paneForMainBody.add(paneForText,1,1);
        paneForMainBody.add(textWantToSend,1,2);
        paneForMainBody.add(btSend,2,2);
        paneForMainBody.setPadding(new Insets(15,15,20,15));
        paneForMainBody.setVgap(15);
        paneForMainBody.setHgap(15);


        //set welcomeScene
        primaryStage.setTitle("LightChat Client");
        primaryStage.setResizable(false);
        tfNickname.setOnAction( e -> {
            nickName=tfNickname.getText();
            Platform.runLater(()->{
                primaryStage.setScene(mainScene);
                try {
                    primaryStage.setTitle("LightChat Client - "+nickName);
                    toServer.writeUTF(nickName);
                    toServer.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (NullPointerException ex) {
                    return;
                }
            });
        });

        btOk.setOnAction( e -> {
            nickName=tfNickname.getText();
            Platform.runLater(()->{
                primaryStage.setScene(mainScene);
                try {
                    primaryStage.setTitle("LightChat Client: "+nickName);
                    toServer.writeUTF(nickName);
                    toServer.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (NullPointerException ex) {
                    return;
                }
            });
        });

        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            System.exit(0);
        });

        //set stageRequest
        Stage stageRequest = new Stage();


        Button btReceive = new Button("Receive");
        btReceive.setOnAction(e-> {
            try {
                toServer.writeUTF("yes");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.runLater(()->{
                stageRequest.close();
            });
        });


        Button btNo = new Button("No");
        btNo.setOnAction(e-> {
            try {
                toServer.writeUTF("no");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.runLater(()-> {
                stageRequest.close();
            });
        });
        Label lbTip = new Label("Request");

        HBox hBoxRequest = new HBox(btReceive,btNo);
        hBoxRequest.setSpacing(30);
        hBoxRequest.setPadding(new Insets(20));
        BorderPane paneRequest = new BorderPane();
        paneRequest.setTop(new StackPane(lbTip));
        paneRequest.setCenter(hBoxRequest);
        Scene sceneRequest = new Scene(paneRequest);
        paneRequest.setPadding(new Insets(30,30,20,30));
        stageRequest.setTitle("Request");
        stageRequest.setScene(sceneRequest);
        stageRequest.setFullScreen(false);
        stageRequest.setResizable(false);
        stageRequest.setAlwaysOnTop(true);


        //Socket
        try {
            socket = new Socket("localhost",8000);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            ta.appendText(ex.toString()+'\n');
        }

        new Thread(()->{
            while (true) {
                try {
                    String info=fromServer.readUTF();
                    if (!info.startsWith("/arg"))
                        Platform.runLater(()->{
                            ta.appendText(info+"\n");
                        });
                    else {
                        String arg=info;
                        if (arg.contains("getId")) {
                            id = fromServer.readInt();
                        } else if (arg.contains("clear")) {
                          ta.clear();
                        } else if ( arg.contains("refreshAliveUsers")) {
                            users.clear();
                            arg=fromServer.readUTF();
                            while (!arg.contains("/end")) {
                                users.add(arg);
                                arg=fromServer.readUTF();
                            }
                            Platform.runLater(()->{
                                cbMenu1.getItems().clear();
                                cbMenu1.getItems().addAll(users);
                                cbMenu1.setOnAction(e -> {
                                    btConnect.setDisable(false);
                                    btConnect.addEventHandler(MouseEvent.MOUSE_ENTERED,(MouseEvent e2)->{
                                        btConnect.setEffect(new DropShadow());
                                    });
                                    btConnect.addEventHandler(MouseEvent.MOUSE_EXITED,(MouseEvent e3)->{
                                        btConnect.setEffect(null);
                                    });
                                });
                            });
                            arg=null;
                        } else if (arg.contains("preForConnection")) {
                            toServer.writeUTF("/arg preForConnection");

                        } else if (arg.contains("preOkay")) {

                        } else if (arg.contains("connectRequest")){
                            ta.appendText(fromServer.readUTF());
                            String nameRequest = fromServer.readUTF();
                            Platform.runLater(()-> {
                                stageRequest.setTitle("Tip");
                                lbTip.setText("Connect Request from "+nameRequest);
                                stageRequest.show();
                            });
                            arg=null;
                        } else if (arg.contains("connecting")) {
                            Platform.runLater(()-> {
                                cbMenu1.setDisable(true);
                                btConnect.setDisable(true);
                                btDisconnect.setDisable(false);

                            });
                        } else if (arg.contains("disconnectFrom")) {
                            Platform.runLater(()-> {
                                cbMenu1.setDisable(false);
                                btConnect.setDisable(false);
                                btDisconnect.setDisable(true);
                            });
                        } else if (arg.contains("disconnectTo")) {
                            toServer.writeUTF("/arg done");
                            Platform.runLater(()-> {
                                cbMenu1.setDisable(false);
                                btConnect.setDisable(false);
                                btDisconnect.setDisable(true);
                            });
                        } else if (arg.contains("closeRequestStage")){
                            Platform.runLater(()->{
                                stageRequest.close();
                            });
                        } else {
                            ta.appendText("Unknown args!");
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                } catch (NullPointerException ex) {
                    Platform.runLater(()-> {
                        ta.appendText("Sorry,the server is not available now.\n");
                        ta.appendText("Please wait a moment and restart the client.\n");
                    });
                    return;
                }
            }
        }).start();


    }


}