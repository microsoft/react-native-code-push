package com.microsoft.codepush.common.core;

import com.microsoft.codepush.common.managers.CodePushAcquisitionManager;
import com.microsoft.codepush.common.managers.CodePushRestartManager;
import com.microsoft.codepush.common.managers.CodePushTelemetryManager;
import com.microsoft.codepush.common.managers.CodePushUpdateManager;
import com.microsoft.codepush.common.managers.SettingsManager;

/**
 * Encapsulates managers that {@link CodePushBaseCore} is using.
 */
@SuppressWarnings("WeakerAccess")
public class CodePushManagers {

    /**
     * Instance of {@link CodePushUpdateManager}.
     */
    public final CodePushUpdateManager mUpdateManager;

    /**
     * Instance of {@link CodePushTelemetryManager}.
     */
    public final CodePushTelemetryManager mTelemetryManager;

    /**
     * Instance of {@link SettingsManager}.
     */
    public final SettingsManager mSettingsManager;

    /**
     * Instance of {@link CodePushRestartManager}.
     */
    public final CodePushRestartManager mRestartManager;

    /**
     * Instance of {@link CodePushAcquisitionManager}.
     */
    public final CodePushAcquisitionManager mAcquisitionManager;

    /**
     * Creates instance of {@link CodePushManagers}.
     *
     * @param updateManager      instance of {@link CodePushUpdateManager}.
     * @param telemetryManager   instance of {@link CodePushTelemetryManager}.
     * @param settingsManager    instance of {@link SettingsManager}.
     * @param restartManager     instance of {@link CodePushRestartManager}.
     * @param acquisitionManager instance of {@link CodePushAcquisitionManager}.
     */
    public CodePushManagers(
            CodePushUpdateManager updateManager,
            CodePushTelemetryManager telemetryManager,
            SettingsManager settingsManager,
            CodePushRestartManager restartManager,
            CodePushAcquisitionManager acquisitionManager) {
        this.mUpdateManager = updateManager;
        this.mTelemetryManager = telemetryManager;
        this.mSettingsManager = settingsManager;
        this.mRestartManager = restartManager;
        this.mAcquisitionManager = acquisitionManager;
    }
}
