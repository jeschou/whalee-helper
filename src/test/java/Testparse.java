import cn.whale.helper.utils.ProtoMeta;
import cn.whale.helper.utils.Utils;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.TypeElement;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class Testparse {
    @Test
    public void TestA() throws IOException {
        ProtoMeta pm = new ProtoMeta("/Users/jessen/work/whgo");
        ProtoFileElement pf = pm.parse("proto/account/account.proto");

        Map<String, TypeElement> typeMaps = pm.typeMaps;
        //System.out.println(pf);
        TypeElement GetShopOwnerListResp = pf.getTypes().stream().filter((te) -> te.getName().equals("GetShopOwnerListResp")).findFirst().get();

        Map<String, Object> typeJson = pm.toJsonMap(GetShopOwnerListResp);

        System.out.println(Utils.mapper.writeValueAsString(typeJson));

        //System.out.println(pf);
    }


}
