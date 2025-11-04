package app.aoki.yuki.omapistinks.xposed;

import android.content.Context;

/**
 * Provider interface for lazy Context resolution
 * Allows flexible context retrieval with fallback strategies
 */
public interface ContextProvider {
    /**
     * Get the current Context
     * @return Context instance or null if unavailable
     */
    Context getContext();
}
