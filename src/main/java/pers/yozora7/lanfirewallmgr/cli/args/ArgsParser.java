package pers.yozora7.lanfirewallmgr.cli.args;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 命令行程序参数解析
 */
public class ArgsParser {
    private static ObjectMapper mapper = new ObjectMapper();

    public static <T> T parse(String[] args, Class<T> tClass) {
        CommandLineParser parser = new DefaultParser();
        // 获取字段
        Field[] declaredFields = tClass.getDeclaredFields();
        // 使用字段创建options
        Options options = new Options();
        for (Field field : declaredFields) {
            String name = field.getName();
            Option option = Option.builder(name).argName(name).desc(name).hasArg(true).type(field.getType()).build();
            options.addOption(option);
        }
        try {
            CommandLine cl = parser.parse(options, args);
            Map<String, String> map = new HashMap<>();
            for (Field argsField : declaredFields) {
                String name = argsField.getName();
                map.put(name, cl.getOptionValue(name));
            }
            return mapper.readValue(mapper.writeValueAsString(map), tClass);
        } catch (ParseException | JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

}
