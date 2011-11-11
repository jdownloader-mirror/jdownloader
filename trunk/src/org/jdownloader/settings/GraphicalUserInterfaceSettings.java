package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractValidator;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.annotations.ValidatorFactory;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;

public interface GraphicalUserInterfaceSettings extends ConfigInterface {

    public static final GraphicalUserInterfaceSettings                 CFG                                = JsonConfig.create(GraphicalUserInterfaceSettings.class);
    @SuppressWarnings("unchecked")
    public static final StorageHandler<GraphicalUserInterfaceSettings> SH                                 = (StorageHandler<GraphicalUserInterfaceSettings>) CFG.getStorageHandler();
    public static final IntegerKeyHandler                              CAPTCHA_SCALE                      = SH.getKeyHandler("CaptchaScaleFactor", IntegerKeyHandler.class);
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_ENABLED        = SH.getKeyHandler("LinkgrabberSidebarEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_TOGGLE_ENABLED = SH.getKeyHandler("LinkgrabberSidebarToggleButtonEnabled", BooleanKeyHandler.class);

    void setActiveConfigPanel(String name);

    String getActiveConfigPanel();

    class ThemeValidator extends AbstractValidator<String> {

        @Override
        public void validate(String themeID) throws ValidationException {
            if (!Application.getResource("themes/" + themeID).exists()) {
                throw new ValidationException(Application.getResource("themes/" + themeID) + " must exist");
            } else if (!Application.getResource("themes/" + themeID).isDirectory()) { throw new ValidationException(Application.getResource("themes/" + themeID) + " must be a directory"); }
        }

    }

    @DefaultStringValue("standard")
    @AboutConfig
    @Description("Icon Theme ID. Make sure that ./themes/<ID>/ exists")
    @ValidatorFactory(ThemeValidator.class)
    String getThemeID();

    void setThemeID(String themeID);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isBalloonNotificationEnabled();

    void setBalloonNotificationEnabled(boolean b);

    @DefaultBooleanValue(false)
    boolean isConfigViewVisible();

    void setConfigViewVisible(boolean b);

    @DefaultBooleanValue(false)
    boolean isLogViewVisible();

    void setLogViewVisible(boolean b);

    @AboutConfig
    String getLookAndFeel();

    void setLookAndFeel(String laf);

    @DefaultIntValue(20)
    @AboutConfig
    int getDialogDefaultTimeout();

    void setDialogDefaultTimeout(int value);

    @Description("True if move button should be visible in downloadview")
    @RequiresRestart
    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isShowMoveToTopButton();

    void setShowMoveToTopButton(boolean b);

    @Description("True if move button should be visible in downloadview")
    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(true)
    boolean isShowMoveToBottomButton();

    void setShowMoveToBottomButton(boolean b);

    @Description("True if move button should be visible in downloadview")
    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(true)
    boolean isShowMoveUpButton();

    void setShowMoveUpButton(boolean b);

    @AboutConfig
    @Description("True if move button should be visible in downloadview")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isShowMoveDownButton();

    void setShowMoveDownButton(boolean b);

    @AboutConfig
    @Description("Set this to false to hide the Bottombar in the Downloadview")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isDownloadViewBottombarEnabled();

    void setDownloadViewBottombarEnabled(boolean b);

    @AboutConfig
    @Description("Highlight Column in Downloadview if table is not in downloadsortorder")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isSortColumnHighlightEnabled();

    void setSortColumnHighlightEnabled(boolean b);

    @AboutConfig
    @Description("Paint all labels/text with or without antialias. Default value is false.")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isTextAntiAliasEnabled();

    void setTextAntiAliasEnabled(boolean b);

    @AboutConfig
    @Description("Enable/disable support for system DPI settings. Default value is true.")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isFontRespectsSystemDPI();

    void setFontRespectsSystemDPI(boolean b);

    @AboutConfig
    @Description("Font scale factor in percent. Default value is 100 which means no font scaling.")
    @DefaultIntValue(100)
    @RequiresRestart
    int getFontScaleFactor();

    void setFontScaleFactor(int b);

    @AboutConfig
    @Description("Captcha Dialog Image scale Faktor in %")
    @DefaultIntValue(100)
    @SpinnerValidator(min = 50, max = 500, step = 10)
    int getCaptchaScaleFactor();

    void setCaptchaScaleFactor(int b);

    @AboutConfig
    @Description("If enabled, the background of captchas will be removed to fit to the rest of the design (transparency)")
    @DefaultBooleanValue(true)
    boolean isCaptchaBackgroundCleanupEnabled();

    void setCaptchaBackgroundCleanupEnabled(boolean b);

    @AboutConfig
    @Description("Font to be used. Default value is default.")
    @DefaultStringValue("default")
    @RequiresRestart
    String getFontName();

    void setFontName(String name);

    @AboutConfig
    @Description("Disable animation and all animation threads. Optional value. Default value is true.")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isAnimationEnabled();

    void setAnimationEnabled(boolean b);

    @AboutConfig
    @Description("Enable/disable window opacity on Java 6u10 and above. A value of 'false' disables window opacity which means that the window corner background which is visible for non-rectangular windows disappear. Furthermore the shadow for popupMenus makes use of real translucent window. Some themes like SyntheticaSimple2D support translucent titlePanes if opacity is disabled. The property is ignored on JRE's below 6u10. Note: It is recommended to activate this feature only if your graphics hardware acceleration is supported by the JVM - a value of 'false' can affect application performance. Default value is false which means the translucency feature is enabled")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isWindowOpaque();

    void setWindowOpaque(boolean b);

    @AboutConfig
    @Description("Enable/Disable the Linkgrabber Sidebar")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isLinkgrabberSidebarEnabled();

    void setLinkgrabberSidebarEnabled(boolean b);

    @AboutConfig
    @Description("Enable/Disable the Linkgrabber Sidebar QuicktoggleButton")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isLinkgrabberSidebarToggleButtonEnabled();

    void setLinkgrabberSidebarToggleButtonEnabled(boolean b);

}
