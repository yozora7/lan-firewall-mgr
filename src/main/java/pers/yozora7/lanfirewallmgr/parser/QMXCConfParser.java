package pers.yozora7.lanfirewallmgr.parser;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import pers.yozora7.lanfirewallmgr.parser.data.Address;
import pers.yozora7.lanfirewallmgr.parser.data.Rule;
import pers.yozora7.lanfirewallmgr.parser.data.Service;
import pers.yozora7.lanfirewallmgr.service.ConfDao;
import pers.yozora7.lanfirewallmgr.xml.SAXParserHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import static pers.yozora7.lanfirewallmgr.parser.NetUtils.longMask2Short;
import static pers.yozora7.lanfirewallmgr.parser.NetUtils.wildcard2Mask;

public class QMXCConfParser extends FirewallConfParser {
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
    @Override
    public void parseAddressSet(String config, ConfDao dao) throws IOException, ParserConfigurationException, SAXException {

    }

    // service-set
    @Override
    public void parseServiceSet(String config, ConfDao dao) throws IOException, ParserConfigurationException, SAXException {

    }

    // service-group
    @Override
    public void parseServiceGroup(String config, ConfDao dao) throws IOException, ParserConfigurationException, SAXException {

    }

    @Override
    public void parseRule(String config, ConfDao dao) throws IOException, ParserConfigurationException, SAXException {

    }

}