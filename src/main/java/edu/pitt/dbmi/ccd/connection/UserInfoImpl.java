package edu.pitt.dbmi.ccd.connection;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * Author : Jeremy Espino MD
 * Created  4/1/16 2:50 PM
 */
public class UserInfoImpl implements UserInfo {

    private String passphrase;
    private String password;

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public String getPassword() {
        return password;
    }

    public boolean promptPassword(String s) {
        return true;
    }

    public boolean promptPassphrase(String s) {
        return true;
    }

    public boolean promptYesNo(String s) {
        System.out.println(s);
        return true;
    }

    public void showMessage(String s) {
        System.out.println(s);

    }


}
