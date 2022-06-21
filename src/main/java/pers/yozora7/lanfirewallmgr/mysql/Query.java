package pers.yozora7.lanfirewallmgr.mysql;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import pers.yozora7.lanfirewallmgr.entity.Result;
import pers.yozora7.lanfirewallmgr.utils.Utils;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class Query {
    private JdbcTemplate jdbcTemplate;
    private String database;
    private String ip;
    private int batch;

    public Query (JdbcTemplate template, String database, String ip, int batch) {
        this.jdbcTemplate = template;
        this.database = database;
        this.ip = ip;
        this.batch = batch;
    }

    public Set<Result> queryRules () throws SQLException {
        Set<Result> results = new HashSet<>();
        jdbcTemplate.execute("USE " + database);
        int count = 0;
        int total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `net`", Integer.class);
        while (count + batch <= total) {
            String sql = "SELECT `id`, `start`, `end`, `set_id` FROM `net` WHERE `id` > " + count
                    + " AND `id` <= " + (count + batch) + ";";
            count += batch;
            SqlRowSet rs = jdbcTemplate.queryForRowSet(sql);
            while (rs.next()) {
                int id = rs.getInt(1);
                String start = rs.getString(2);
                String end = rs.getString(3);
                String startIp = start.split("/")[0];
                String endIp = end.split("/")[0];
                int setId = rs.getInt(4);
                if (end.equals(start) ? Utils.isIpInCidr(ip, start) : Utils.isIpInRange(ip, startIp, endIp)) {
                    String sql2 = "SELECT `id`, `name`, `src_zone`, `dst_zone`, `src_net_id`, `src_set_id`, `service_id`, `action` " +
                            "FROM `rule` " +
                            "WHERE `dst_net_id` = '" + id + "'" +
                            " OR `dst_net_id` LIKE '%," + id + ",%'" +
                            " OR `dst_net_id` LIKE '" + id + ",%'" +
                            " OR `dst_net_id` LIKE '%," + id + "'" +
                            " OR `dst_set_id` = '" + setId + "'" +
                            " OR `dst_set_id` LIKE '%," + setId + ",%'" +
                            " OR `dst_set_id` LIKE '" + setId + ",%'" +
                            " OR `dst_set_id` LIKE '%," + setId + "';";
                    SqlRowSet rs2 = jdbcTemplate.queryForRowSet(sql2);
                    while (rs2.next()) {
                        Result result = new Result();
                        result.setRuleId(rs2.getInt(1));
                        result.setRuleName(rs2.getString(2));
                        result.setSrcZone(rs2.getString(3));
                        result.setDstZone(rs2.getString(4));
                        result.setSrcNetIds(rs2.getString(5));
                        result.setSrcSetIds(rs2.getString(6));
                        result.setServiceIds(rs2.getString(7));
                        result.setAction(rs2.getString(8));
                        results.add(result);
                    }
                }
            }
        }
        return results;
    }
}
