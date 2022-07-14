package pers.yozora7.lanfirewallmgr.parser;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import pers.yozora7.lanfirewallmgr.entity.Net;
import pers.yozora7.lanfirewallmgr.entity.Rule;
import pers.yozora7.lanfirewallmgr.entity.Service;
import pers.yozora7.lanfirewallmgr.mysql.Dao;
import pers.yozora7.lanfirewallmgr.utils.Utils;
import pers.yozora7.lanfirewallmgr.xml.SAXParserHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static pers.yozora7.lanfirewallmgr.utils.Utils.longMaskToShort;

/**
 * 解析新华三(H3C)防火墙配置文件
 */
public class H3CParser implements Parser {
    private String config;  // 配置文件路径
    private Dao dao;    // 数据库操作类

    /**
     * 解析 & 存储
     * @param config
     * @param dao
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public void parse(String config, Dao dao) throws IOException, ParserConfigurationException, SAXException {
        this.config = config;
        this.dao = dao;
        parseNetSet();
        parseServiceSet();
        parseRule();
    }

    /**
     * 从XML读取正则表达式
     * @param nodeName
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private List<Map<String, String>> getRegex(String nodeName) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parse = factory.newSAXParser();
        XMLReader xmlReader = parse.getXMLReader();
        SAXParserHandler handler = new SAXParserHandler(nodeName);
        xmlReader.setContentHandler(handler);
        xmlReader.parse("src/main/resources/H3CRegex.xml");
        return handler.getList();
    }

    /**
     * 解析地址集合
     * 通用格式: object-group ip address
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void parseNetSet() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;   // 是否读取到地址集
        int count = dao.count("net");   // 数据库已有地址集数量
        int setId = 0;

        // 获取正则表达式
        Map<String, String> regex = getRegex("address").get(0);
        Pattern header = Pattern.compile(regex.get("header"));  // 地址集头部正则表达式
        Pattern host = Pattern.compile(regex.get("host"));      // 单IP正则表达式
        Pattern range = Pattern.compile(regex.get("range"));    // IP段正则表达式
        Pattern subnet = Pattern.compile(regex.get("subnet"));  // 子网正则表达式

        // 读取配置文件
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            // 文件结束
            if (line == null) {
                break;
            } else {
                line = line.trim().replaceAll("\\\\","/");
            }
            // 读取地址集头部, 存储至Set表
            if (header.matcher(line).find()) {
                String name = line.split(split)[3].replace("\"","");
                setId = dao.addSet(name);
                flag = true;    // 读取到地址集
                continue;
            }
            // 读取集合内的IP地址, 存储至Net表
            if (flag) {
                String[] temp = line.split("\\s+");
                Net data = new Net();
                data.setSetId(setId);
                // 单IP
                if (host.matcher(line).find()) {
                    data.setStart(temp[4]);
                    data.setStartMask(32);
                    data.setEnd(temp[4]);
                    data.setEndMask(32);
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
                // IP范围
                else if (range.matcher(line).find()) {
                    data.setStart(temp[3]);
                    data.setStartMask(32);
                    data.setEnd(temp[4]);
                    data.setEndMask(32);
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
                // 子网
                else if (subnet.matcher(line).find()) {
                    data.setStart(temp[3]);
                    data.setStartMask(longMaskToShort(temp[4]));
                    data.setEnd(temp[3]);
                    data.setEndMask(longMaskToShort(temp[4]));
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
            }
            // 地址集结尾
            if (line.equals("#")) {
                flag = false;
            }
        }
        reader.close();
    }

    /**
     * 解析自定义服务
     * 通用格式: object-group service
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void parseServiceSet() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        String name = null;
        int count = dao.count("service");

        // 获取正则表达式
        Map<String, String> regex = getRegex("service").get(0);
        Pattern header = Pattern.compile(regex.get("header"));          // 服务集头部正则表达式
        Pattern eq = Pattern.compile(regex.get("eq"));                  // 目的端口号
        Pattern range = Pattern.compile(regex.get("range"));            // 目的端口范围
        Pattern bothEq = Pattern.compile(regex.get("both-eq"));         // 源端和目的端都是指定端口号
        Pattern bothRange = Pattern.compile(regex.get("both-range"));   // 源端和目的端都是端口范围
        Pattern eqRange = Pattern.compile(regex.get("eq-range"));       // 源端是端口号, 目的端是端口范围
        Pattern rangeEq = Pattern.compile(regex.get("range-eq"));       // 源端是端口范围, 目的端是端口号

        // 读取配置文件
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            // 文件结束
            if (line == null) {
                break;
            } else {
                line = line.trim().replaceAll("\\\\","/");  // 替换反斜杠
            }
            // 读取服务集头部
            if (header.matcher(line).find()) {
                name = line.split(split)[2].replace("\"","");
                flag = true;
                continue;
            }
            if (flag) {
                // 读取服务内容, 存储至Service表
                Service data = new Service();
                data.setName(name);
                // 获取源端口和目的端口
                // eq 目的端口号
                if (eq.matcher(line).find()) {
                    String temp = line.split("\\s+")[4];
                    String protocol = line.split("\\s+")[2];
                    data.setProtocol(protocol);
                    data.setSrcStartPort(0);
                    data.setSrcEndPort(65535);
                    switch (temp) {
                        case "eq":
                            data.setDstStartPort(Integer.parseInt(line.split("\\s+")[5]));
                            data.setDstEndPort(data.getDstStartPort());
                            break;
                        case "lt":
                            data.setDstStartPort(0);
                            data.setDstEndPort(Integer.parseInt(line.split("\\s+")[5]));
                            break;
                        case "gt":
                            data.setDstStartPort(Integer.parseInt(line.split("\\s+")[5]));
                            data.setDstEndPort(65535);
                            break;
                    }
                    data.setId(count);
                    if (dao.addService(data) == count) {
                        count++;
                    }
                }
                // range 目的端口范围
                else if (range.matcher(line).find()) {
                    String protocol = line.split("\\s+")[2];
                    data.setProtocol(protocol);
                    data.setSrcStartPort(0);
                    data.setSrcEndPort(65535);
                    data.setDstStartPort(Integer.parseInt(line.split("\\s+")[5]));
                    data.setDstEndPort(Integer.parseInt(line.split("\\s+")[6]));
                    data.setId(count);
                    if (dao.addService(data) == count) {
                        count++;
                    }
                }
                // both-eq 源端口号和目的端口号
                else if (bothEq.matcher(line).find()) {
                    String protocol = line.split("\\s+")[2];
                    String temp1 = line.split("\\s+")[4];
                    String temp2 = line.split("\\s+")[7];
                    data.setProtocol(protocol);
                    switch (temp1) {
                        case "eq":
                            data.setSrcStartPort(Integer.parseInt(line.split("\\s+")[5]));
                            data.setSrcEndPort(data.getDstStartPort());
                            break;
                        case "lt":
                            data.setSrcStartPort(0);
                            data.setSrcEndPort(Integer.parseInt(line.split("\\s+")[5]));
                            break;
                        case "gt":
                            data.setSrcStartPort(Integer.parseInt(line.split("\\s+")[5]));
                            data.setSrcEndPort(65535);
                            break;
                    }
                    switch (temp2) {
                        case "eq":
                            data.setDstStartPort(Integer.parseInt(line.split("\\s+")[8]));
                            data.setDstEndPort(data.getDstStartPort());
                            break;
                        case "lt":
                            data.setDstStartPort(0);
                            data.setDstEndPort(Integer.parseInt(line.split("\\s+")[8]));
                            break;
                        case "gt":
                            data.setDstStartPort(Integer.parseInt(line.split("\\s+")[8]));
                            data.setDstEndPort(65535);
                            break;
                    }
                    data.setId(count);
                    if (dao.addService(data) == count) {
                        count++;
                    }
                }
                // both-range 源端口范围和目的端口范围
                else if (bothRange.matcher(line).find()) {
                    String protocol = line.split("\\s+")[2];
                    data.setProtocol(protocol);
                    data.setSrcStartPort(Integer.parseInt(line.split("\\s+")[5]));
                    data.setSrcEndPort(Integer.parseInt(line.split("\\s+")[6]));
                    data.setDstStartPort(Integer.parseInt(line.split("\\s+")[9]));
                    data.setDstEndPort(Integer.parseInt(line.split("\\s+")[10]));
                    data.setId(count);
                    if (dao.addService(data) == count) {
                        count++;
                    }
                }
                // range-dq 源端口范围和目的端口号
                else if (rangeEq.matcher(line).find()) {
                    String protocol = line.split("\\s+")[2];
                    String temp = line.split("\\s+")[8];
                    data.setProtocol(protocol);
                    data.setSrcStartPort(Integer.parseInt(line.split("\\s+")[5]));
                    data.setSrcEndPort(Integer.parseInt(line.split("\\s+")[6]));
                    switch (temp) {
                        case "eq":
                            data.setDstStartPort(Integer.parseInt(line.split("\\s+")[5]));
                            data.setDstEndPort(data.getDstStartPort());
                            break;
                        case "lt":
                            data.setDstStartPort(0);
                            data.setDstEndPort(Integer.parseInt(line.split("\\s+")[5]));
                            break;
                        case "gt":
                            data.setDstStartPort(Integer.parseInt(line.split("\\s+")[5]));
                            data.setDstEndPort(65535);
                            break;
                    }
                    data.setId(count);
                    if (dao.addService(data) == count) {
                        count++;
                    }
                }
                // eq-range 源端口号和目的端口范围
                else if (eqRange.matcher(line).find()) {
                    String protocol = line.split("\\s+")[2];
                    String temp = line.split("\\s+")[4];
                    data.setProtocol(protocol);
                    switch (temp) {
                        case "eq":
                            data.setSrcStartPort(Integer.parseInt(line.split("\\s+")[5]));
                            data.setSrcEndPort(data.getDstStartPort());
                            break;
                        case "lt":
                            data.setSrcStartPort(0);
                            data.setSrcEndPort(Integer.parseInt(line.split("\\s+")[5]));
                            break;
                        case "gt":
                            data.setSrcStartPort(Integer.parseInt(line.split("\\s+")[5]));
                            data.setSrcEndPort(65535);
                            break;
                    }
                    data.setDstStartPort(Integer.parseInt(line.split("\\s+")[8]));
                    data.setDstEndPort(Integer.parseInt(line.split("\\s+")[9]));
                    data.setId(count);
                    if (dao.addService(data) == count) {
                        count++;
                    }
                } else if (line.equals("#")) {
                    flag = false;
                }
            }
        }
        reader.close();
    }

    /**
     * 解析防火墙规则
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void parseRule() throws IOException, ParserConfigurationException, SAXException {
        Rule data = null;
        Boolean flag = false;
        int count = dao.count("rule");
        int countNet = dao.count("net");
        int countService = dao.count("service");

        // 存储规则中的地址/服务/安全域等条目在表中的id
        HashSet<Integer> srcSetIds = null;
        HashSet<Integer> dstSetIds = null;
        HashSet<Integer> srcNetIds = null;
        HashSet<Integer> dstNetIds = null;
        HashSet<Integer> srcZoneIds = null;
        HashSet<Integer> dstZoneIds = null;
        HashSet<Integer> serviceIds = null;

        // 获取正则表达式
        Map<String, String> regex = getRegex("rule").get(0);
        Pattern header = Pattern.compile(regex.get("header"));
        Pattern action = Pattern.compile(regex.get("action"));
        Pattern srcZone = Pattern.compile(regex.get("src-zone"));
        Pattern srcSet = Pattern.compile(regex.get("src-set"));
        Pattern srcHost = Pattern.compile(regex.get("src-host"));
        Pattern srcSubnet = Pattern.compile(regex.get("src-subnet"));
        Pattern dstZone = Pattern.compile(regex.get("dst-zone"));
        Pattern dstSet = Pattern.compile(regex.get("dst-set"));
        Pattern dstHost = Pattern.compile(regex.get("dst-host"));
        Pattern dstSubnet = Pattern.compile(regex.get("dst-subnet"));
        Pattern serviceName = Pattern.compile(regex.get("service-name"));

        // 读取配置文件
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            // 文件结束
            if (line == null) {
                break;
            } else {
                line = line.trim().replaceAll("\\\\","/");  // 替换反斜杠
            }
            // 读取规则头部
            if (header.matcher(line).find()) {
                // 保存上一条规则
                if (flag && data != null) {
                    data.setSrcSetIds(Utils.setToString(srcSetIds, Integer.class));
                    data.setSrcNetIds(Utils.setToString(srcNetIds, Integer.class));
                    data.setSrcZoneIds(Utils.setToString(srcZoneIds, Integer.class));
                    data.setDstSetIds(Utils.setToString(dstSetIds, Integer.class));
                    data.setDstNetIds(Utils.setToString(dstNetIds, Integer.class));
                    data.setDstZoneIds(Utils.setToString(dstZoneIds, Integer.class));
                    data.setServiceIds(Utils.setToString(serviceIds, Integer.class));
                    data.setId(count);
                    if (dao.addRule(data) == count) {
                        count++;
                    }
                }
                // 新的规则
                data = new Rule();
                srcSetIds = new HashSet<>();
                dstSetIds = new HashSet<>();
                srcNetIds = new HashSet<>();
                dstNetIds = new HashSet<>();
                srcZoneIds = new HashSet<>();
                dstZoneIds = new HashSet<>();
                serviceIds = new HashSet<>();
                data.setName(line.split(split)[3].replace("\"",""));    // 规则名称
                flag = true;
            }
            // 读取规则内容
            if (flag) {
                // 动作 (pass/block/reject/...)
                if (action.matcher(line).find()) {
                    data.setAction(line.replace("action", "").trim());
                }
                // 源安全域
                else if (srcZone.matcher(line).find()) {
                    srcZoneIds.add(dao.addZone(line.split(split)[1].replace("\"","")));
                }
                // 源地址集
                else if (srcSet.matcher(line).find()) {
                    srcSetIds.add(dao.addSet(line.split(split)[1].replace("\"","")));
                }
                // 源地址(单IP)
                else if (srcHost.matcher(line).find()) {
                    Net net = new Net();
                    net.setStart(line.split(split)[1]);
                    net.setStartMask(32);
                    net.setEnd(net.getStart());
                    net.setEndMask(32);
                    net.setSetId(0);
                    net.setId(countNet);
                    int id = dao.addNet(net);
                    srcNetIds.add(id);
                    if (id == countNet) {
                        countNet++;
                    }
                }
                // 源地址(子网)
                else if (srcSubnet.matcher(line).find()) {
                    Net net = new Net();
                    net.setStart(line.split(split)[1]);
                    net.setStartMask(Utils.longMaskToShort(line.split(split)[2]));
                    net.setEnd(net.getStart());
                    net.setEndMask(net.getStartMask());
                    net.setSetId(0);
                    net.setId(countNet);
                    int id = dao.addNet(net);
                    srcNetIds.add(id);
                    if (id == countNet) {
                        countNet++;
                    }
                }
                // 目标安全域
                else if (dstZone.matcher(line).find()) {
                    dstZoneIds.add(dao.addZone(line.split(split)[1].replace("\"","")));
                }
                // 目标地址集
                else if (dstSet.matcher(line).find()) {
                    dstSetIds.add(dao.addSet(line.split(split)[1].replace("\"","")));
                }
                // 目标地址(单IP)
                else if (dstHost.matcher(line).find()) {
                    Net net = new Net();
                    net.setStart(line.split(split)[1]);
                    net.setStartMask(32);
                    net.setEnd(net.getStart());
                    net.setEndMask(32);
                    net.setSetId(0);
                    net.setId(countNet);
                    int id = dao.addNet(net);
                    dstNetIds.add(id);
                    if (id == countNet) {
                        countNet++;
                    }
                }
                // 目标地址(子网)
                else if (dstSubnet.matcher(line).find()) {
                    Net net = new Net();
                    net.setStart(line.split(split)[1]);
                    net.setStartMask(Utils.longMaskToShort(line.split(split)[2]));
                    net.setEnd(net.getStart());
                    net.setEndMask(net.getStartMask());
                    net.setSetId(0);
                    net.setId(countNet);
                    int id = dao.addNet(net);
                    dstNetIds.add(id);
                    if (id == countNet) {
                        countNet++;
                    }
                }
                // 服务
                else if (serviceName.matcher(line).find()) {
                    String name = line.split(split)[1].replace("\"","");
                    Service service = new Service();
                    service.setName(name);
                    service.setId(countService);
                    int id = dao.addService(service);
                    serviceIds.add(id);
                    if (id == countService) {
                        countService++;
                    }
                }
                // 读到#号保存最后一条规则
                else if (line.equals("#")) {
                    flag = false;
                    data.setSrcSetIds(Utils.setToString(srcSetIds, Integer.class));
                    data.setSrcNetIds(Utils.setToString(srcNetIds, Integer.class));
                    data.setSrcZoneIds(Utils.setToString(srcZoneIds, Integer.class));
                    data.setDstSetIds(Utils.setToString(dstSetIds, Integer.class));
                    data.setDstNetIds(Utils.setToString(dstNetIds, Integer.class));
                    data.setDstZoneIds(Utils.setToString(dstZoneIds, Integer.class));
                    data.setServiceIds(Utils.setToString(serviceIds, Integer.class));
                    data.setId(count);
                    if (dao.addRule(data) == count) {
                        count++;
                    }
                }
            }
        }
    }
}
