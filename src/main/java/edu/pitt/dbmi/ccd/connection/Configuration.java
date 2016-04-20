package edu.pitt.dbmi.ccd.connection;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Author : Jeremy Espino MD
 * Created  4/7/16 2:41 PM
 */
public class Configuration {

    private String username;
    private String password;
    private String public_key_file;
    private String passphrase;
    private String hostname;
    private int port;
    private boolean logging;
    private String templatePath;
    private String scratchDirectory;

    private static Configuration ourInstance = new Configuration();
    public  String knownHosts;

    public static Configuration getInstance() {
        return ourInstance;
    }

    private Configuration() {

        try {
            getProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Properties getProperties() throws Exception {
        InputStream inputStream = null;
        Properties prop = new Properties();

        try {

            String propFileName = "remote_scheduler_client.properties";

            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            // get the property value and print it out
            username = prop.getProperty("username");
            password = prop.getProperty("password");
            public_key_file = prop.getProperty("public_key_file");
            passphrase = prop.getProperty("passphrase");
            hostname = prop.getProperty("hostname");
            port = Integer.parseInt(prop.getProperty("port"));
            logging = prop.getProperty("logging").equalsIgnoreCase("true");
            knownHosts = prop.getProperty("known_hosts");
            templatePath = prop.getProperty("template_path");
            scratchDirectory = prop.getProperty("scratch_directory");

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (inputStream != null) inputStream.close();
        }
        return prop;
    }

    public String getHostname() {
        return hostname;
    }

    public boolean isLogging() {
        return logging;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public String getPublic_key_file() {
        return public_key_file;
    }

    public String getUsername() {
        return username;
    }

    public String getKnownHosts() {
        return knownHosts;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public String getScratchDirectory() {
        return scratchDirectory;
    }
}

