package cn.whale.helper.utils;

public class DbConfig {
    
    public String serviceName;
    
    public String host;
    public String port;
    public String user;
    public String password;
    // default database
    public String database;

    @Override
    public String toString() {
        return serviceName;
    }
}