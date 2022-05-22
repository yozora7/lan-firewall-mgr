package pers.yozora7.lanfirewallmgr.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SAXParserHandler extends DefaultHandler {
    private Map<String, String> map = null;
    private List<Map<String, String>> list = null;
    String currentTag = null;
    String currentValue = null;
    String nodeName;

    public SAXParserHandler (String nodeName) {
        this.nodeName = nodeName;
    }

    public List<Map<String, String>> getList() {
        return list;
    }

    // 开始解析XML根元素时调用该方法
    @Override
    public void startDocument() throws SAXException {
        list = new ArrayList<>();
    }

    // 开始解析每个元素时调用该方法
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        // 判断正在解析的元素是不是开始解析的元素
        if (qName.equals(nodeName)) {
            map = new HashMap<>();
        }

        // 判断正在解析的元素是否有属性值, 若有则将其保存到map中
//        if (attributes != null && map != null) {
//            for (int i = 0; i < attributes.getLength(); i++) {
//                map.put(attributes.getQName(i), attributes.getValue(i));
//            }
//        }
        currentTag = qName; // 正在解析的元素
    }

    // 解析每个元素的内容时会调用此方法
    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
        if (currentTag != null && map != null) {
            currentValue = new String(chars, start, length);
            // 如果内容不为空或换行符, 将该元素的名称和值和存入map中
            if (currentValue != null && !currentValue.trim().equals("") && !currentValue.trim().equals("\n")) {
                map.put(currentTag, currentValue);
            }
            // 当前的元素已解析过, 置空标识以解析下一个元素
            currentTag = null;
            currentValue = null;
        }
    }

    // 每个元素结束的时候都会调用该方法
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // 判断是否为一个节点结束的元素标签
        if (qName.equals(nodeName)) {
            list.add(map);
            map = null;
        }
    }

    // 解析根元素结束标签时调用该方法
    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
    }
}