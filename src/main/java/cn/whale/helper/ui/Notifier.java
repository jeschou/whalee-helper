package cn.whale.helper.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public class Notifier {

    private final NotificationGroup NOTIFICATION_GROUP;

    public Notifier(String displayId) {
        NOTIFICATION_GROUP = new NotificationGroup(displayId, NotificationDisplayType.BALLOON, true);
    }


    public void info(Project project, String content) {
        final Notification notification = NOTIFICATION_GROUP.createNotification(content, NotificationType.INFORMATION);
        notification.notify(project);
    }

    public void warn(Project project, String content) {
        final Notification notification = NOTIFICATION_GROUP.createNotification(content, NotificationType.WARNING);
        notification.notify(project);
    }

    public void error(Project project, String content) {
        final Notification notification = NOTIFICATION_GROUP.createNotification(content, NotificationType.ERROR);
        notification.notify(project);
    }
}
