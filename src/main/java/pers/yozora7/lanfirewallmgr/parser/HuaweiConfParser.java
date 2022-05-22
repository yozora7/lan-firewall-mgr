package pers.yozora7.lanfirewallmgr.parser;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import pers.yozora7.lanfirewallmgr.parser.data.Address;
import pers.yozora7.lanfirewallmgr.parser.data.Rule;
import pers.yozora7.lanfirewallmgr.parser.data.Service;
import pers.yozora7.lanfirewallmgr.service.ConfDaoService;
import pers.yozora7.lanfirewallmgr.xml.SAXParserHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import static pers.yozora7.lanfirewallmgr.parser.NetUtils.longMask2Short;
import static pers.yozora7.lanfirewallmgr.parser.NetUtils.wildcard2Mask;

public class HuaweiConfParser extends FirewallConfParser {
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
    // 地址集 (address-set)
    @Override
    public void parseAddressSet(String config, ConfDaoService dao) throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        String set = "";

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
            // 地址集记录开始, 名称强制小写 (ip address-set ... type object)
            if (header.matcher(line).find()) {
                set = line.trim().split("\\s+")[2].toLowerCase();
                flag = true;
                continue;
            }
            // 存入地址
            if (flag) {
                String[] temp = line.trim().split("\\s+");
                Address data = new Address();
                data.setSet(set);
                // 单IP (address n x.x.x.x 0)
                if (host.matcher(line).find()) {
                    data.setStart(temp[2] + "/32");
                    data.setEnd("null");
                }
                // IP范围 (address n range start end)
                else if (range.matcher(line).find()) {
                    data.setStart(temp[3] + "/32");
                    data.setEnd(temp[4] + "/32");
                }
                // 带2位数掩码 (CIDR) (address n x.x.x.x mask x)
                else if (withMask.matcher(line).find()) {
                    data.setStart(temp[2] + "/" + temp[4]);
                    data.setEnd("null");
                }
                // 带长掩码 (address n x.x.x.x mask x.x.x.x)
                else if (withLongMask.matcher(line).find()) {
                    data.setStart(temp[2] + "/" + longMask2Short(temp[4]));
                    data.setEnd("null");
                }
                // 带反掩码 (address n x.x.x.x x.x.x.x)
                else if (withWildcardMask.matcher(line).find()) {
                    data.setStart(temp[2] + "/" + wildcard2Mask(temp[3]));
                    data.setEnd("null");
                }
                // 添加地址
                dao.address(data);
            }
            // 地址集记录结束
            if (line.contains("#")) {
                flag = false;
            }
        }
        reader.close();
    }

    @Override
    public void parseServiceSet(String config, ConfDaoService dao) throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        String name = null;

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
            // 读取服务集名称, 强制小写 (ip service-set ... type object)
            if (!flag && header.matcher(line).find()) {
                name = line.trim().split("\\s+")[2].toLowerCase();
                flag = true;
                continue;
            }
            // service n protocol ... source-port n1 [to n2] destination-port n3 [to n4]
            if (flag && content.matcher(line).find()) {
                Service data = new Service();
                String protocol = line.trim().split("\\s+")[3].toLowerCase();
                data.setName(name);
                data.setProtocol(protocol);
                // 起始/结束端口
                String[] temp = line.trim()
                        .split("source-port|destination-port")[1]
                        .replaceAll("\\s+", "")
                        .split("to");
                data.setSrcStartPort(Integer.valueOf(temp[0]));
                data.setSrcStartPort(Integer.valueOf(temp[1]));
                data.setDstStartPort(Integer.valueOf(temp[0]));
                data.setDstEndPort(Integer.valueOf(temp[1]));
                data.setGroup("null");
                dao.service(data);
            }
            // 记录结束
            if (line.contains("#")) {
                flag = false;
            }
        }
        reader.close();
    }

    @Override
    public void parseServiceGroup(String config, ConfDaoService dao) throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        String group = null;

        Map<String, String> regex = getRegex("service-set").get(0);
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
                group = line.trim().split("\\s+")[2].toLowerCase();
                flag = true;
                continue;
            }
            // service n service-set ...
            if (flag && set.matcher(line).find()) {
                String service = line.trim().split("\\s+")[3].toLowerCase();
                dao.serviceGroup(service, group);
            }
            // 记录结束
            if (line.contains("#")) {
                flag = false;
            }
        }
        reader.close();
    }

    @Override
    public void parseRule(String config, ConfDaoService dao) throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        Rule data = null;
        Address address = null;
        Set<String> srcSets = new HashSet<>();
        Set<String> dstSets = new HashSet<>();
        Set<Integer> srcAddressIds = new HashSet<>();
        Set<Integer> dstAddressIds = new HashSet<>();
        Set<Integer> serviceIds = new HashSet<>();
        Set<String> serviceGroups = new HashSet<>();

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
                data.setName(line.trim().split("\\s+")[2].toLowerCase());
                flag = true;

            }
            if (flag) {
                // source-zone
                if (srcZone.matcher(line).find()) {
                    data.setSrcZone(line.trim().split("\\s+")[1].toLowerCase());
                }
                // source-address address-set
                else if (srcSet.matcher(line).find()) {
                    srcSets.add(line.trim().split("\\s+")[2].toLowerCase());
                }
                // source-address x.x.x.x mask x.x.x.x
                else if (srcMask.matcher(line).find()) {
                    address = new Address();
                    address.setStart(line.trim().split("\\s+")[1] + "/" + longMask2Short(line.trim().split("\\s+")[3]));
                    address.setEnd("null");
                    address.setSet("null");
                    srcAddressIds.add(dao.address(address));
                }
                // source-address range
                else if (srcRange.matcher(line).find()) {
                    address = new Address();
                    address.setStart(line.trim().split("\\s+")[2] + "/32");
                    address.setEnd(line.trim().split("\\s+")[3] + "/32");
                    address.setSet("null");
                    srcAddressIds.add(dao.address(address));
                }
                // destination-zone
                else if (dstZone.matcher(line).find()) {
                    data.setDstZone(line.trim().split("\\s+")[1].toLowerCase());
                }
                // destination-address address-set
                else if (dstSet.matcher(line).find()) {
                    dstSets.add(line.trim().split("\\s+")[2].toLowerCase());
                }
                // destination-address x.x.x.x mask x.x.x.x
                else if (dstMask.matcher(line).find()) {
                    address = new Address();
                    address.setStart(line.trim().split("\\s+")[1] + "/" + longMask2Short(line.trim().split("\\s+")[3]));
                    address.setEnd("null");
                    address.setSet("null");
                    dstAddressIds.add(dao.address(address));
                }
                // destination-address range
                else if (dstRange.matcher(line).find()) {
                    address = new Address();
                    address.setStart(line.trim().split("\\s+")[2] + "/32");
                    address.setEnd(line.trim().split("\\s+")[3] + "/32");
                    address.setSet("null");
                    dstAddressIds.add(dao.address(address));
                }
                // service
                else if (serviceName.matcher(line).find()) {
                    String name = line.trim().split("\\s+")[1].toLowerCase();
                    // service-group
                    if (dao.isServiceGroup(name)) {
                        serviceGroups.add(name);
                    }
                    // service-set
                    else {
                        Service temp = new Service();
                        temp.setName(name);
                        serviceIds.add(dao.service(temp));
                    }
                }
                // application (as service)
                else if (app.matcher(line).find()) {
                    Service temp = new Service();
                    temp.setName(line.trim().split("\\s+")[1].toLowerCase());
                    serviceIds.add(dao.service(temp));
                }
                // service protocol ... destination-port ...
                else if (serviceContent.matcher(line).find()) {
                    Service temp = new Service();
                    String protocol = line.trim().split("\\s+")[2].toLowerCase();
                    temp.setProtocol(protocol);
                    // 目标端口
                    String[] dst = line.trim()
                            .split("destination-port")[1]
                            .replaceAll("\\s+", "")
                            .split("to");
                    temp.setDstStartPort(Integer.valueOf(dst[0]));
                    temp.setDstEndPort(Integer.valueOf(dst[1]));
                    // 服务命名: 协议_目标端口
                    temp.setName(protocol + "_" + dst[0] + "_" + dst[1]);
                    serviceIds.add(dao.service(temp));
                }
                // action
                if (action.matcher(line).find() && flag) {
                    flag = false;
                    data.setSrcSets(srcSets);
                    data.setSrcAddressIds(srcAddressIds);
                    data.setDstSets(dstSets);
                    data.setDstAddressIds(dstAddressIds);
                    data.setServiceIds(serviceIds);
                    data.setServiceGroups(serviceGroups);
                    data.setAction(line.trim().split("\\s+")[1].toLowerCase());
                    if (data.getSrcZone() == null) {
                        data.setSrcZone("null");
                    }
                    if (data.getDstZone() == null) {
                        data.setDstZone("null");
                    }
                    // 记录规则
                    dao.rule(data);
                }
            }
        }
    }
}