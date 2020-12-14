package pt.inesc.termite.server.exceptions;

public class ConfigErrorException extends Exception {

    public ConfigErrorException() {
    }

    public ConfigErrorException(String msg) {
        super(msg);
    }
}
