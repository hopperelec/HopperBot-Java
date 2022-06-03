package uk.co.hopperelec.hopperbot;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;

public record SimpleMySQLLoginDetails(@NotNull String host, @NotNull String name, @NotNull String user, @NotNull String password) {
    @NotNull
    @CheckReturnValue
    public MysqlDataSource getDataSource() {
        final MysqlDataSource mysql = new MysqlConnectionPoolDataSource();
        mysql.setServerName(host());
        mysql.setDatabaseName(name());
        mysql.setUser(user());
        mysql.setPassword(password());
        return mysql;
    }
}
