package pers.yozora7.lanfirewallmgr.parser;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import pers.yozora7.lanfirewallmgr.entity.Net;
import pers.yozora7.lanfirewallmgr.entity.Rule;
import pers.yozora7.lanfirewallmgr.entity.Service;
import pers.yozora7.lanfirewallmgr.mysql.Dao;
import pers.yozora7.lanfirewallmgr.xml.SAXParserHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import static pers.yozora7.lanfirewallmgr.utils.NetUtils.longMaskToShort;
import static pers.yozora7.lanfirewallmgr.utils.NetUtils.wildcardToMask;

public class HuaweiParser {
    private String config;
    private Dao dao;
    public void parse(String config, Dao dao) throws IOException, ParserConfigurationException, SAXException {
        this.config = config;
        this.dao = dao;
        parseNetSet();
        parseServiceSet();
        parseServiceGroup();
        parseRule();
    }

    // 从XML读取正则表达式
    private List<Map<String, String>> getRegex(String nodeName) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parse = factory.newSAXParser();
        XMLReader xmlReader = parse.getXMLReader();
        SAXParserHandler handler = new SAXParserHandler(nodeName);
        xmlReader.setContentHandler(handler);
        xmlReader.parse("src/main/resources/HuaweiRegex.xml");
        return handler.getList();
    }
    // address-set
    private void parseNetSet() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        int count = dao.count("net");
        int setId = 0;
        // 正则
        Map<String, String> regex = getRegex("address").get(0);
        // 起始字段
        Pattern header = Pattern.compile(regex.get("header"));
        // 单IP
        Pattern host = Pattern.compile(regex.get("host"));
        // 范围
        Pattern range = Pattern.compile(regex.get("range"));
        // 带掩码
        Pattern withMask = Pattern.compile(regex.get("mask"));
        // 带长掩码
        Pattern withLongMask = Pattern.compile(regex.get("long-mask"));
        // 带反掩码
        Pattern withWildcardMask = Pattern.compile(regex.get("wildcard"));
        // 读取配置文件
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            // 文件末尾
            if (line == null) {
                break;
            }
            // 开始读取文件
            else {
                line = line.trim();
            }
            // ip address-set ... type object
            if (header.matcher(line).find()) {
                setId = dao.addSet( line.trim().split("\\s+")[2]);
                flag = true;
                continue;
            }
            // 存入地址
            if (flag) {
                String[] temp = line.trim().split("\\s+");
                Net data = new Net();
                data.setSetId(setId);
                // address n x.x.x.x 0
                if (host.matcher(line).find()) {
                    data.setStart(temp[2] + "/32");
                    data.setEnd(temp[2] + "/32");
                    // 添加地址
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
                // address n range start end
                else if (range.matcher(line).find()) {
                    data.setStart(temp[3] + "/32");
                    data.setEnd(temp[4] + "/32");
                    // 添加地址
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
                // address n x.x.x.x mask x
                else if (withMask.matcher(line).find()) {
                    data.setStart(temp[2] + "/" + temp[4]);
                    data.setEnd(temp[2] + "/" + temp[4]);
                    // 添加地址
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
                // address n x.x.x.x mask x.x.x.x
                else if (withLongMask.matcher(line).find()) {
                    data.setStart(temp[2] + "/" + longMaskToShort(temp[4]));
                    data.setEnd(temp[2] + "/" + longMaskToShort(temp[4]));
                    // 添加地址
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
                // address n x.x.x.x x.x.x.x
                else if (withWildcardMask.matcher(line).find()) {
                    data.setStart(temp[2] + "/" + wildcardToMask(temp[3]));
                    data.setEnd(temp[2] + "/" + wildcardToMask(temp[3]));
                    // 添加地址
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
            }
            // 地址集记录结束
            if (line.contains("#")) {
                flag = false;
            }
        }
        reader.close();
    }

    // service-set
    private void parseServiceSet() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        String name = null;
        int count = dao.count("service");
        Map<String, String> regex = getRegex("service-set").get(0);
        // 起始字段
        Pattern header = Pattern.compile(regex.get("header"));
        // 源端口和目标端口
        Pattern content = Pattern.compile(regex.get("content"));
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            else {
                line = line.trim();
            }
            // ip service-set ... type object
            if (!flag && header.matcher(line).find()) {
                name = line.trim().split("\\s+")[2];
                flag = true;
                continue;
            }
            // service n protocol ... source-port n1 [to n2] destination-port n3 [to n4]
            if (flag && content.matcher(line).find()) {
                Service data = new Service();
                String protocol = line.trim().split("\\s+")[3];
                data.setName(name);
                data.setProtocol(protocol);
                // 起始/结束端口
                String[] srcPorts = line.trim()
                        .split("source-port|destination-port")[1]
                        .replaceAll("\\s+", "")
                        .split("to");
                String[] dstPorts = line.trim()
                        .split("source-port|destination-port")[2]
                        .replaceAll("\\s+", "")
                        .split("to");
                data.setSrcStartPort(Integer.valueOf(srcPorts[0]));
                data.setDstStartPort(Integer.valueOf(dstPorts[0]));
                if (srcPorts.length > 1) {
                    data.setSrcEndPort(Integer.valueOf(srcPorts[1]));
                }
                else {
                    data.setSrcEndPort(Integer.valueOf(srcPorts[0]));
                }
                if (dstPorts.length > 1) {
                    data.setDstEndPort(Integer.valueOf(dstPorts[1]));
                }
                else {
                    data.setDstEndPort(Integer.valueOf(dstPorts[0]));
                }
                data.setId(count);
                if (dao.addService(data) == count) {
                    count++;
                }
            }
            // 记录结束
            if (line.contains("#")) {
                flag = false;
            }
        }
        reader.close();
    }

    private void parseServiceGroup() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        String group = null;
        Map<String, String> regex = getRegex("service-group").get(0);
        // 起始字段
        Pattern header = Pattern.compile(regex.get("header"));
        // service-set
        Pattern set = Pattern.compile(regex.get("set"));
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            else {
                line = line.trim();
            }
            // ip service-set \S+ type group
            if (!flag && header.matcher(line).find()) {
                group = line.trim().split("\\s+")[2];
                flag = true;
                continue;
            }
            // service n service-set ...
            if (flag && set.matcher(line).find()) {
                String service = line.trim().split("\\s+")[3];
                dao.addGroup(service, group);
            }
            // 记录结束
            if (line.contains("#")) {
                flag = false;
            }
        }
        reader.close();
    }

    private void parseRule() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        Rule data = null;
        Net net;
        int count = dao.count("rule");
        int countNet = dao.count("net");
        int countService = dao.count("service");
        HashSet<Integer> srcSetIds = new HashSet<>();
        HashSet<Integer> dstSetIds = new HashSet<>();
        HashSet<Integer> srcNetIds = new HashSet<>();
        HashSet<Integer> dstNetIds = new HashSet<>();
        HashSet<Integer> serviceIds = new HashSet<>();
        HashSet<String> serviceGroups = new HashSet<>();
        Map<String, String> regex = getRegex("rule").get(0);
        // 起始字段
        Pattern header = Pattern.compile(regex.get("header"));
        // 源安全域
        Pattern srcZone = Pattern.compile(regex.get("src-zone"));
        // 源地址集
        Pattern srcSet = Pattern.compile(regex.get("src-set"));
        // 源地址 (带掩码)
        Pattern srcMask = Pattern.compile(regex.get("src-mask"));
        // 源地址 (范围)
        Pattern srcRange = Pattern.compile(regex.get("src-range"));
        // 目标安全域
        Pattern dstZone = Pattern.compile(regex.get("dst-zone"));
        // 目标地址集
        Pattern dstSet = Pattern.compile(regex.get("dst-set"));
        // 目标地址 (带掩码)
        Pattern dstMask = Pattern.compile(regex.get("dst-mask"));
        // 目标地址 (范围)
        Pattern dstRange = Pattern.compile(regex.get("dst-range"));
        // 服务名
        Pattern serviceName = Pattern.compile(regex.get("service-name"));
        // 服务
        Pattern serviceContent = Pattern.compile(regex.get("service-content"));
        // 应用 (视为服务)
        Pattern app = Pattern.compile(regex.get("app"));
        // 行为
        Pattern action = Pattern.compile(regex.get("action"));
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            else {
                line = line.trim();
            }
            // 规则开始
            if (header.matcher(line).find()) {
                data = new Rule();
                data.setName(line.trim().split("\\s+")[2]);
                flag = true;
            }
            if (flag) {
                // source-zone
                if (srcZone.matcher(line).find()) {
                    data.setSrcZone(line.trim().split("\\s+")[1]);
                }
                // source-address address-set
                else if (srcSet.matcher(line).find()) {
                    srcSetIds.add(dao.addSet(line.trim().split("\\s+")[2]));
                }
                // source-address x.x.x.x mask x.x.x.x
                else if (srcMask.matcher(line).find()) {
                    net = new Net();
                    net.setStart(line.trim().split("\\s+")[1] + "/" + longMaskToShort(line.trim().split("\\s+")[3]));
                    net.setEnd(net.getStart());
                    net.setSetId(0);
                    net.setId(countNet);
                    int id = dao.addNet(net);
                    srcNetIds.add(id);
                    if (id == countNet) {
                        countNet++;
                    }
                }
                // source-address range
                else if (srcRange.matcher(line).find()) {
                    net = new Net();
                    net.setStart(line.trim().split("\\s+")[2] + "/32");
                    net.setEnd(line.trim().split("\\s+")[3] + "/32");
                    net.setSetId(0);
                    net.setId(countNet);
                    int id = dao.addNet(net);
                    srcNetIds.add(id);
                    if (id == countNet) {
                        countNet++;
                    }
                }
                // destination-zone
                else if (dstZone.matcher(line).find()) {
                    data.setDstZone(line.trim().split("\\s+")[1]);
                }
                // destination-address address-set
                else if (dstSet.matcher(line).find()) {
                    dstSetIds.add(dao.addSet(line.trim().split("\\s+")[2]));
                }
                // destination-address x.x.x.x mask x.x.x.x
                else if (dstMask.matcher(line).find()) {
                    net = new Net();
                    net.setStart(line.trim().split("\\s+")[1] + "/" + longMaskToShort(line.trim().split("\\s+")[3]));
                    net.setEnd(net.getStart());
                    net.setSetId(0);
                    net.setId(countNet);
                    int id = dao.addNet(net);
                    dstNetIds.add(id);
                    if (id == countNet) {
                        countNet++;
                    }
                }
                // destination-address range
                else if (dstRange.matcher(line).find()) {
                    net = new Net();
                    net.setStart(line.trim().split("\\s+")[2] + "/32");
                    net.setEnd(line.trim().split("\\s+")[3] + "/32");
                    net.setSetId(0);
                    net.setId(countNet);
                    int id = dao.addNet(net);
                    dstNetIds.add(id);
                    if (id == countNet) {
                        countNet++;
                    }
                }
                // service
                else if (serviceName.matcher(line).find()) {
                    String name = line.trim().split("\\s+")[1];
                    // service-group
                    if (dao.isServiceGroup(name)) {
                        serviceGroups.add(name);
                    }
                    // service-set
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
                // application (as service)
                else if (app.matcher(line).find()) {
                    Service service = new Service();
                    service.setName(line.trim().split("\\s+")[1]);
                    service.setId(countService);
                    int id = dao.addService(service);
                    serviceIds.add(id);
                    if (id == countService) {
                        countService++;
                    }
                }
                // service protocol ... destination-port ...
                else if (serviceContent.matcher(line).find()) {
                    Service service = new Service();
                    String protocol = line.trim().split("\\s+")[2];
                    service.setProtocol(protocol);
                    // 目标端口
                    String[] dstPorts = line.trim()
                            .split("destination-port")[1]
                            .replaceAll("\\s+", "")
                            .split("to");
                    service.setDstStartPort(Integer.valueOf(dstPorts[0]));
                    if (dstPorts.length > 1) {
                        service.setDstEndPort(Integer.valueOf(dstPorts[1]));
                        // 服务命名: 协议_目标端口
                        service.setName(protocol + "_" + dstPorts[0] + "_" + dstPorts[1]);
                    }
                    else {
                        service.setDstEndPort(Integer.valueOf(dstPorts[0]));
                        service.setName(protocol + "_" + dstPorts[0]);
                    }
                    service.setId(countService);
                    int id = dao.addService(service);
                    serviceIds.add(id);
                    if (id == countService) {
                        countService++;
                    }
                }
                // action
                if (action.matcher(line).find() && flag) {
                    flag = false;
                    data.setSrcSetIds(srcSetIds);
                    data.setSrcNetIds(srcNetIds);
                    data.setDstSetIds(dstSetIds);
                    data.setDstNetIds(dstNetIds);
                    data.setServiceIds(serviceIds);
                    data.setServiceGroups(serviceGroups);
                    data.setAction(line.replace("action", "").trim());
                    // 记录规则
                    data.setId(count);
                    if (dao.addRule(data) == count) {
                        count++;
                    }
                }
            }
        }
    }
}