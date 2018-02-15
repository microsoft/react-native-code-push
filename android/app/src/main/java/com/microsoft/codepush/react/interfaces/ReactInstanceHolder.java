package com.microsoft.codepush.react.interfaces;

import com.facebook.react.ReactInstanceManager;

/**
 * Provides access to a {@link ReactInstanceManager}.
 * <p>
 * ReactNativeHost already implements this interface, if you make use of that react-native
 * component (just add `implements ReactInstanceHolder`).
 */
public interface ReactInstanceHolder {

    /**
     * Get the current {@link ReactInstanceManager} instance. May return null.
     */
    ReactInstanceManager getReactInstanceManager();
}
