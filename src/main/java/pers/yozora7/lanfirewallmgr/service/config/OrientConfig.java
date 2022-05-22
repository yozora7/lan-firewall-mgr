package pers.yozora7.lanfirewallmgr.service.config;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class OrientConfig {
    @Value("${orient.url}")
    String url;

    @Value("${orient.database}")
    String database;

    @Value("${orient.username}")
    String username;

    @Value("${orient.password}")
    String password;

    @Bean
    public ODatabaseSession oDatabaseSession() {
        OrientDB orient = new OrientDB(url, OrientDBConfig.defaultConfig());
        ODatabaseSession oDatabaseSession = orient.open(database, username, password);
        return oDatabaseSession;
    }
}
