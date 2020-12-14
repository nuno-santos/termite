package main.pt.inesc.termite2.cli;

public class Emulator {

    private String mId;
    private String mName;
    private String mIp;
    private int mPort;
    private int mCommitPort;
    private int mMessagePort;

    public Emulator(String name, int port, String ip, int cport, int mport){
        mId = ""+ ip + ":" + port;
        mName = name;
        mIp = ip;
        mPort = port;
        mCommitPort = cport;
        mMessagePort = mport;
    }

    public String getId(){
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getIp() {
        return mIp;
    }

    public int getPort() {
        return mPort;
    }

    public int getCommitPort() {
        return mCommitPort;
    }

    public int getMessagePort() {
        return mMessagePort;
    }

    public boolean equals(Emulator emu){
        return emu.getId().equals(mId);
    }

    public void print() {
        System.out.println("    Name: " + mName);
        System.out.println("    Ip: " + mIp);
        System.out.println("    EmuPort: " + mPort);
        System.out.println("    CommitPort: " + mCommitPort);
        System.out.println("    MessagePort: " + mMessagePort);
    }

    public String toString(){
        return "Name: " + mName + " Ip: " + mIp + " Port: " + mPort + " Cport: " + mCommitPort + " Mport: " + mMessagePort ;
    }

    public String dataForUi(){
        return "" + mName + ";" + mPort + ";" + mCommitPort + ";" + mMessagePort;
    }
}
