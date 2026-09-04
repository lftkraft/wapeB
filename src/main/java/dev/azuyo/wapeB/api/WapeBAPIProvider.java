package dev.azuyo.wapeB.api;

import dev.azuyo.wapeB.WapeB;

public final class WapeBAPIProvider {

    private WapeBAPIProvider() {}

    public static WapeBAPI getAPI() {
        return WapeB.getApi();
    }
}
