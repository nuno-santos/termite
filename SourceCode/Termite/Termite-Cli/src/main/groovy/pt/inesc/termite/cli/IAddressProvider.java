package pt.inesc.termite.cli;

import pt.inesc.termite.cli.exceptions.ConnectorTargetException;

public interface IAddressProvider {

    AddressSet claimAddressSet() throws ConnectorTargetException;
    void releaseAddressSet(AddressSet addressSet) throws ConnectorTargetException;
}
