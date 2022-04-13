package uk.co.hopperelec.hopperbot;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;

public record SimpleMySQLLoginDetails(String host, String name, String user, String password) {
    public MysqlDataSource getDataSource() {
        final MysqlDataSource mysql = new MysqlConnectionPoolDataSource();
        mysql.setServerName(host());
        mysql.setDatabaseName(name());
        mysql.setUser(user());
        mysql.setPassword(password());
        return mysql;
    }
}
