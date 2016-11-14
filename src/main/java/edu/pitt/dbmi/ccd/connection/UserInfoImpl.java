package edu.pitt.dbmi.ccd.connection;

import com.jcraft.jsch.UserInfo;

/**
 * Author : Jeremy Espino MD Created 4/1/16 2:50 PM
 */
public class UserInfoImpl implements UserInfo {

    String password = null;

    @Override
    public String getPassphrase() {
        return null;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String passwd) {
        password = passwd;
    }

    @Override
    public boolean promptPassphrase(String message) {
        return false;
    }

    @Override
    public boolean promptPassword(String message) {
        return false;
    }

    @Override
    public boolean promptYesNo(String message) {
        return true;
    }

    @Override
    public void showMessage(String message) {
    }

}
