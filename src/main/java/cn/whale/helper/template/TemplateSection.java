package cn.whale.helper.template;

import java.util.ArrayList;
import java.util.List;

public abstract class TemplateSection {
    protected List<TemplateParam> params = new ArrayList<>();


    public void set(String key, String val) {
        for (TemplateParam p : params) {
            if (p.name.equals(key)) {
                p.value = val;
            }
        }
    }

    /**
     * return null if current section should not render
     *
     * @return
     */
    public abstract String render();
}
