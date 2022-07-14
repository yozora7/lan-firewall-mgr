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
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import static pers.yozora7.lanfirewallmgr.utils.Utils.longMaskToShort;
import static pers.yozora7.lanfirewallmgr.utils.Utils.wildcardToMask;

/**
 * 解析Huawei防火墙配置文件
 */
public class HuaweiParser implements Parser {
    private String config;  // 防火墙配置文件路径
    private Dao dao;        // 数据库操作类

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
        parseServiceGroup();
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
        xmlReader.parse("src/main/resources/HuaweiRegex.xml");
        return handler.getList();
    }

    /**
     * 解析地址集合
     * 通用格式: ip address-set
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
        Pattern header = Pattern.compile(regex.get("header"));      // 地址集头部正则表达式
        Pattern host = Pattern.compile(regex.get("host"));          // 单IP正则表达式
        Pattern range = Pattern.compile(regex.get("range"));        // IP段正则表达式
        Pattern mask = Pattern.compile(regex.get("mask"));          // 带CIDR掩码正则表达式
        Pattern longMask = Pattern.compile(regex.get("long-mask")); // 带掩码正则表达式
        Pattern wildcard = Pattern.compile(regex.get("wildcard"));  // 带反掩码正则表达式

        // 读取配置文件
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            // 文件结束
            if (line == null) {
                break;
            }
            else {
                line = line.trim().replaceAll("\\\\","/");  // 替换反斜杠
            }

            // 读取地址集头部, 存储至Set表
            if (header.matcher(line).find()) {
                setId = dao.addSet(line.split(split)[2].replace("\"",""));
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
                    data.setStart(temp[2]);
                    data.setStartMask(32);
                    data.setEnd(temp[2]);
                    data.setEndMask(32);
                    data.setId(count);
                    // 存储至数据库
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
                // CIDR (带2位数掩码)
                else if (mask.matcher(line).find()) {
                    data.setStart(temp[2]);
                    data.setStartMask(32);
                    data.setEnd(temp[2]);
                    data.setEndMask(32);
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
                // 带掩码 (xxx.xxx.xxx.xxx)
                else if (longMask.matcher(line).find()) {
                    data.setStart(temp[2]);
                    data.setStartMask(longMaskToShort(temp[4]));
                    data.setEnd(temp[2]);
                    data.setEndMask(longMaskToShort(temp[4]));
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
                // 带反掩码
                else if (wildcard.matcher(line).find()) {
                    data.setStart(temp[2]);
                    data.setStartMask(wildcardToMask(temp[3]));
                    data.setEnd(temp[2]);
                    data.setEndMask(wildcardToMask(temp[3]));
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
            }
            // 地址集结尾
            if (line.equals("#")) {
                flag = false;   // 重置flag
            }
        }
        reader.close();
    }

    /**
     * 解析自定义服务
     * 通用格式: ip service-set type object
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void parseServiceSet() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;   // 是否读取到服务内容
        String name = null;     // 服务名称
        int count = dao.count("service");

        // 获取正则表达式
        Map<String, String> regex = getRegex("service-set").get(0);
        Pattern header = Pattern.compile(regex.get("header"));      // 服务开头正则表达式
        Pattern content = Pattern.compile(regex.get("content"));    // 服务内容正则表达式

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
            // 读取服务头部
            if (header.matcher(line).find()) {
                name = line.split(split)[2].replace("\"","");
                flag = true;
                continue;
            }
            if (flag) {
                // 读取服务内容, 存储至Service表
                if (content.matcher(line).find()) {
                    Service data = new Service();
                    data.setName(name);
                    data.setProtocol(line.split("\\s+")[3]);
                    // 获取源端口和目的端口
                    String[] srcPorts = line.split("source-port|destination-port")[1].replaceAll("\\s+", "").split("to");
                    String[] dstPorts = line.split("source-port|destination-port")[2].replaceAll("\\s+", "").split("to");
                    data.setSrcStartPort(Integer.parseInt(srcPorts[0]));
                    data.setDstStartPort(Integer.parseInt(dstPorts[0]));
                    if (srcPorts.length > 1) {
                        data.setSrcEndPort(Integer.parseInt(srcPorts[1]));
                    } else {
                        data.setSrcEndPort(Integer.parseInt(srcPorts[0]));
                    }
                    if (dstPorts.length > 1) {
                        data.setDstEndPort(Integer.parseInt(dstPorts[1]));
                    } else {
                        data.setDstEndPort(Integer.parseInt(dstPorts[0]));
                    }
                    data.setId(count);
                    // 存储至数据库
                    if (dao.addService(data) == count) {
                        count++;
                    }
                }
                // 服务结尾
                else if (line.equals("#")) {
                    flag = false;   // 重置flag
                }
            }
        }
        reader.close();
    }

    /**
     * 解析服务集合
     * 通用格式: ip service-set type group
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void parseServiceGroup() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;   // 是否读取到服务集
        String group = null;    // 服务集名称

        // 获取正则表达式
        Map<String, String> regex = getRegex("service-group").get(0);
        Pattern header = Pattern.compile(regex.get("header"));    // 服务集开头正则表达式
        Pattern set = Pattern.compile(regex.get("set"));    // 服务集内容正则表达式(存储服务名称)

        // 读取配置文件
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            // 文件结束
            if (line == null) {
                break;
            }
            else {
                line = line.trim().replaceAll("\\\\","/");  // 替换反斜杠
            }
            // 读取服务集头部
            if (header.matcher(line).find()) {
                group = line.split(split)[2].replace("\"","");
                flag = true;
                continue;
            }
            // 读取集合内服务名称, 将集合名称追加至Service表
            if (flag) {
                if (set.matcher(line).find()) {
                    String service = line.split(split)[3].replace("\"","");
                    dao.addGroup(service, group);
                }
                // 服务集结尾
                else if (line.equals("#")) {
                    flag = false;   // 重置flag
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
        boolean flag = false;
        Rule data = null;
        Net net;
        int count = dao.count("rule");
        int countNet = dao.count("net");
        int countService = dao.count("service");

        // 存储规则中的地址/服务/安全域等条目在表中的id
        HashSet<Integer> srcSetIds = null;
        HashSet<Integer> dstSetIds = null;
        HashSet<Integer> srcZoneIds = null;
        HashSet<Integer> dstZoneIds = null;
        HashSet<Integer> srcNetIds = null;
        HashSet<Integer> dstNetIds = null;
        HashSet<Integer> serviceIds = null;
        HashSet<String> serviceGroups = null;

        // 获取正则表达式
        Map<String, String> regex = getRegex("rule").get(0);
        Pattern header = Pattern.compile(regex.get("header"));                  // 规则开头正则表达式
        Pattern srcZone = Pattern.compile(regex.get("src-zone"));               // 源安全域
        Pattern srcSet = Pattern.compile(regex.get("src-set"));                 // 源地址集
        Pattern srcMask = Pattern.compile(regex.get("src-mask"));               // 源CIDR地址
        Pattern srcRange = Pattern.compile(regex.get("src-range"));             // 源地址段
        Pattern dstZone = Pattern.compile(regex.get("dst-zone"));               // 目标安全域
        Pattern dstSet = Pattern.compile(regex.get("dst-set"));                 // 目标地址集
        Pattern dstMask = Pattern.compile(regex.get("dst-mask"));               // 目标CIDR地址
        Pattern dstRange = Pattern.compile(regex.get("dst-range"));             // 目标地址段
        Pattern serviceName = Pattern.compile(regex.get("service-name"));       // 服务名称
        Pattern app = Pattern.compile(regex.get("app"));                        // 应用名称
        Pattern serviceContent = Pattern.compile(regex.get("service-content")); // 服务内容
        Pattern action = Pattern.compile(regex.get("action"));                  // 动作(permit/deny)

        // 读取配置文件
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            // 文件结束
            if (line == null) {
                break;
            }
            else {
                line = line.trim().replaceAll("\\\\","/");  // 替换反斜杠
            }
            // 读取规则头部
            if (header.matcher(line).find()) {
                data = new Rule();
                data.setName(line.split(split)[2].replace("\"",""));
                srcSetIds = new HashSet<>();
                dstSetIds = new HashSet<>();
                srcNetIds = new HashSet<>();
                dstNetIds = new HashSet<>();
                srcZoneIds = new HashSet<>();
                dstZoneIds = new HashSet<>();
                serviceIds = new HashSet<>();
                serviceGroups = new HashSet<>();
                flag = true;
            }
            // 读取规则内容
            if (flag) {
                // 源安全域
                if (srcZone.matcher(line).find()) {
                    srcZoneIds.add(dao.addZone(line.split(split)[1].replace("\"","")));
                }
                // 源地址集
                else if (srcSet.matcher(line).find()) {
                    srcSetIds.add(dao.addSet(line.split(split)[2].replace("\"","")));
                }
                // 源CIDR
                else if (srcMask.matcher(line).find()) {
                    net = new Net();
                    net.setStart(line.split("\\s+")[1]);
                    net.setStartMask(longMaskToShort(line.split("\\s+")[3]));
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
                // 源地址段
                else if (srcRange.matcher(line).find()) {
                    net = new Net();
                    net.setStart(line.split("\\s+")[2]);
                    net.setStartMask(32);
                    net.setEnd(line.split("\\s+")[3]);
                    net.setEndMask(32);
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
                    dstSetIds.add(dao.addSet(line.split(split)[2].replace("\"","")));
                }
                // 目标CIDR
                else if (dstMask.matcher(line).find()) {
                    net = new Net();
                    net.setStart(line.split("\\s+")[1]);
                    net.setStartMask(longMaskToShort(line.split("\\s+")[3]));
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
                // 目标地址段
                else if (dstRange.matcher(line).find()) {
                    net = new Net();
                    net.setStart(line.split("\\s+")[2]);
                    net.setStartMask(32);
                    net.setEnd(line.split("\\s+")[3]);
                    net.setEndMask(32);
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
                    // group
                    if (dao.isServiceGroup(name)) {
                        serviceGroups.add(name);
                    }
                    // set
                    else {
                        Service service = new Service();
                        service.setName(name);
                        service.setId(countService);
                        int id = dao.addService(service);
                        serviceIds.add(id);
                        if (id == countService) {
                            countService++;
                        }
                    }
                }
                // 应用(视为服务)
                else if (app.matcher(line).find()) {
                    Service service = new Service();
                    service.setName(line.split(split)[2].replace("\"",""));
                    service.setId(countService);
                    int id = dao.addService(service);
                    serviceIds.add(id);
                    if (id == countService) {
                        countService++;
                    }
                }
                // 新建服务
                else if (serviceContent.matcher(line).find()) {
                    Service service = new Service();
                    String protocol = line.split("\\s+")[2];
                    service.setProtocol(protocol);
                    String[] dstPorts = line.split("destination-port")[1].replaceAll("\\s+","").split("to");
                    service.setDstStartPort(Integer.parseInt(dstPorts[0]));
                    if (dstPorts.length > 1) {
                        service.setDstEndPort(Integer.parseInt(dstPorts[1]));
                        // 服务命名:协议_目标端口
                        service.setName(protocol + "_" + dstPorts[0] + "_" + dstPorts[1]);
                    } else {
                        service.setDstEndPort(Integer.parseInt(dstPorts[0]));
                        service.setName(protocol + "_" + dstPorts[0]);
                    }
                    service.setId(countService);
                    int id = dao.addService(service);
                    serviceIds.add(id);
                    if (id == countService) {
                        countService++;
                    }
                }
                // 动作(permit/deny)
                else if (action.matcher(line).find()) {
                    flag = false;
                    // id集合转换为字符串
                    data.setSrcSetIds(Utils.setToString(srcSetIds, Integer.class));
                    data.setSrcNetIds(Utils.setToString(srcNetIds, Integer.class));
                    data.setSrcZoneIds(Utils.setToString(srcZoneIds, Integer.class));
                    data.setDstSetIds(Utils.setToString(dstSetIds, Integer.class));
                    data.setDstNetIds(Utils.setToString(dstNetIds, Integer.class));
                    data.setDstZoneIds(Utils.setToString(dstZoneIds, Integer.class));
                    data.setServiceIds(Utils.setToString(serviceIds, Integer.class));
                    data.setServiceGroups(Utils.setToString(serviceGroups, String.class));
                    data.setAction(line.replace("action","").trim());
                    data.setId(count);
                    // 添加到Rule表
                    if (dao.addRule(data) == count) {
                        count++;
                    }
                }
            }
        }
    }
}