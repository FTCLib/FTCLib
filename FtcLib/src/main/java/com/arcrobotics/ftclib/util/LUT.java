package com.arcrobotics.ftclib.util;

import java.util.HashMap;
import java.util.Map;

/**
 * A lookup table
 */
public class LUT<T extends Number, R> extends HashMap<T, R> {

    public void add(T key, R out) {
        put(key, out);
    }

    /**
     * Returns the closest possible value for the given key.
     *
     * @param key   the input key
     * @return the closest value to the input key
     */
    public R getClosest(T key) {
        T closest = key;
        double diff = Double.POSITIVE_INFINITY;
        for (Map.Entry<T, R> entry : entrySet()) {
            double dif = Math.abs(entry.getKey().doubleValue() - key.doubleValue());
            if (dif <= diff) {
                diff = dif;
                closest = entry.getKey();
            }
        }
        return get(closest);
    }

}
