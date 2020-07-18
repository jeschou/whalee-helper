package cn.whale.helper.template;

import java.util.HashMap;
import java.util.Map;

public class TemplateTest {
    public static void main(String[] args) throws Exception {
        SimpleTemplateRender template = new SimpleTemplateRender();
        template.loadTemplate(TemplateTest.class.getResourceAsStream("test.txt"));
        Map<String, String> param = new HashMap<>();
        param.put("show1", "1");
        param.put("show2", "");
        template.render(param);
        System.out.println(template.getResult());
    }
}
