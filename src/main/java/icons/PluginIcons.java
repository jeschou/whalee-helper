package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface PluginIcons {
    Icon BAZEL = IconLoader.getIcon("/icons/bazel-icon.svg", PluginIcons.class);
    Icon PROTOBUF = IconLoader.getIcon("/icons/protobuf.svg", PluginIcons.class);
    Icon REPO = IconLoader.getIcon("/icons/repo.svg", PluginIcons.class);
    Icon SWAG = IconLoader.getIcon("/icons/swagger.svg", PluginIcons.class);
    Icon UPLOAD = IconLoader.getIcon("/icons/upload.svg", PluginIcons.class);
    Icon DEPLOY = IconLoader.getIcon("/icons/deploy.svg", PluginIcons.class);
    Icon RANCHER = IconLoader.getIcon("/icons/rancher.svg", PluginIcons.class);
    Icon BLOCK = IconLoader.getIcon("/icons/blocks.svg", PluginIcons.class);
}