package pers.yozora7.lanfirewallmgr.mysql;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;
import pers.yozora7.lanfirewallmgr.entity.Result;
import pers.yozora7.lanfirewallmgr.parser.H3CParser;
import pers.yozora7.lanfirewallmgr.parser.HuaweiParser;
import pers.yozora7.lanfirewallmgr.parser.QMXCParser;

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

    // 解析防火墙配置文件
    @RequestMapping(value = "/parse", method = RequestMethod.POST)
    public String parse (@RequestParam String database ,@RequestParam String config, @RequestParam String type) throws IOException, ParserConfigurationException, SAXException {
        Dao dao = new Dao(jdbcTemplate, database);
        log.info("{}", database);
        // 根据品牌使用不同的解析器
        switch (type) {
            case "huawei": {
                HuaweiParser parser = new HuaweiParser();
                parser.parse(config, dao);
                break;
            }
            case "QMXC": {
                QMXCParser parser = new QMXCParser();
                parser.parse(config, dao);
                break;
            }
            case "H3C": {
                H3CParser parser = new H3CParser();
                parser.parse(config, dao);
                break;
            }
        }
        return "success";
    }

    // 简单查询接口示例(查询目标IP对应的规则内容)
    @RequestMapping(value = "/query", method = RequestMethod.GET)
    public String query(@RequestParam String database ,@RequestParam String ip) throws SQLException {
        Gson gson = new Gson();
        Query query = new Query(jdbcTemplate, database, ip, 10);
        Set<Result> results = query.queryRules();
        return gson.toJson(results);
    }

}
