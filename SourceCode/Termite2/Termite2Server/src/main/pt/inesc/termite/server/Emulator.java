package pt.inesc.termite.server;

public class Emulator {
    private String _name;
    private int _port;
    private int _state;

    public Emulator(int port, int state) {
        _port = port;
        _state = state;
    }

    public String get_name() {
        return _name;
    }

    public int get_port() {
        return _port;
    }

    public int get_state() {
        return _state;
    }

    public void setName(String name) {
        _name = name;
    }

    public void changeState(int newState) {
        _state = newState;
    }

    public void print() {
        System.out.println("    Emulator " + _name + ", port: " + _port + ", state: " + _state);
    }
}
