package org.jdownloader.extensions.captchapush;

import jd.controlling.captcha.CaptchaEventSender;
import jd.plugins.AddonPanel;

import org.appwork.txtresource.TranslateInterface;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.logging.LogController;

public class CaptchaPushExtension extends AbstractExtension<CaptchaPushConfig, TranslateInterface> {

    private CaptchaPushConfigPanel configPanel;

    private CaptchaPushService     service;

    public CaptchaPushExtension() {
        super();
        setTitle("Captcha Push");
    }

    @Override
    public boolean isDefaultEnabled() {
        return false;
    }

    @Override
    public String getIconKey() {
        return "ocr";
    }

    @Override
    protected void stop() throws StopException {
        stopService();
    }

    @Override
    protected void start() throws StartException {
        startService();
    }

    private void startService() {
        LogController.CL().info("Start the MQTT Service ...");
        LogController.CL().info("Broker " + getSettings().getBrokerHost() + ":" + getSettings().getBrokerPort() + " on Topic " + getSettings().getBrokerTopic());

        service.connect();

        CaptchaEventSender.getInstance().addListener(service);

    }

    private void stopService() {
        LogController.CL().info("Stop the MQTT Service ...");

        service.disconnect();

        CaptchaEventSender.getInstance().removeListener(service);

    }

    @Override
    protected void initExtension() throws StartException {

        configPanel = new CaptchaPushConfigPanel(this, getSettings());

        service = new CaptchaPushService(this);

        LogController.CL().info("CaptchaPush: OK");
    }

    @Override
    public ExtensionConfigPanel<CaptchaPushExtension> getConfigPanel() {
        return configPanel;
    }

    @Override
    public boolean isQuickToggleEnabled() {
        return true;
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getDescription() {
        return "This plugin can push any Captcha request to your Android or WebOS Smartphone";
    }

    @Override
    public AddonPanel<CaptchaPushExtension> getGUI() {
        return null;
    }

}