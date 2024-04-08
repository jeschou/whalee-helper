package cn.whale.helper.utils;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class IconFontUtil {
    static String lastJsUrl;
    static Map<String, Map<String, String>> svgMap = new HashMap<>();

    public static void initLoad(){
        try {
            loadSvgMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Map<String, Map<String, String>> loadSvgMap() throws Exception {
        String jsUrl = NacosUtil.getIconFontAlivia();
        jsUrl = Utils.trimToEmpty(jsUrl);
        if (jsUrl.isEmpty()) {
            return svgMap;
        }
        if (jsUrl.equals(lastJsUrl)) {
            return svgMap;
        }
        String jsContent = Utils.readUrlAsTextNoProxy(jsUrl);
        jsContent = Utils.trimToEmpty(jsContent);
        if (jsContent.isEmpty()) {
            return svgMap;
        }
        parseJs(jsContent);
        lastJsUrl = jsUrl;
        return svgMap;
    }

    static void parseJs(String jsContent) throws Exception {
        int idx0 = jsContent.indexOf("JSON.parse(\"");
        int idx1 = jsContent.indexOf("\");", idx0);
        String json = jsContent.substring(idx0 + 12, idx1);
        json = json.replace("\\\"", "\"");
        json = json.replace("\\\\", "\\");
        HashMap id2svgMap = Utils.mapper.readValue(json, HashMap.class);
        idx0 = jsContent.indexOf("var nm=");
        idx1 = jsContent.indexOf(";", idx0);
        json = jsContent.substring(idx0 + 7, idx1);
        HashMap name2idMap = Utils.mapper.readValue(json, HashMap.class);
        name2idMap.forEach((name, id) -> {
            Map m = (Map) id2svgMap.get(id.toString());
            if (m == null) {
                return;
            }
            svgMap.put(name.toString(), m);
        });
    }

    static class MyTranscoder extends ImageTranscoder {
        @Override
        public BufferedImage createImage(int w, int h) {
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            return bi;
        }

        @Override
        public void writeImage(BufferedImage img, TranscoderOutput output) {
            this.img = img;
        }

        public BufferedImage getBufferedImage() {
            return img;
        }

        private BufferedImage img = null;
    }

    static BufferedImage transcode(Map<String, String> svg, float width, float height) throws TranscoderException {
        MyTranscoder transcoder = new MyTranscoder();

        TranscodingHints hints = new TranscodingHints();
        hints.put(ImageTranscoder.KEY_WIDTH, width); // e.g. width=new Float(300)
        hints.put(ImageTranscoder.KEY_HEIGHT, height);// e.g. height=new Float(75)
        hints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation());
        hints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI);
        hints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, SVGConstants.SVG_SVG_TAG);
        hints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, false);
        transcoder.setTranscodingHints(hints);
        ByteArrayInputStream sis = new ByteArrayInputStream(("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"" + svg.get("viewBox") + "\" width=\"" + width + "\" height=\"" + height + "\">" + svg.get("content") + "</svg>").getBytes(StandardCharsets.UTF_8));
        transcoder.transcode(new TranscoderInput(sis), null);
        return transcoder.getBufferedImage();
    }

    public static ImageIcon getSvgIcon(String name) throws Exception {
        Map<String, Map<String, String>> map = loadSvgMap();
        Map<String, String> content = map.get(name);
        if (content == null) {
            return null;
        }
        BufferedImage img = transcode(content, 24, 24);
        if (img == null) {
            return null;
        }
        return new ImageIcon(img);
    }
}
