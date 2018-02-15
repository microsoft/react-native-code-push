package com.microsoft.codepush.react;

import android.content.Context;
import android.content.res.Resources;

import com.microsoft.codepush.common.exceptions.CodePushInvalidPublicKeyException;
import com.microsoft.codepush.common.interfaces.CodePushPublicKeyProvider;

/**
 * React-specific instance of {@link CodePushPublicKeyProvider}.
 */
public class CodePushReactPublicKeyProvider implements CodePushPublicKeyProvider {

    /**
     * Public-key related resource descriptor.
     */
    private Integer mPublicKeyResourceDescriptor;

    /**
     * Instance of application context.
     */
    private Context mContext;

    /**
     * Creates an instance of {@link CodePushReactPublicKeyProvider}.
     *
     * @param publicKeyResourceDescriptor public-key related resource descriptor.
     * @param context                     application context.
     */
    public CodePushReactPublicKeyProvider(final Integer publicKeyResourceDescriptor, final Context context) {
        mPublicKeyResourceDescriptor = publicKeyResourceDescriptor;
        mContext = context;
    }

    @Override
    public String getPublicKey() throws CodePushInvalidPublicKeyException {
        if (mPublicKeyResourceDescriptor == null) {
            return null;
        }
        String publicKey;
        try {
            publicKey = mContext.getString(mPublicKeyResourceDescriptor);
        } catch (Resources.NotFoundException e) {
            throw new CodePushInvalidPublicKeyException(
                    "Unable to get public key, related resource descriptor " +
                            mPublicKeyResourceDescriptor +
                            " can not be found", e
            );
        }
        if (publicKey.isEmpty()) {
            throw new CodePushInvalidPublicKeyException("Specified public key is empty");
        }
        return publicKey;
    }
}
