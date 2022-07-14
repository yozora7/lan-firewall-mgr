package pers.yozora7.lanfirewallmgr.parser;

import org.xml.sax.SAXException;
import pers.yozora7.lanfirewallmgr.mysql.Dao;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public interface Parser {
    String split = "\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
    void parse(String config, Dao dao) throws IOException, ParserConfigurationException, SAXException;
}
