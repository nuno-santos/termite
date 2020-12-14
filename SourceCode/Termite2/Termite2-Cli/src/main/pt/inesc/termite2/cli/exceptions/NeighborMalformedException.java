package main.pt.inesc.termite2.cli.exceptions;

public class NeighborMalformedException extends Exception {

	private static final long serialVersionUID = 1L;

	public NeighborMalformedException() {
	}

	public NeighborMalformedException(String msg) {
		super(msg);
	}
}
