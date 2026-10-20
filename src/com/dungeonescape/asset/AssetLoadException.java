package com.dungeonescape.asset;

/**
 * Thrown when an asset cannot be located, read, or decoded.
 *
 * <p>The message always names the file and the reason, so a missing, unreadable,
 * or unsupported asset is reported explicitly instead of being silently
 * ignored or falsely treated as loaded.</p>
 */
public class AssetLoadException extends RuntimeException {

    public AssetLoadException(String message) {
        super(message);
    }

    public AssetLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
