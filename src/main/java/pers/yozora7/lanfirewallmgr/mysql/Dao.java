package pers.yozora7.lanfirewallmgr.mysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import pers.yozora7.lanfirewallmgr.entity.Net;
import pers.yozora7.lanfirewallmgr.entity.Rule;
import pers.yozora7.lanfirewallmgr.entity.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;

import static pers.yozora7.lanfirewallmgr.utils.Utils.setToString;

/**
 * 数据库持久化操作类
 * 使用JDBC, 后续将迁移至MyBatis
 */
public class Dao {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private final String database;

    public Dao(JdbcTemplate template, String database) {
        this.database = database;
        this.jdbcTemplate = template;
        jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS " + database);
        jdbcTemplate.execute("USE " + database);

        // set(id,name)
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `set` ( " +
                "`id` INT(10) NOT NULL AUTO_INCREMENT," +
                "`name` VARCHAR(255), " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY IDX_NAME(`name`) " +
                ") ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4;");

        // net(id,start,end,set_id)
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `net` (" +
                "`id` INT(10) NOT NULL AUTO_INCREMENT," +
                "`start` VARCHAR(50)," +
                "`start_mask` INT(2) DEFAULT 32," +
                "`end` VARCHAR(50)," +
                "`end_mask` INT(2) DEFAULT 32," +
                "`set_id` INT(10)," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY IDX_ALL(`start`, `start_mask`, `end`, `end_mask`, `set_id`)," +
                "KEY IDX_SET_ID(`set_id`)" +
        ") ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4;");

        // service(id,name,protocol,src_start_port,dst_start_port,src_end_port,dst_end_port,group)
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `service` ( " +
                "`id` INT(10) NOT NULL AUTO_INCREMENT," +
                "`name` VARCHAR(255), " +
                "`protocol` VARCHAR(20), " +
                "`src_start_port` INT(10), " +
                "`src_end_port` INT(10), " +
                "`dst_start_port` INT(10), " +
                "`dst_end_port` INT(10), " +
                "`group` VARCHAR(255), " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY IDX_NAME(`name`), " +
                "KEY IDX_GROUP(`group`)" +
                ") ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4;");

        // zone(id,name)
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `zone` ( " +
                "`id` INT(10) NOT NULL AUTO_INCREMENT," +
                "`name` VARCHAR(255), " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY IDX_NAME(`name`) " +
                ") ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4;");

        // rule(id,name,src_zone,dst_zone,src_net_id,dst_net_id,src_set_id,dst_set_id,service_id,action)
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `rule` ( " +
                "`id` INT(10) NOT NULL AUTO_INCREMENT, " +
                "`name` VARCHAR(255), " +
                "`src_zone_id` TEXT, " +
                "`dst_zone_id` TEXT, " +
                "`src_net_id` TEXT, " +
                "`dst_net_id` TEXT, " +
                "`src_set_id` TEXT, " +
                "`dst_set_id` TEXT, " +
                "`service_id` TEXT, " +
                "`action` VARCHAR(255), " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY IDX_NAME(`name`) " +
        ") ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8;");
    }

