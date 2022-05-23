package pers.yozora7.lanfirewallmgr.parser;

import org.xml.sax.SAXException;
import pers.yozora7.lanfirewallmgr.service.ConfDao;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public abstract class FirewallConfParser {
    // address-set
    public abstract void parseAddressSet(String config, ConfDao dao) throws IOException, ParserConfigurationException, SAXException;
    // service-set
    public abstract void parseServiceSet(String config, ConfDao dao) throws IOException, ParserConfigurationException, SAXException;
    // service-group
    public abstract void parseServiceGroup(String config, ConfDao dao) throws IOException, ParserConfigurationException, SAXException;
    // rule
    public abstract void parseRule(String config, ConfDao dao) throws IOException, ParserConfigurationException, SAXException;
}
