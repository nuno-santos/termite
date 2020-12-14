package pt.inesc.termite.server;

public class AddressSet {

    private String networkIp;

    private int rCommitPort;
    private int rMessagePort;

    public AddressSet(int rcport, int rmport, String netIp) {
        this.rCommitPort = rcport;
        this.rMessagePort = rmport;
        this.networkIp = netIp;
    }

    public String getNetworkIp() {
        return networkIp;
    }

    public int getRealCommitPort() {
        return rCommitPort;
    }

    public int getRealMessagePort() {
        return rMessagePort;
    }

    public String getDataString() {
        return "" + networkIp + "|" + rCommitPort + "|" + rMessagePort;
    }

    public String getString() {
        return "IP: " + networkIp + " | RCommit: " + rCommitPort + " | RMsg: " + rMessagePort;
    }

    public void print() {
        System.out.println("[ vCommitPort = " + 9001 +
                ", rCommitPort = " + rCommitPort +
                ", vMessagePort = " + 10001 +
                ", rMessagePort = " + rMessagePort + "]");
    }

}
