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

public class H3CParser implements Parser {
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
        xmlReader.parse("src/main/resources/H3CRegex.xml");
        return handler.getList();
    }
    private void parseNetSet() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        int count = dao.count("net");
        int setId = 0;
        Map<String, String> regex = getRegex("address").get(0);
        Pattern header = Pattern.compile(regex.get("header"));
        Pattern host = Pattern.compile(regex.get("host"));
        Pattern range = Pattern.compile(regex.get("range"));
        Pattern subnet = Pattern.compile(regex.get("subnet"));
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            } else {
                line = line.trim().replaceAll("\\\\","/");
            }
            // object-group ip address (".*?"|\S+)
            if (header.matcher(line).find()) {
                String name = line.split(split)[3].replace("\"","");
                setId = dao.addSet(name);
                flag = true;
                continue;
            }
            if (flag) {
                String[] temp = line.split("\\s+");
                Net data = new Net();
                data.setSetId(setId);
                // \d+ network host address \d\S+
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
                // \d+ network range \d\S+ \d\S+$
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
                // \d+ network subnet \S+ \d\S+$
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
            if (line.equals("#")) {
                flag = false;
            }
        }
        reader.close();
    }
    private void parseServiceSet() throws IOException, ParserConfigurationException, SAXException {
        boolean flag = false;
        String name = null;
        int count = dao.count("service");
        Map<String, String> regex = getRegex("service").get(0);
        Pattern header = Pattern.compile(regex.get("header"));
        Pattern eq = Pattern.compile(regex.get("eq"));
        Pattern range = Pattern.compile(regex.get("range"));
        Pattern bothEq = Pattern.compile(regex.get("both-eq"));
        Pattern bothRange = Pattern.compile(regex.get("both-range"));
        Pattern eqRange = Pattern.compile(regex.get("eq-range"));
        Pattern rangeEq = Pattern.compile(regex.get("range-eq"));
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            } else {
                line = line.trim().replaceAll("\\\\","/");
            }
            // object-group service (".*?"|\S+)
            if (header.matcher(line).find()) {
                name = line.split(split)[2].replace("\"","");
                flag = true;
                continue;
            }
            if (flag) {
                Service data = new Service();
                data.setName(name);
                // \d+ service \S+ destination \D+ \d+$
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
                // \d+ service \S+ destination range \d+ \d+$
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
                // \d+ service \S+ source \D+ \d+ destination \D+ \d+$
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
                // \d+ service \S+ source range \d+ \d+ destination range \d+ \d+$
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
                // \d+ service \S+ source range \d+ \d+ destination \D+ \d+$
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
                // \d+ service \S+ source \D+ \d+ destination range \d+ \d+$
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
    private void parseRule() throws IOException, ParserConfigurationException, SAXException {
        Rule data = null;
        Boolean flag = false;
        int count = dao.count("rule");
        int countNet = dao.count("net");
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
        BufferedReader reader = new BufferedReader(new FileReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            } else {
                line = line.trim().replaceAll("\\\\","/");
            }
            // rule \d+ name (".*?"|\S+)$
            if (header.matcher(line).find()) {
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
                data = new Rule();
                srcSetIds = new HashSet<>();
                dstSetIds = new HashSet<>();
                srcNetIds = new HashSet<>();
                dstNetIds = new HashSet<>();
                srcZoneIds = new HashSet<>();
                dstZoneIds = new HashSet<>();
                serviceIds = new HashSet<>();
                data.setName(line.split(split)[3].replace("\"",""));
                flag = true;
            }
            if (flag) {
                // action (".*?"|\S+)
                if (action.matcher(line).find()) {
                    data.setAction(line.replace("action", "").trim());
                }
                // source-zone (".*?"|\S+)$
                else if (srcZone.matcher(line).find()) {
                    srcZoneIds.add(dao.addZone(line.split(split)[1].replace("\"","")));
                }
                // source-ip (".*?"|\S+)$
                else if (srcSet.matcher(line).find()) {
                    srcSetIds.add(dao.addSet(line.split(split)[1].replace("\"","")));
                }
                // source-ip-host (".*?"|\S+)$
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
                // source-ip-subnet (".*?"|\S+) (".*?"|\S+)$
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
                // destination-zone (".*?"|\S+)$
                else if (dstZone.matcher(line).find()) {
                    dstZoneIds.add(dao.addZone(line.split(split)[1].replace("\"","")));
                }
                // destination-ip (".*?"|\S+)$
                else if (dstSet.matcher(line).find()) {
                    dstSetIds.add(dao.addSet(line.split(split)[1].replace("\"","")));
                }
                // destination-ip-host (".*?"|\S+)$
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
                // destination-ip-subnet (".*?"|\S+) (".*?"|\S+)$
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
