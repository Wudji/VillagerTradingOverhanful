package com.wudji.villagertradingoverhanful.platform;

import com.wudji.villagertradingoverhanful.Constants;

import java.util.ServiceLoader;

public final class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final ITradeCyclingCompat TRADE_CYCLING = load(ITradeCyclingCompat.class);

    private Services() {
    }

    private static <T> T load(Class<T> clazz) {
        T loadedService = ServiceLoader.load(clazz, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