    public int addSet (String set) {
        String query = "SELECT `id` FROM `set` WHERE `name` = '" + set + "';";
        jdbcTemplate.execute("USE " + database);
        int id = jdbcTemplate.query(query, rs -> rs.next() ? rs.getInt(1) : 0);
        if (id != 0) {
            return id;
        }
        else {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.execute("USE " + database);
            String insert = "INSERT INTO `set` (`name`) VALUES (?)";
            PreparedStatementCreator creator = connection -> {
                PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, set);
                return ps;
            };
            jdbcTemplate.update(creator, keyHolder);
            return keyHolder.getKey().intValue();
        }
    }

    public int addNet(Net data) {
        String query = "SELECT `id` FROM `net` WHERE `start` = '" + data.getStart()
                + "' AND `start_mask` = " + data.getStartMask()
                + " AND `end` = '" + data.getEnd()
                + "' AND `end_mask` = " + data.getEndMask()
                + " AND `set_id` = '" + data.getSetId() + "' LIMIT 1;";
        jdbcTemplate.execute("USE " + database);
        int id = jdbcTemplate.query(query, rs -> rs.next() ? rs.getInt(1) : 0);
        if (id != 0) {
            return id;
        }
        else {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.execute("USE " + database);
            String insert = "INSERT INTO `net` (`start`, `start_mask`, `end`, `end_mask`, `set_id`) VALUES (?, ?, ?, ?, ?);";
            PreparedStatementCreator creator = connection -> {
                PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, data.getStart());
                ps.setInt(2, data.getStartMask());
                ps.setString(3, data.getEnd());
                ps.setInt(4, data.getEndMask());
                ps.setInt(5, data.getSetId());
                return ps;
            };
            jdbcTemplate.update(creator, keyHolder);
            return keyHolder.getKey().intValue();
        }
    }

    public int addService(Service data) {
        String query = "SELECT `id` FROM `service` WHERE `name` = '" + data.getName() + "' LIMIT 1;";
        jdbcTemplate.execute("USE " + database);
        int id = jdbcTemplate.query(query, rs -> rs.next() ? rs.getInt(1) : 0);
        if (id != 0) {
            return id;
        }
        else {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.execute("USE " + database);
            String insert = "INSERT INTO `service` " +
                    "(`name`, `protocol`, `src_start_port`, `src_end_port`, `dst_start_port`, `dst_end_port`, `group`) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?);";
            PreparedStatementCreator creator = connection -> {
                PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, data.getName());
                ps.setString(2, data.getProtocol());
                ps.setInt(3, data.getSrcStartPort());
                ps.setInt(4, data.getSrcEndPort());
                ps.setInt(5, data.getDstStartPort());
                ps.setInt(6, data.getDstEndPort());
                ps.setString(7, data.getGroup());
                return ps;
            };
            jdbcTemplate.update(creator, keyHolder);
            return keyHolder.getKey().intValue();
        }
    }

    public void addGroup(String serviceName, String groupName) {
        String update = "UPDATE `service` SET `group` = ? WHERE `name` = ?;";
        PreparedStatementCreator creator = connection -> {
            PreparedStatement ps = connection.prepareStatement(update);
            ps.setString(1, groupName);
            ps.setString(2, serviceName);
            return ps;
        };
        jdbcTemplate.execute("USE " + database);
        jdbcTemplate.update(creator);
    }

    public int addZone (String zone) {
        String query = "SELECT `id` FROM `zone` WHERE `name` = '" + zone + "';";
        jdbcTemplate.execute("USE " + database);
        int id = jdbcTemplate.query(query, rs -> rs.next() ? rs.getInt(1) : 0);
        if (id != 0) {
            return id;
        }
        else {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.execute("USE " + database);
            String insert = "INSERT INTO `zone` (`name`) VALUES (?)";
            PreparedStatementCreator creator = connection -> {
                PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, zone);
                return ps;
            };
            jdbcTemplate.update(creator, keyHolder);
            return keyHolder.getKey().intValue();
        }
    }


    public int addRule(Rule data) {
        String query = "SELECT `id` FROM `rule` WHERE `name` = '" + data.getName() + "' LIMIT 1;";
        jdbcTemplate.execute("USE " + database);
        int id = jdbcTemplate.query(query, rs -> rs.next() ? rs.getInt(1) : 0);
        if (id != 0) {
            return id;
        }
        else {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.execute("USE " + database);
            String insert = "INSERT INTO `rule` " +
                    "(`name`, `src_zone_id`, `dst_zone_id`, `src_net_id`, `dst_net_id`, `src_set_id`, " +
                    "`dst_set_id`, `service_id`, `action`) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
            PreparedStatementCreator creator = connection -> {
                PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, data.getName());
                ps.setString(2, data.getSrcZoneIds());
                ps.setString(3, data.getDstZoneIds());
                ps.setString(4, data.getSrcNetIds());
                ps.setString(5, data.getDstNetIds());
                ps.setString(6, data.getSrcSetIds());
                ps.setString(7, data.getDstSetIds());
                ps.setString(8, data.getServiceIds());
                ps.setString(9, data.getAction());
                return ps;
            };
            jdbcTemplate.update(creator, keyHolder);
            return keyHolder.getKey().intValue();
        }
    }

    public boolean isServiceGroup (String group) {
        String query = "SELECT `id` FROM `service` WHERE `group` = '" + group + "' LIMIT 1;";
        jdbcTemplate.execute("USE " + database);
        return jdbcTemplate.query(query, rs -> rs.next() ? true : false);
    }

    public int count(String table) {
        String query = "SELECT COUNT(*) FROM " + table + ";";
        jdbcTemplate.execute("USE " + database);
        return jdbcTemplate.query(query, rs -> rs.next() ? rs.getInt(1) : 0);
    }
}
