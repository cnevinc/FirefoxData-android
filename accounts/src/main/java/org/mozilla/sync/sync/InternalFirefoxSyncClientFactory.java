/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.impl.FirefoxAccount;

/**
 * <b>NON-PUBLIC API:</b> please use {@link org.mozilla.sync.FirefoxSync} instead. This class is used to escalate
 * visibility of {@code protected} components for internal library use.
 */
public class InternalFirefoxSyncClientFactory {
    private InternalFirefoxSyncClientFactory() {}

    /** Please don't use directly. */
    public static FirefoxSyncClient getSyncClient(final FirefoxAccount account, final TokenServerToken token,
            final CollectionKeys collectionKeys) {
        return new FirefoxSyncFirefoxAccountClient(account, token, collectionKeys);
    }
}