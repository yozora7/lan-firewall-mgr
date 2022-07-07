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

import static pers.yozora7.lanfirewallmgr.utils.Utils.longMaskToShort;
import static pers.yozora7.lanfirewallmgr.utils.Utils.wildcardToMask;

public class HuaweiParser implements Parser {
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
    // ip address-set
    private void parseNetSet() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        int count = dao.count("net");
        int setId = 0;
        Map<String, String> regex = getRegex("address").get(0);
        Pattern header = Pattern.compile(regex.get("header"));
        Pattern host = Pattern.compile(regex.get("host"));
        Pattern range = Pattern.compile(regex.get("range"));
        Pattern mask = Pattern.compile(regex.get("mask"));
        Pattern longMask = Pattern.compile(regex.get("long-mask"));
        Pattern wildcard = Pattern.compile(regex.get("wildcard"));
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            else {
                line = line.trim().replaceAll("\\\\","/");
            }
            // ip address-set (".*?"|\S+) type
            if (header.matcher(line).find()) {
                setId = dao.addSet(line.split(split)[2].replace("\"",""));
                flag = true;
                continue;
            }
            if (flag) {
                String[] temp = line.split("\\s+");
                Net data = new Net();
                data.setSetId(setId);
                // address \d+ \d\S+ 0$
                if (host.matcher(line).find()) {
                    data.setStart(temp[2]);
                    data.setStartMask(32);
                    data.setEnd(temp[2]);
                    data.setEndMask(32);
                    data.setId(count);
                    if (dao.addNet(data) == count) {
                        count++;
                    }
                }
                // address \d+ range \d\S+ \d\S+$
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
                // address \d+ \d\S+ mask \d+$
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
                // address \d+ \d\S+ mask \d+.\S+$
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
                // address \d+ \d\S+ \d\S+$
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
            if (line.equals("#")) {
                flag = false;
            }
        }
        reader.close();
    }
    // ip service-set type object
    private void parseServiceSet() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        String name = null;
        int count = dao.count("service");
        Map<String, String> regex = getRegex("service-set").get(0);
        Pattern header = Pattern.compile(regex.get("header"));
        Pattern content = Pattern.compile(regex.get("content"));
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            else {
                line = line.trim().replaceAll("\\\\","/");
            }
            // ip service-set (".*?"|\S+) type object \d+$
            if (header.matcher(line).find()) {
                name = line.split(split)[2].replace("\"","");
                flag = true;
                continue;
            }
            if (flag) {
                // service \d+ protocol \S+ source-port \d+
                if (content.matcher(line).find()) {
                    Service data = new Service();
                    data.setName(name);
                    data.setProtocol(line.split("\\s+")[3]);
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
    // ip service-set type group
    private void parseServiceGroup() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        String group = null;
        Map<String, String> regex = getRegex("service-group").get(0);
        Pattern header = Pattern.compile(regex.get("header"));
        Pattern set = Pattern.compile(regex.get("set"));
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            else {
                line = line.trim().replaceAll("\\\\","/");
            }
            // ip service-set (".*?"|\S+) type group \d+$
            if (header.matcher(line).find()) {
                group = line.split(split)[2].replace("\"","");
                flag = true;
                continue;
            }
            // service \d+ service-set (".*?"|\S+)$
            if (flag) {
                if (set.matcher(line).find()) {
                    String service = line.split(split)[3].replace("\"","");
                    dao.addGroup(service, group);
                }
                else if (line.equals("#")) {
                    flag = false;
                }
            }
        }
        reader.close();
    }
    // rule
    private void parseRule() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        Rule data = null;
        Net net;
        int count = dao.count("rule");
        int countNet = dao.count("net");
        int countService = dao.count("service");
        HashSet<Integer> srcSetIds = null;
        HashSet<Integer> dstSetIds = null;
        HashSet<Integer> srcZoneIds = null;
        HashSet<Integer> dstZoneIds = null;
        HashSet<Integer> srcNetIds = null;
        HashSet<Integer> dstNetIds = null;
        HashSet<Integer> serviceIds = null;
        HashSet<String> serviceGroups = null;
        Map<String, String> regex = getRegex("rule").get(0);
        Pattern header = Pattern.compile(regex.get("header"));
        Pattern srcZone = Pattern.compile(regex.get("src-zone"));
        Pattern srcSet = Pattern.compile(regex.get("src-set"));
        Pattern srcMask = Pattern.compile(regex.get("src-mask"));
        Pattern srcRange = Pattern.compile(regex.get("src-range"));
        Pattern dstZone = Pattern.compile(regex.get("dst-zone"));
        Pattern dstSet = Pattern.compile(regex.get("dst-set"));
        Pattern dstMask = Pattern.compile(regex.get("dst-mask"));
        Pattern dstRange = Pattern.compile(regex.get("dst-range"));
        Pattern serviceName = Pattern.compile(regex.get("service-name"));
        Pattern app = Pattern.compile(regex.get("app"));
        Pattern serviceContent = Pattern.compile(regex.get("service-content"));
        Pattern action = Pattern.compile(regex.get("action"));
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            else {
                line = line.trim().replaceAll("\\\\","/");
            }
            // rule name (".*?"|\S+)
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
            if (flag) {
                // source-zone (".*?"|\S+)
                if (srcZone.matcher(line).find()) {
                    srcZoneIds.add(dao.addZone(line.split(split)[1].replace("\"","")));
                }
                // source-address address-set (".*?"|\S+)$
                else if (srcSet.matcher(line).find()) {
                    srcSetIds.add(dao.addSet(line.split(split)[2].replace("\"","")));
                }
                // source-address \d\S+ mask \d\S+$
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
                // source-address range \d\S+ \d\S+$
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
                // destination-zone (".*?"|\S+)$
                else if (dstZone.matcher(line).find()) {
                    dstZoneIds.add(dao.addZone(line.split(split)[1].replace("\"","")));
                }
                // destination-address address-set (".*?"|\S+)$
                else if (dstSet.matcher(line).find()) {
                    dstSetIds.add(dao.addSet(line.split(split)[2].replace("\"","")));
                }
                // destination-address \d\S+ mask \d\S+$
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
                // destination-address \d\S+ range \d\S+$
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
                // service (".*?"|\S+)$
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
                // application app (".*?"|\S+)
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
                // service protocol \S+ destination-port \d+
                else if (serviceContent.matcher(line).find()) {
                    Service service = new Service();
                    String protocol = line.split("\\s+")[2];
                    service.setProtocol(protocol);
                    String[] dstPorts = line.split("destination-port")[1].replaceAll("\\s+","").split("to");
                    service.setDstStartPort(Integer.parseInt(dstPorts[0]));
                    if (dstPorts.length > 1) {
                        service.setDstEndPort(Integer.parseInt(dstPorts[1]));
                        // 服务命名 协议_目标端口
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
                // action (".*?"|\S+)
                else if (action.matcher(line).find()) {
                    flag = false;
                    data.setSrcSetIds(srcSetIds);
                    data.setSrcNetIds(srcNetIds);
                    data.setSrcZoneIds(srcZoneIds);
                    data.setDstSetIds(dstSetIds);
                    data.setDstNetIds(dstNetIds);
                    data.setDstZoneIds(dstZoneIds);
                    data.setServiceIds(serviceIds);
                    data.setServiceGroups(serviceGroups);
                    data.setAction(line.replace("action","").trim());
                    data.setId(count);
                    if (dao.addRule(data) == count) {
                        count++;
                    }
                }
            }
        }
    }
}