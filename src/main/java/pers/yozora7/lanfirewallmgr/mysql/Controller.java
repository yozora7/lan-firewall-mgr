package pers.yozora7.lanfirewallmgr.mysql;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;
import pers.yozora7.lanfirewallmgr.entity.Result;
import pers.yozora7.lanfirewallmgr.parser.HuaweiConfParser;
import pers.yozora7.lanfirewallmgr.parser.QMXCConfParser;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/jdbc")
public class Controller {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @RequestMapping(value = "/parse", method = RequestMethod.POST)
    public String parse (@RequestParam String database ,@RequestParam String config, @RequestParam String type) throws IOException, ParserConfigurationException, SAXException {
        Dao dao = new Dao(jdbcTemplate, database);
        log.info("{}", database);
        if (type.equals("huawei")) {
            HuaweiConfParser parser = new HuaweiConfParser();
            parser.parse(config, dao);
        }
        else if (type.equals("QMXC")) {
            QMXCConfParser parser = new QMXCConfParser();
            parser.parse(config, dao);
        }
        return "success";
    }

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    public String query(@RequestParam String database ,@RequestParam String ip) throws SQLException {
        Gson gson = new Gson();
        Query query = new Query(jdbcTemplate, database, ip, 10);
        Set<Result> results = query.queryRules();
        return gson.toJson(results);
    }

}
