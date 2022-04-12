package uk.co.hopperelec.HopperBot.Features.Economy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mysql.cj.jdbc.MysqlDataSource;
import net.dv8tion.jda.api.events.ReadyEvent;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.HopperBot.HopperBotCommandFeature;
import uk.co.hopperelec.HopperBot.HopperBotFeatures;
import uk.co.hopperelec.HopperBot.SimpleMySQLLoginDetails;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

public final class Economy extends HopperBotCommandFeature {
    public Economy() {
        super(HopperBotFeatures.economy, "$");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);

        final String dbLoginFilename = "economy_db.yml";
        final File dbLoginFile = Paths.get(System.getProperty("user.dir"),dbLoginFilename).toFile();
        try {
            if (dbLoginFile.createNewFile()) {
                getUtils().log(dbLoginFilename+" couldn't be found. An empty file has been created for you. Please enter the host, name, user and password into it",null,featureEnum);
                return;
            } else {
                getUtils().log("Found "+dbLoginFilename,null,featureEnum);
            }
        } catch (IOException e) {
            getUtils().log(dbLoginFilename+" couldn't be found and an empty file could not be created",null,featureEnum);
            return;
        }

        final SimpleMySQLLoginDetails dbLogin;
        try {
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            dbLogin = objectMapper.readValue(dbLoginFile, SimpleMySQLLoginDetails.class);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        getUtils().log("Loaded "+dbLoginFilename,null,featureEnum);

        final MysqlDataSource mysql = dbLogin.getDataSource();
        try (Connection conn = mysql.getConnection()) {
            if (conn.isValid(1000)) {
                getUtils().log("Successfully connected to database",null,featureEnum);
            } else {
                throw new SQLException("Could not establish database connection.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
