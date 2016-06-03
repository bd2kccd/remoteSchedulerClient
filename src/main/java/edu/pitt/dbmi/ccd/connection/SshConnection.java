package edu.pitt.dbmi.ccd.connection;

import com.jcraft.jsch.*;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Author : Jeremy Espino MD 
 * Created 9/15/15 11:29 AM 
 * Author : Chirayu (Kong) Wongchokprasitti, PhD 
 * Modified 6/2/16 1:41 PM
 */
public class SshConnection implements Connection {

	static final org.slf4j.Logger logger = LoggerFactory.getLogger(SshConnection.class);

	private String host;
	private String username;
	private String password;
	private String privateKey;
	private int port;
	private String passphrase;
	private String knownHosts;

	private JSch jsch;
	private Session session;

	private static SshConnection theInstance = null;

	/**
	 * Singleton method to get the global instance
	 *
	 * @return
	 */
	public static SshConnection getInstance() {

		if (theInstance == null)
			theInstance = new SshConnection();

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
		this.port = configuration.getPort();

		if (configuration.isLogging()) {
			JSch.setLogger(new MyLogger());
		}

		this.jsch = new JSch();

	}

	/**
	 * Connect to the remote system
	 *
	 * @throws Exception
	 */
	public void connect() throws Exception {

		session = jsch.getSession(username, host, port);
		session.setConfig("StrictHostKeyChecking", "no");
		session.setPassword(password);

		UserInfoImpl ui = new UserInfoImpl();
		session.setUserInfo(ui);

		session.connect();

	}

	/**
	 * Close the remote connection
	 *
	 */
	public void close() {

		session.disconnect();

	}

	/**
	 * Execute a command on remote system. Must connect first.
	 *
	 * @param command
	 * @return
	 * @throws Exception
	 */
	public String executeCommand(String command) throws Exception {

		ChannelExec channel = (ChannelExec) session.openChannel("exec");
		channel.setCommand(command);

		channel.setInputStream(null);

		InputStream in = channel.getInputStream();
		InputStream stderr = ((ChannelExec) channel).getErrStream();

		channel.connect();

		StringBuffer output = new StringBuffer();
		byte[] tmp = new byte[1024];
		while (true) {
			while (in.available() > 0) {
				int i = in.read(tmp, 0, 1024);
				if (i < 0)
					break;
				output.append(new String(tmp, 0, i));
			}

			if (channel.isClosed()) {
				if (in.available() > 0)
					continue;
				if (channel.getExitStatus() != 0) {
					StringBuffer result = new StringBuffer();
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stderr));
					readAll(bufferedReader, result);
					logger.error(result.toString());
					throw new Exception(result.toString());
				}

				break;
			}

			try {
				Thread.sleep(500);
			} catch (Exception e) {
				logger.error("Error when executing command " + command, e);
			}

		}
		channel.disconnect();
		return output.toString();
	}

	private void readAll(BufferedReader bufferedReader, StringBuffer result) throws Exception {
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			result.append(line);
		}
	}

	/**
	 * Make a remote directory
	 *
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public boolean mkDir(String path) throws Exception {

		Channel channel = session.openChannel("sftp");
		channel.connect();
		ChannelSftp c = (ChannelSftp) channel;

		try {
			c.mkdir(path);
		} catch (SftpException e) {
			logger.error("exit-status: " + channel.getExitStatus(), e);
			return false;
		}

		if (channel.isClosed()) {
			logger.warn("Channel already closed - exit-status: " + channel.getExitStatus());
		}

		return true;

	}

	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				logger.error(sb.toString());
			}
			if (b == 2) { // fatal error
				logger.error(sb.toString());
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
			throw new Exception("Error in transfer of data");
		}

		File _lfile = new File(localFilename);

		if (ptimestamp) {
			command = "T" + (_lfile.lastModified() / 1000) + " 0";
			// The access time should be sent here,
			// but it is not accessible with JavaAPI ;-<
			command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				throw new Exception("Error in transfer of data");
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
			throw new Exception("Error in transfer of data");
		}

		// send a content of lfile
		FileInputStream fis = new FileInputStream(localFilename);
		byte[] buf = new byte[1024];
		while (true) {
			int len = fis.read(buf, 0, buf.length);
			if (len <= 0)
				break;
			out.write(buf, 0, len); // out.flush();
		}
		fis.close();
		fis = null;
		// send '\0'
		buf[0] = 0;
		out.write(buf, 0, 1);
		out.flush();
		if (checkAck(in) != 0) {
			throw new Exception("Error in transfer of data");
		}
		out.close();

		channel.disconnect();
		session.disconnect();

	}

	public void receiveFile(String remoteFilename, String localFilename) throws Exception {
		FileOutputStream fos = null;

		// exec 'scp -f rfile' remotely
		String command = "scp -f " + remoteFilename;
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);

		// get I/O streams for remote scp
		OutputStream out = channel.getOutputStream();
		InputStream in = channel.getInputStream();

		channel.connect();

		byte[] buf = new byte[1024];

		// send '\0'
		buf[0] = 0;
		out.write(buf, 0, 1);
		out.flush();

		while (true) {
			int c = checkAck(in);
			if (c != 'C') {
				break;
			}

			// read '0644 '
			in.read(buf, 0, 5);

			long filesize = 0L;
			while (true) {
				if (in.read(buf, 0, 1) < 0) {
					// error
					break;
				}
				if (buf[0] == ' ')
					break;
				filesize = filesize * 10L + (long) (buf[0] - '0');
			}

			String file = null;
			for (int i = 0;; i++) {
				in.read(buf, i, 1);
				if (buf[i] == (byte) 0x0a) {
					file = new String(buf, 0, i);
					break;
				}
			}

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			// read a content of lfile
			fos = new FileOutputStream(localFilename);
			int foo;
			while (true) {
				if (buf.length < filesize)
					foo = buf.length;
				else
					foo = (int) filesize;
				foo = in.read(buf, 0, foo);
				if (foo < 0) {
					// error
					break;
				}
				fos.write(buf, 0, foo);
				filesize -= foo;
				if (filesize == 0L)
					break;
			}
			fos.close();
			fos = null;

			if (checkAck(in) != 0) {
				throw new Exception("Error in transfer of data");
			}

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

		}

		session.disconnect();

	}

}
