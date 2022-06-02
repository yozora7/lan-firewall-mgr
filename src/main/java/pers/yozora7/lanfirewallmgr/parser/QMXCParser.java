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

public class QMXCParser {
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
        xmlReader.parse("src/main/resources/QMXCRegex.xml");
        return handler.getList();
    }

    // address-set
    private void parseNetSet() throws IOException, ParserConfigurationException, SAXException {
        int count = dao.count("net");
        int setId = 0;
        Map<String, String> regex = getRegex("address").get(0);
        Pattern header = Pattern.compile(regex.get("header"));
        Pattern net = Pattern.compile(regex.get("net"));
        Pattern host = Pattern.compile(regex.get("host"));
        Pattern range = Pattern.compile(regex.get("range"));

        BufferedReader reader = new BufferedReader(new FileReader(config));

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            else {
                line = line.trim();
            }
            if (header.matcher(line).find()) {
                setId = dao.addSet(line.trim().split("\\s+")[1]);
            }
            String[] temp = line.trim().split("\\s+");
            // net-address
            if (net.matcher(line).find()) {
                Net data = new Net();
                data.setSetId(setId);
                data.setStart(temp[1]);
                data.setEnd(temp[1]);
                data.setId(count);
                if (dao.addNet(data) == count) {
                    count++;
                }
            }
            // host-address
            else if (host.matcher(line).find()) {
                Net data = new Net();
                data.setSetId(setId);
                data.setStart(temp[1] + "/32");
                data.setEnd(temp[1] + "/32");
                data.setId(count);
                if (dao.addNet(data) == count) {
                    count++;
                }
            }
            // range-address
            else if (range.matcher(line).find()) {
                Net data = new Net();
                data.setSetId(setId);
                data.setStart(temp[1] + "/32");
                data.setEnd(temp[2] + "/32");
                data.setId(count);
                if (dao.addNet(data) == count) {
                    count++;
                }
            }
        }
        reader.close();
    }

    // service-set
    private void parseServiceSet() throws IOException, ParserConfigurationException, SAXException {
        String name = null;
        int count = dao.count("service");

        Map<String, String> regex = getRegex("service").get(0);
        Pattern header = Pattern.compile(regex.get("header"));
        Pattern both = Pattern.compile(regex.get("both"));
        Pattern dest = Pattern.compile(regex.get("dst"));
        Pattern source = Pattern.compile(regex.get("src"));

        BufferedReader reader = new BufferedReader(new FileReader(config));

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            } else {
                line = line.trim();
            }
            if (header.matcher(line).find()) {
                name = line.trim().split("\\s+")[1];
                continue;
            }
            if (both.matcher(line).find()) {
                Service data = new Service();
                data.setName(name);
                String protocol = line.trim().split("\\s+")[0];
                data.setProtocol(protocol);
                String[] srcPorts = line.trim()
                        .split("dest|source")[2]
                        .trim()
                        .split("\\s+");
                String[] dstPorts = line.trim()
                        .split("dest|source")[1]
                        .trim()
                        .split("\\s+");
                data.setSrcStartPort(Integer.valueOf(srcPorts[0]));
                data.setDstStartPort(Integer.valueOf(dstPorts[0]));
                if (srcPorts.length > 1) {
                    data.setSrcEndPort(Integer.valueOf(srcPorts[1]));
                } else {
                    data.setSrcEndPort(Integer.valueOf(srcPorts[0]));
                }
                if (dstPorts.length > 1) {
                    data.setDstEndPort(Integer.valueOf(dstPorts[1]));
                } else {
                    data.setDstEndPort(Integer.valueOf(dstPorts[0]));
                }
                data.setId(count);
                if (dao.addService(data) == count) {
                    count++;
                }
            } else if (dest.matcher(line).find()) {
                Service data = new Service();
                data.setName(name);
                String protocol = line.trim().split("\\s+")[0];
                data.setProtocol(protocol);
                data.setSrcStartPort(0);
                data.setSrcEndPort(0);
                data.setDstStartPort(Integer.valueOf(line.trim().split("\\s+")[2]));
                if (line.trim().split("\\s+").length > 3) {
                    data.setDstEndPort(Integer.valueOf(line.trim().split("\\s+")[3]));
                } else {
                    data.setDstEndPort(data.getDstStartPort());
                }
                data.setId(count);
                if (dao.addService(data) == count) {
                    count++;
                }
            } else if (source.matcher(line).find()) {
                Service data = new Service();
                data.setName(name);
                String protocol = line.trim().split("\\s+")[0];
                data.setProtocol(protocol);
                data.setDstStartPort(0);
                data.setDstEndPort(0);
                data.setSrcStartPort(Integer.valueOf(line.trim().split("\\s+")[2]));
                if (line.trim().split("\\s+").length > 3) {
                    data.setSrcEndPort(Integer.valueOf(line.trim().split("\\s+")[3]));
                } else {
                    data.setSrcEndPort(data.getSrcStartPort());
                }
                data.setId(count);
                if (dao.addService(data) == count) {
                    count++;
                }
            }
        }
    }

    private void parseRule() throws IOException, ParserConfigurationException, SAXException {
        // TODO
        Rule data = null;
        Net net;
        boolean flag = false;
        String policyNum = "";
        int count = dao.count("rule");
        int countService = dao.count("service");
        HashSet<Integer> srcSetIds = new HashSet<>();
        HashSet<Integer> dstSetIds = new HashSet<>();
        HashSet<Integer> srcNetIds = new HashSet<>();
        HashSet<Integer> dstNetIds = new HashSet<>();
        HashSet<Integer> serviceIds = new HashSet<>();
        HashSet<String> serviceGroups = new HashSet<>();

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

        BufferedReader reader = new BufferedReader(new FileReader(config));

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            else {
                line = line.trim();
            }
            if (header.matcher(line).find()) {
                flag = true;
                data = new Rule();
                policyNum = line.split("\\s+")[2];
            }
            if (flag) {
                if (ruleName.matcher(line).find()) {
                    data.setName(line.trim().split("\\s+")[1]);
                } else if (action.matcher(line).find()) {
                    data.setAction(line.trim().split("\\s+")[1]);
                } else if (srcZone.matcher(line).find()) {
                    data.setSrcZone(line.trim().split("\\s+")[1]);
                } else if (dstZone.matcher(line).find()) {
                    data.setDstZone(line.trim().split("\\s+")[1]);
                } else if (srcSet.matcher(line).find()) {
                    srcSetIds.add(dao.addSet(line.trim().split("\\s+")[1]));
                } else if (dstSet.matcher(line).find()) {
                    dstSetIds.add(dao.addSet(line.trim().split("\\s+")[1]));
                } else if (serviceName.matcher(line).find()) {
                    String name = line.trim().split("\\s+")[1];
                    Service service = new Service();
                    service.setName(name);
                    service.setId(countService);
                    int id = dao.addService(service);
                    serviceIds.add(id);
                    if (id == countService) {
                        countService++;
                    }
                } else if (app.matcher(line).find()) {
                    String name = line.trim().split("\\s+")[1];
                    Service service = new Service();
                    service.setName(name);
                    service.setId(countService);
                    int id = dao.addService(service);
                    serviceIds.add(id);
                    if (id == countService) {
                        countService++;
                    }
                    data.setSrcSetIds(srcSetIds);
                    data.setSrcNetIds(srcNetIds);
                    data.setDstSetIds(dstSetIds);
                    data.setDstNetIds(dstNetIds);
                    data.setServiceIds(serviceIds);
                    data.setServiceGroups(serviceGroups);
                    data.setId(count);
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