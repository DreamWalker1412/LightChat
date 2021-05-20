import java.net.Socket;

public class ClientInfo {
    private String nickName;
    private String ip;
    private int localPort = -1;
    private int id = -1;
    private Socket socket;
    private boolean inChat=false;
    private boolean alive=false;

    public ClientInfo () { }

    public ClientInfo (String nickName,String ip,int localPort,int id,Socket socket,boolean alive) {
        this.nickName=nickName;
        this.id=id;
        this.ip=ip;
        this.localPort=localPort;
        this.socket=socket;
        this.alive=alive;
    }



    public void setNickName (String nickName) {
        this.nickName=nickName;
    }

    public void setIp (String ip) {
        this.ip=ip;
    }

    public void setId (int id) {
        this.id=id;
    }

    public void setLocalPort (int localPort) {
        this.localPort = localPort;
    }

    public void setSocket (Socket socket) {this.socket=socket;}

    public void setInChat (boolean inChat) {this.inChat=inChat;}

    public void setAlive (boolean alive) {this.alive=alive;}

    public String getNickName () { return nickName;}

    public String getIp () {
        return ip;
    }

    public int getLocalPort () {
        return localPort;
    }

    public int getId () {
        return id;
    }

    public Socket getSocket () { return socket; }

    public boolean isInChat () { return inChat; }

    public boolean isAlive () { return alive; }
}
