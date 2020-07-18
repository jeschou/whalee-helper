package cn.whale.helper.template;

import cn.whale.helper.utils.Utils;

import java.util.List;

public class TemplateLineSection extends TemplateSection {
    private String line;

    public TemplateLineSection(String line, List<TemplateParam> paramList) {
        this.line = line;
        params = paramList;
    }

    @Override
    public String render() {
        for (TemplateParam p : params) {
            if (p.type < TemplateParam.PARAM_TYPE_NORMAL && Utils.isEmpty(p.value)) {
                return null;
            }
            if (p.value != null)
                line = line.replace(p.prefix + p.name + p.suffix, p.value);
        }
        return line;
    }

    public String toString() {
        return line;
    }
}
