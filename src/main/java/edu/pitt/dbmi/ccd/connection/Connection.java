package edu.pitt.dbmi.ccd.connection;

/**
 * Author : Jeremy Espino MD
 * Created  9/15/15 11:30 AM
 */
public interface Connection {

    void connect() throws Exception;

    void close();

    String executeCommand(String command) throws Exception;

    boolean mkDir(String directoryName) throws Exception;

    void sendFile(String localFilename, String remoteFilename) throws Exception;

    void receiveFile(String remoteFilename, String localFilename) throws Exception;

}
