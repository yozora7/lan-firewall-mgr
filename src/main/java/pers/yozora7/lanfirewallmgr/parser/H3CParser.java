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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static pers.yozora7.lanfirewallmgr.utils.NetUtils.longMaskToShort;
import static pers.yozora7.lanfirewallmgr.utils.NetUtils.wildcardToMask;

public class H3CParser {
    private String config;
    private Dao dao;

    public void parse(String config, Dao dao) throws IOException, ParserConfigurationException, SAXException {
        this.config = config;
        this.dao = dao;
        parseNetSet();
        parseServiceSet();
        parseRule();
    }

    // 从XML读取正则表达式
    private List<Map<String, String>> getRegex(String nodeName) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parse = factory.newSAXParser();
        XMLReader xmlReader = parse.getXMLReader();
        SAXParserHandler handler = new SAXParserHandler(nodeName);
        xmlReader.setContentHandler(handler);
        xmlReader.parse("src/main/resources/H3CRegex.xml");
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
        // 带长掩码
        Pattern subnet = Pattern.compile(regex.get("subnet"));
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
            // object-group ip address \S+
            if (header.matcher(line).find()) {
                String name = line.trim().replace("\"","").split("\\s+", 4)[3];
                setId = dao.addSet(name);
                flag = true;
                continue;
            }
            // 存入地址
            if (flag) {
                String[] temp = line.trim().split("\\s+");
                Net data = new Net();
                data.setSetId(setId);
                // \d+ network host address \d\S+
                if (host.matcher(line).find()) {
                    data.setStart(temp[4] + "/32");
                    data.setEnd(temp[4] + "/32");
                    // 添加地址
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
                // \d+ network range \d\S+ \d\S+$
                else if (range.matcher(line).find()) {
                    data.setStart(temp[3] + "/32");
                    data.setEnd(temp[4] + "/32");
                    // 添加地址
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
                // \d+ network subnet \S+ \d\S+$
                else if (subnet.matcher(line).find()) {
                    data.setStart(temp[3] + "/" + longMaskToShort(temp[4]));
                    data.setEnd(temp[3] + "/" + longMaskToShort(temp[4]));
                    // 添加地址
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
            }
            // 地址集记录结束
            if (line.trim().equals("#")) {
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

        Pattern header = Pattern.compile(regex.get("header"));
        Pattern eq = Pattern.compile(regex.get("eq"));
        Pattern range = Pattern.compile(regex.get("range"));
        Pattern bothEq = Pattern.compile(regex.get("both-eq"));
        Pattern bothRange = Pattern.compile(regex.get("both-range"));
        Pattern rangeEq = Pattern.compile(regex.get("range-eq"));
        Pattern eqRange = Pattern.compile(regex.get("eq-range"));

        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            } else {
                line = line.trim();
            }
            // object-group service \S+
            if (!flag && header.matcher(line).find()) {
                name = line.trim().replace("\"", "").split("\\s+", 3)[2];
                flag = true;
                continue;
            }
            if (flag) {
                // \d+ service \S+ destination \D+ \d+$
                if (eq.matcher(line).find()) {
                    Service data = new Service();
                    String temp = line.trim().split("\\s+")[4];
                    String protocol = line.trim().split("\\s+")[2];
                    data.setName(name);
                    data.setProtocol(protocol);
                    data.setSrcStartPort(0);
                    data.setSrcEndPort(65535);
                    if (temp.equals("eq")) {
                        data.setDstStartPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                        data.setDstEndPort(data.getDstStartPort());
                    } else if (temp.equals("lt")) {
                        data.setDstStartPort(0);
                        data.setDstEndPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                    } else if (temp.equals("gt")) {
                        data.setDstStartPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                        data.setDstEndPort(65535);
                    }
                    data.setId(count);
                    if (dao.addService(data) == count) {
                        count++;
                    }
                }
                // \d+ service \S+ destination range \d+ \d+$
                else if (range.matcher(line).find()) {
                    Service data = new Service();
                    String protocol = line.trim().split("\\s+")[2];
                    data.setName(name);
                    data.setProtocol(protocol);
                    data.setSrcStartPort(0);
                    data.setSrcEndPort(65535);
                    data.setDstStartPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                    data.setDstEndPort(Integer.valueOf(line.trim().split("\\s+")[6]));
                    data.setId(count);
                    if (dao.addService(data) == count) {
                        count++;
                    }
                }
                // \d+ service \S+ source \D+ \d+ destination \D+ \d+$
                else if (bothEq.matcher(line).find()) {
                    Service data = new Service();
                    String protocol = line.trim().split("\\s+")[2];
                    String temp1 = line.trim().split("\\s+")[4];
                    String temp2 = line.trim().split("\\s+")[7];
                    data.setName(name);
                    data.setProtocol(protocol);
                    if (temp1.equals("eq")) {
                        data.setSrcStartPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                        data.setSrcEndPort(data.getDstStartPort());
                    } else if (temp1.equals("lt")) {
                        data.setSrcStartPort(0);
                        data.setSrcEndPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                    } else if (temp1.equals("gt")) {
                        data.setSrcStartPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                        data.setSrcEndPort(65535);
                    }
                    if (temp2.equals("eq")) {
                        data.setDstStartPort(Integer.valueOf(line.trim().split("\\s+")[8]));
                        data.setDstEndPort(data.getDstStartPort());
                    } else if (temp2.equals("lt")) {
                        data.setDstStartPort(0);
                        data.setDstEndPort(Integer.valueOf(line.trim().split("\\s+")[8]));
                    } else if (temp2.equals("gt")) {
                        data.setDstStartPort(Integer.valueOf(line.trim().split("\\s+")[8]));
                        data.setDstEndPort(65535);
                    }
                    data.setId(count);
                    if (dao.addService(data) == count) {
                        count++;
                    }
                }
                // \d+ service \S+ source range \d+ \d+ destination range \d+ \d+$
                else if (bothRange.matcher(line).find()) {
                    Service data = new Service();
                    String protocol = line.trim().split("\\s+")[2];
                    data.setName(name);
                    data.setProtocol(protocol);
                    data.setSrcStartPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                    data.setSrcEndPort(Integer.valueOf(line.trim().split("\\s+")[6]));
                    data.setDstStartPort(Integer.valueOf(line.trim().split("\\s+")[9]));
                    data.setDstEndPort(Integer.valueOf(line.trim().split("\\s+")[10]));
                    data.setId(count);
                    if (dao.addService(data) == count) {
                        count++;
                    }
                }
                // \d+ service \S+ source range \d+ \d+ destination \D+ \d+$
                else if (rangeEq.matcher(line).find()) {
                    Service data = new Service();
                    String protocol = line.trim().split("\\s+")[2];
                    String temp = line.trim().split("\\s+")[8];
                    data.setName(name);
                    data.setProtocol(protocol);
                    data.setSrcStartPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                    data.setSrcEndPort(Integer.valueOf(line.trim().split("\\s+")[6]));
                    if (temp.equals("eq")) {
                        data.setDstStartPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                        data.setDstEndPort(data.getDstStartPort());
                    } else if (temp.equals("lt")) {
                        data.setDstStartPort(0);
                        data.setDstEndPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                    } else if (temp.equals("gt")) {
                        data.setDstStartPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                        data.setDstEndPort(65535);
                    }
                    data.setId(count);
                    if (dao.addService(data) == count) {
                        count++;
                    }
                }
                // \d+ service \S+ source \D+ \d+ destination range \d+ \d+$
                else if (eqRange.matcher(line).find()) {
                    Service data = new Service();
                    String protocol = line.trim().split("\\s+")[2];
                    String temp = line.trim().split("\\s+")[4];
                    data.setName(name);
                    data.setProtocol(protocol);
                    if (temp.equals("eq")) {
                        data.setSrcStartPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                        data.setSrcEndPort(data.getDstStartPort());
                    } else if (temp.equals("lt")) {
                        data.setSrcStartPort(0);
                        data.setSrcEndPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                    } else if (temp.equals("gt")) {
                        data.setSrcStartPort(Integer.valueOf(line.trim().split("\\s+")[5]));
                        data.setSrcEndPort(65535);
                    }
                    data.setDstStartPort(Integer.valueOf(line.trim().split("\\s+")[8]));
                    data.setDstEndPort(Integer.valueOf(line.trim().split("\\s+")[9]));
                    data.setId(count);
                    if (dao.addService(data) == count) {
                        count++;
                    }
                }
                // 记录结束
                else if (line.trim().equals("#")) {
                    flag = false;
                }
            }
        }
        reader.close();
    }

    // rule
    private void parseRule() throws IOException, ParserConfigurationException, SAXException {
        Rule data = null;
        Boolean flag = false;
        int count = dao.count("rule");
        int countService = dao.count("service");
        HashSet<Integer> srcSetIds = new HashSet<>();
        HashSet<Integer> dstSetIds = new HashSet<>();
        HashSet<Integer> srcNetIds = new HashSet<>();
        HashSet<Integer> dstNetIds = new HashSet<>();
        HashSet<Integer> serviceIds = new HashSet<>();
        Map<String, String> regex = getRegex("rule").get(0);
        // 起始字段
        Pattern header = Pattern.compile(regex.get("header"));
        // 行为
        Pattern action = Pattern.compile(regex.get("action"));
        // 源安全域
        Pattern srcZone = Pattern.compile(regex.get("src-zone"));
        // 源地址集
        Pattern srcSet = Pattern.compile(regex.get("src-set"));
        // 目标安全域
        Pattern dstZone = Pattern.compile(regex.get("dst-zone"));
        // 目标地址集
        Pattern dstSet = Pattern.compile(regex.get("dst-set"));
        // 服务名
        Pattern serviceName = Pattern.compile(regex.get("service-name"));
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            } else {
                line = line.trim();
            }
            // rule \d+ name \S+
            if (header.matcher(line).find()) {
                data = new Rule();
                data.setName(line.trim().replace("\"","").split("\\s+", 4)[3]);
                flag = true;
            }
            if (flag) {
                // action \S+
                if (action.matcher(line).find()) {
                    data.setAction(line.trim().replace("\"","").split("\\s+", 2)[1]);
                }
                // source-zone \S+
                else if (srcZone.matcher(line).find()) {
                    data.setSrcZone(line.trim().replace("\"","").split("\\s+", 2)[1]);
                }
                // source-ip \S+
                else if (srcSet.matcher(line).find()) {
                    srcSetIds.add(dao.addSet(line.trim().replace("\"","").split("\\s+", 2)[1]));
                }

                // destination-zone \S+
                else if (dstZone.matcher(line).find()) {
                    data.setDstZone(line.trim().replace("\"","").split("\\s+", 2)[1]);
                }
                // destination-ip \S+
                else if (dstSet.matcher(line).find()) {
                    dstSetIds.add(dao.addSet(line.trim().replace("\"","").split("\\s+", 2)[1]));
                }
                // service \S+
                else if (serviceName.matcher(line).find()) {
                    String name = line.trim().replace("\"","").split("\\s+", 2)[1];
                    Service service = new Service();
                    service.setName(name);
                    service.setId(countService);
                    int id = dao.addService(service);
                    serviceIds.add(id);
                    if (id == countService) {
                        countService++;
                    }
                    flag = false;
                    data.setSrcSetIds(srcSetIds);
                    data.setSrcNetIds(srcNetIds);
                    data.setDstSetIds(dstSetIds);
                    data.setDstNetIds(dstNetIds);
                    data.setServiceIds(serviceIds);
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