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

public class QMXCParser implements Parser {
    private String config;
    private Dao dao;
    public void parse(String config, Dao dao) throws IOException, ParserConfigurationException, SAXException {
        this.config = config;
        this.dao = dao;
        parseNetSet();
        parseServiceSet();
        parseRule();
    }
    private List<Map<String, String>> getRegex(String nodeName) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parse = factory.newSAXParser();
        XMLReader xmlReader = parse.getXMLReader();
        SAXParserHandler handler = new SAXParserHandler(nodeName);
        xmlReader.setContentHandler(handler);
        xmlReader.parse("src/main/resources/QMXCRegex.xml");
        return handler.getList();
    }
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
                line = line.trim().replaceAll("\\\\","/");
            }
            // ^address (".*?"|\S+)$
            if (header.matcher(line).find()) {
                setId = dao.addSet(line.split(split)[1].replace("\"",""));
                continue;
            }
            String[] temp = line.split("\\s+");
            // net-address \d+.\d+.\d+.\d+/\d+$
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
            // host-address \d+.\d+.\d+.\d+$
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
            // range-address \d+.\d+.\d+.\d+ \d+.\d+.\d+.\d+$
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
    private void parseServiceSet() throws IOException, ParserConfigurationException, SAXException {
        String name = null;
        int count = dao.count("service");
        Map<String, String> regex = getRegex("service").get(0);
        Pattern header = Pattern.compile(regex.get("header"));
        Pattern both = Pattern.compile(regex.get("both"));
        Pattern dst = Pattern.compile(regex.get("dst"));
        Pattern src = Pattern.compile(regex.get("src"));
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            } else {
                line = line.trim().replaceAll("\\\\","/");
            }
            // ^service (".*?"|\S+)$
            if (header.matcher(line).find()) {
                name = line.split(split)[1].replace("\"","");
                continue;
            }
            // \S+ dest \d+(\s+\d+)? source \d+(\s+\d+)?$
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
            // \S+ dest \d+(\s+\d+)?$
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
            // \S+ source \d+(\s+\d+)?$
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
                if (dao.addService(data) == count) {
                    count++;
                }
            }
        }
    }
    private void parseRule() throws IOException, ParserConfigurationException, SAXException {
        Rule data = null;
        boolean flag = false;
        String policyNum = "";
        int count = dao.count("rule");
        int countService = dao.count("service");
        HashSet<Integer> srcSetIds = null;
        HashSet<Integer> dstSetIds = null;
        HashSet<Integer> srcNetIds = null;
        HashSet<Integer> dstNetIds = null;
        HashSet<Integer> srcZoneIds = null;
        HashSet<Integer> dstZoneIds = null;
        HashSet<Integer> serviceIds = null;
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
                line = line.trim().replaceAll("\\\\","/");
            }
            // ^firewall policy \d+$
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
                // name (".*?"|\S+)$
                if (ruleName.matcher(line).find()) {
                    data.setName(line.split(split)[1].replace("\"",""));
                }
                // action (".*?"|\S+)
                else if (action.matcher(line).find()) {
                    data.setAction(line.replace("action","").trim());
                }
                // src-zone (".*?"|\S+)$
                else if (srcZone.matcher(line).find()) {
                    srcZoneIds.add(dao.addZone(line.split(split)[1].replace("\"","")));
                }
                // dst-zone (".*?"|\S+)$
                else if (dstZone.matcher(line).find()) {
                    dstZoneIds.add(dao.addZone(line.split(split)[1].replace("\"","")));
                }
                // src-addr (".*?"|\S+)$
                else if (srcSet.matcher(line).find()) {
                    srcSetIds.add(dao.addSet(line.split(split)[1].replace("\"","")));
                }
                // dst-addr (".*?"|\S+)$
                else if (dstSet.matcher(line).find()) {
                    dstSetIds.add(dao.addSet(line.split(split)[1].replace("\"","")));
                }
                // service (".*?"|\S+)$
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
                // app (".*?"|\S+)$
                else if (app.matcher(line).find()) {
                    data.setSrcSetIds(srcSetIds);
                    data.setSrcNetIds(srcNetIds);
                    data.setDstSetIds(dstSetIds);
                    data.setDstNetIds(dstNetIds);
                    data.setSrcZoneIds(srcZoneIds);
                    data.setDstZoneIds(dstZoneIds);
                    data.setServiceIds(serviceIds);
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