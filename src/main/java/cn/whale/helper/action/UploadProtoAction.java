package cn.whale.helper.action;

import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

public class UploadProtoAction extends AnAction {

    static Notifier notifier = Notifier.getInstance("whgo_helper proto");

    static final String PROTO_SERVICE_URL = "https://proto.develop.meetwhale.com/grpc/source/proto";

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);

        Presentation presentation = e.getPresentation();

        if (virtualFile == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        boolean isProto = IDEUtils.isProtoFile(virtualFile);
        if (isProto) {
            presentation.setText("upload " + virtualFile.getName());
        }
        presentation.setEnabledAndVisible(isProto);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        if (project == null || project.getBasePath() == null) {
            return;
        }
        VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            return;
        }

        if (IDEUtils.isProtoFile(virtualFile)) {
            Editor editor = event.getData(PlatformDataKeys.EDITOR);
            if (editor != null) {
                FileDocumentManager.getInstance().saveDocument(editor.getDocument());
            }
            try {
                uploadSingle(project, virtualFile);
            } catch (IOException e) {
                notifier.error(project, e.getMessage());
                throw new RuntimeException(e);
            }
        }

    }

    void uploadSingle(Project project, VirtualFile vf) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(60000).build();
        HttpPost httpPost = new HttpPost(PROTO_SERVICE_URL);
        httpPost.setConfig(requestConfig);
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();


        // source/account/account.proto
        String path = StringUtils.substringAfter(vf.getPath(), "/product/proto/");
        multipartEntityBuilder.addTextBody("name", path);
        // use InputStream of VirtualFile instead of File
        multipartEntityBuilder.addBinaryBody("upload", vf.getInputStream(), ContentType.DEFAULT_BINARY, vf.getName());
        final HttpEntity httpEntity = multipartEntityBuilder.build();
        httpPost.setEntity(httpEntity);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Uploading " + path) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setText("POST " + PROTO_SERVICE_URL);
                progressIndicator.setIndeterminate(true);
                try {
                    CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
                    HttpEntity responseEntity = httpResponse.getEntity();
                    InputStream responseStream = responseEntity.getContent();
                    String responseText = null;
                    if (responseStream != null) {
                        responseText = Utils.readText(responseStream, "UTF-8");
                    }
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode >= 200 && statusCode < 300) {
                        notifier.info(project, String.format("upload %s success", path));
                    } else {
                        if (responseText != null) {
                            notifier.error(project, String.format("upload %s failed\n%s", path, responseText));
                        }
                    }

                    httpClient.close();
                    httpResponse.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }

}
