package cn.whale.helper.template;

import cn.whale.helper.utils.Joiner;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TemplateBlockSection extends TemplateSection {
    private TemplateLineSection condition;

    private List<TemplateSection> innerSections = new ArrayList<>();


    public TemplateBlockSection(TemplateLineSection conditionLine) {
        this.condition = conditionLine;
    }

    public void addSection(TemplateSection section) {
        innerSections.add(section);
    }

    @Override
    public String render() {
        if (condition.render() == null) {
            return null;
        }
        return innerSections.stream().map(TemplateSection::render).filter(Objects::nonNull).reduce(Joiner.on("\n")).orElse(null);
    }

    public String toString() {
        return condition.toString();
    }
}
