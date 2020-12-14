package pt.inesc.termite.cli;


public class AddressSet {

    public String mAVAddr;
    public String mARAddr;
    public String mCVAddr;
    public String mCRAddr;

    public AddressSet(String avaddr, String araddr, String cvaddr, String craddr) {
        mAVAddr = avaddr;
        mARAddr = araddr;
        mCVAddr = cvaddr;
        mCRAddr = craddr;
    }

    public void print() {
        System.out.println("[ avaddr = " + mAVAddr +
                ", araddr = " + mARAddr +
                ", cvaddr = " + mCVAddr +
                ", craddr = " + mCRAddr + "]");
    }

}
