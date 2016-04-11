package edu.pitt.dbmi.ccd.connection;

import com.jcraft.jsch.*;

import java.io.*;

/**
 * Author : Jeremy Espino MD
 * Created  9/15/15 11:29 AM
 */
public class SshConnection implements Connection {

    private String host;
    private String username;
    private String password;
    private String privateKey;
    private int port = 22;
    private String passphrase;
    private String knownHosts;

    private JSch jsch;
    private Session session;

    private static SshConnection theInstance = null;


    public static SshConnection getInstance() {

        if (theInstance == null) theInstance = new SshConnection();

        return theInstance;

    }

    private SshConnection() {
        Configuration configuration = Configuration.getInstance();
        this.host = configuration.getHostname();
        this.username = configuration.getUsername();
        this.password = configuration.getPassword();
        this.privateKey = configuration.getPublic_key_file();
        this.passphrase = configuration.getPassphrase();
        this.knownHosts = configuration.getKnownHosts();

        if (configuration.isLogging()) {
            JSch.setLogger(new MyLogger());
        }

        this.jsch = new JSch();

    }


    public void connect() throws Exception {

        jsch.setKnownHosts(knownHosts);
        jsch.addIdentity(privateKey);


        session = jsch.getSession(username, host, port);


        UserInfoImpl ui = new UserInfoImpl();
        ui.setPassphrase(passphrase);
        ui.setPassword(password);
        session.setUserInfo(ui);

        session.connect();


    }

    public void close() {

        session.disconnect();

    }

    public String getScratchDir() {
        return null;
    }

    public String executeCommand(String command) throws Exception {

        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        channel.setInputStream(null);

        ((ChannelExec) channel).setErrStream(System.err);

        InputStream in = channel.getInputStream();

        channel.connect();

        StringBuffer output = new StringBuffer();
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                output.append(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                if (in.available() > 0) continue;
                if (channel.getExitStatus() != 0) throw new Exception("Command executed with errors");
                // System.out.println("exit-status: " + channel.getExitStatus());
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }

        }
        channel.disconnect();
        return output.toString();
    }

    public boolean mkDir(String path) throws Exception {

        Channel channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp c = (ChannelSftp) channel;


        try {
            c.mkdir(path);
        } catch (SftpException e) {
            // TODO: log
            //System.out.println(e.toString());
            //System.out.println("exit-status: " + channel.getExitStatus());
            return false;
        }

        if (channel.isClosed()) {
            // TODO: log
            //System.out.println("exit-status: " + channel.getExitStatus());

        }

        return true;


    }

    static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if (b == 0) return b;
        if (b == -1) return b;

        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            }
            while (c != '\n');
            if (b == 1) { // error
                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }


    public void sendFile(String localFilename, String remoteFilename) throws Exception {
        boolean ptimestamp = true;

        // exec 'scp -t rfile' remotely
        String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + remoteFilename;
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        // get I/O streams for remote scp
        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        if (checkAck(in) != 0) {
            System.exit(0);
        }

        File _lfile = new File(localFilename);

        if (ptimestamp) {
            command = "T " + (_lfile.lastModified() / 1000) + " 0";
            // The access time should be sent here,
            // but it is not accessible with JavaAPI ;-<
            command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
            out.write(command.getBytes());
            out.flush();
            if (checkAck(in) != 0) {
                System.exit(0);
            }
        }

        // send "C0644 filesize filename", where filename should not include '/'
        long filesize = _lfile.length();
        command = "C0644 " + filesize + " ";
        if (localFilename.lastIndexOf('/') > 0) {
            command += localFilename.substring(localFilename.lastIndexOf('/') + 1);
        } else {
            command += localFilename;
        }
        command += "\n";
        out.write(command.getBytes());
        out.flush();
        if (checkAck(in) != 0) {
            System.exit(0);
        }

        // send a content of lfile
        FileInputStream fis = new FileInputStream(localFilename);
        byte[] buf = new byte[1024];
        while (true) {
            int len = fis.read(buf, 0, buf.length);
            if (len <= 0) break;
            out.write(buf, 0, len); //out.flush();
        }
        fis.close();
        fis = null;
        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();
        if (checkAck(in) != 0) {
            System.exit(0);
        }
        out.close();

        channel.disconnect();
        session.disconnect();

    }


    public void receiveFile(String remoteFilename, String localFilename) {

    }

    public static void main(String[] args) throws Exception {

        SshConnection sshConnection = new SshConnection();
        sshConnection.connect();
        System.out.println(sshConnection.executeCommand("squeue"));
        sshConnection.executeCommand("date");
        sshConnection.mkDir("/home/jue/test1234");
        sshConnection.close();

    }


}