package uk.co.hopperelec.hopperbot.features.economy;

import com.mysql.cj.jdbc.MysqlDataSource;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.ReadyEvent;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotCommandFeature;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;
import uk.co.hopperelec.hopperbot.SimpleMySQLLoginDetails;

import java.sql.Connection;
import java.sql.SQLException;

public final class EconomyFeature extends HopperBotCommandFeature {
    private static final String dbLoginFileLocation = "economy_db.yml";

    public EconomyFeature(@NotNull JDABuilder builder) {
        super(builder,HopperBotFeatures.ECONOMY, "$");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);

        final SimpleMySQLLoginDetails dbLogin = getUtils().getYAMLFile(featureEnum,dbLoginFileLocation,SimpleMySQLLoginDetails.class);
        if (dbLogin != null) {
            final MysqlDataSource mysql = dbLogin.getDataSource();
            try (Connection conn = mysql.getConnection()) {
                if (conn.isValid(1000)) {
                    getUtils().logGlobally("Successfully connected to database",featureEnum);
                } else {
                    throw new SQLException("Could not establish database connection.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
