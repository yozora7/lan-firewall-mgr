package pers.yozora7.lanfirewallmgr.parser;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import pers.yozora7.lanfirewallmgr.entity.Net;
import pers.yozora7.lanfirewallmgr.entity.Rule;
import pers.yozora7.lanfirewallmgr.entity.Service;
import pers.yozora7.lanfirewallmgr.mysql.Dao;
import pers.yozora7.lanfirewallmgr.xml.SAXParserHandler;
import pers.yozora7.lanfirewallmgr.utils.Utils;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 解析启明星辰防火墙配置文件
 */
public class QMXCParser implements Parser {
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
        xmlReader.parse("src/main/resources/QMXCRegex.xml");
        return handler.getList();
    }

    /**
     * 解析地址集合
     * 通用格式: address
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void parseNetSet() throws IOException, ParserConfigurationException, SAXException {
        int count = dao.count("net");
        int setId = 0;

        // 读取正则表达式
        Map<String, String> regex = getRegex("address").get(0);
        Pattern header = Pattern.compile(regex.get("header"));  // 地址头部正则表达式
        Pattern net = Pattern.compile(regex.get("net"));        // CIDR
        Pattern host = Pattern.compile(regex.get("host"));      // 单IP
        Pattern range = Pattern.compile(regex.get("range"));    // IP范围

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
                setId = dao.addSet(line.split(split)[1].replace("\"",""));
                continue;
            }
            // 读取集合内的IP地址, 存储至Net表
            String[] temp = line.split("\\s+");
            // CIDR
            if (net.matcher(line).find()) {
                Net data = new Net();
                data.setSetId(setId);
                data.setStart(temp[1].split("/")[0]);
                data.setStartMask(Integer.parseInt(temp[1].split("/")[1]));
                data.setEnd(data.getStart());
                data.setEndMask(data.getStartMask());
                data.setId(count);
                if (dao.addNet(data) == count) {
                    count++;
                }
            }
            // 单IP
            else if (host.matcher(line).find()) {
                Net data = new Net();
                data.setSetId(setId);
                data.setStart(temp[1]);
                data.setStartMask(32);
                data.setEnd(temp[1]);
                data.setEndMask(32);
                data.setId(count);
                if (dao.addNet(data) == count) {
                    count++;
                }
            }
            // IP范围
            else if (range.matcher(line).find()) {
                Net data = new Net();
                data.setSetId(setId);
                data.setStart(temp[1]);
                data.setStartMask(32);
                data.setEnd(temp[2]);
                data.setEndMask(32);
                data.setId(count);
                if (dao.addNet(data) == count) {
                    count++;
                }
            }
        }
        reader.close();
    }

    /**
     * 解析服务集合
     * 通用格式: service
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void parseServiceSet() throws IOException, ParserConfigurationException, SAXException {
        String name = null;
        int count = dao.count("service");

        // 读取正则表达式
        Map<String, String> regex = getRegex("service").get(0);
        Pattern header = Pattern.compile(regex.get("header"));  // 服务集头部正则表达式
        Pattern both = Pattern.compile(regex.get("both"));      // 源端口和目标端口
        Pattern dst = Pattern.compile(regex.get("dst"));        // 目标端口
        Pattern src = Pattern.compile(regex.get("src"));        // 源端口

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
            // 读取服务名称
            if (header.matcher(line).find()) {
                name = line.split(split)[1].replace("\"","");
                continue;
            }
            // 目标端口和源端口
            if (both.matcher(line).find()) {
                Service data = new Service();
                data.setName(name);
                String protocol = line.split("\\s+")[0];
                data.setProtocol(protocol);
                String[] srcPorts = line.split("dest|source")[2].trim().split("\\s+");
                String[] dstPorts = line.split("dest|source")[1].trim().split("\\s+");
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
                if (dao.addService(data) == count) {
                    count++;
                }
            }
            // 目标端口
            else if (dst.matcher(line).find()) {
                Service data = new Service();
                data.setName(name);
                String protocol = line.split("\\s+")[0];
                data.setProtocol(protocol);
                data.setSrcStartPort(0);
                data.setSrcEndPort(0);
                data.setDstStartPort(Integer.parseInt(line.split("\\s+")[2]));
                if (line.split("\\s+").length > 3) {
                    data.setDstEndPort(Integer.parseInt(line.split("\\s+")[3]));
                } else {
                    data.setDstEndPort(data.getDstStartPort());
                }
                data.setId(count);
                if (dao.addService(data) == count) {
                    count++;
                }
            }
            // 目标端口
            else if (src.matcher(line).find()) {
                Service data = new Service();
                data.setName(name);
                String protocol = line.split("\\s+")[0];
                data.setProtocol(protocol);
                data.setDstStartPort(0);
                data.setDstEndPort(0);
                data.setSrcStartPort(Integer.parseInt(line.split("\\s+")[2]));
                if (line.split("\\s+").length > 3) {
                    data.setSrcEndPort(Integer.parseInt(line.split("\\s+")[3]));
                } else {
                    data.setSrcEndPort(data.getSrcStartPort());
                }
                data.setId(count);
                //
                if (dao.addService(data) == count) {
                    count++;
                }
            }
        }
    }

    /**
     * 解析防火墙规则
     * 通用格式: firewall policy
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void parseRule() throws IOException, ParserConfigurationException, SAXException {
        Rule data = null;
        boolean flag = false;
        String policyNum = "";
        int count = dao.count("rule");
        int countService = dao.count("service");

        // 存储规则中的地址/服务/安全域等条目在表中的id
        HashSet<Integer> srcSetIds = null;
        HashSet<Integer> dstSetIds = null;
        HashSet<Integer> srcNetIds = null;
        HashSet<Integer> dstNetIds = null;
        HashSet<Integer> srcZoneIds = null;
        HashSet<Integer> dstZoneIds = null;
        HashSet<Integer> serviceIds = null;

        // 读取正则表达式
        Map<String, String> regex = getRegex("rule").get(0);
        Pattern header = Pattern.compile(regex.get("header"));
        Pattern ruleName = Pattern.compile(regex.get("name"));
        Pattern action = Pattern.compile(regex.get("action"));
        Pattern srcZone = Pattern.compile(regex.get("src-zone"));
        Pattern dstZone = Pattern.compile(regex.get("dst-zone"));
        Pattern srcSet = Pattern.compile(regex.get("src-set"));
        Pattern dstSet = Pattern.compile(regex.get("dst-set"));
        Pattern serviceName = Pattern.compile(regex.get("service-name"));
        Pattern app = Pattern.compile(regex.get("app"));

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
                flag = true;
                data = new Rule();
                srcNetIds = new HashSet<>();
                dstNetIds = new HashSet<>();
                srcSetIds = new HashSet<>();
                dstSetIds = new HashSet<>();
                srcZoneIds = new HashSet<>();
                dstZoneIds = new HashSet<>();
                serviceIds = new HashSet<>();
                policyNum = line.split("\\s+")[2];
            }
            if (flag) {
                // 规则名称
                if (ruleName.matcher(line).find()) {
                    data.setName(line.split(split)[1].replace("\"",""));
                }
                // 动作
                else if (action.matcher(line).find()) {
                    data.setAction(line.replace("action","").trim());
                }
                // 源安全域
                else if (srcZone.matcher(line).find()) {
                    srcZoneIds.add(dao.addZone(line.split(split)[1].replace("\"","")));
                }
                // 目标安全域
                else if (dstZone.matcher(line).find()) {
                    dstZoneIds.add(dao.addZone(line.split(split)[1].replace("\"","")));
                }
                // 源地址集
                else if (srcSet.matcher(line).find()) {
                    srcSetIds.add(dao.addSet(line.split(split)[1].replace("\"","")));
                }
                // 目标地址集
                else if (dstSet.matcher(line).find()) {
                    dstSetIds.add(dao.addSet(line.split(split)[1].replace("\"","")));
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
                // 应用(不再记录, 直接保存规则)
                else if (app.matcher(line).find()) {
                    data.setSrcSetIds(Utils.setToString(srcSetIds, Integer.class));
                    data.setSrcNetIds(Utils.setToString(srcNetIds, Integer.class));
                    data.setSrcZoneIds(Utils.setToString(srcZoneIds, Integer.class));
                    data.setDstSetIds(Utils.setToString(dstSetIds, Integer.class));
                    data.setDstNetIds(Utils.setToString(dstNetIds, Integer.class));
                    data.setDstZoneIds(Utils.setToString(dstZoneIds, Integer.class));
                    data.setServiceIds(Utils.setToString(serviceIds, Integer.class));
                    data.setId(count);
                    // 没有指定name, 则使用规则编号
                    if (data.getName() == null) {
                        data.setName("policy_" + policyNum);
                    }
                    if (dao.addRule(data) == count) {
                        count++;
                    }
                    flag = false;
                    data = null;
                }
            }
        }
    }
}