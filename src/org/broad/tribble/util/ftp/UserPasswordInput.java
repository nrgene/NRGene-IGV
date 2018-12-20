package org.broad.tribble.util.ftp;

public interface UserPasswordInput {
	public void setHost(String host);
    public boolean showDialog();
    public String getUser();
    public String getPassword();
}
