package com.efimchick.ifmo.collections.countwords;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class MapValueKeyComparator <K extends Comparable<? super K>, V extends Comparable<? super V>>
        implements Comparator <Map.Entry<K, V>> {

    @Override
    public int compare(Map.Entry<K, V> a, Map.Entry<K, V>b) {
        int cmp1 = b.getValue().compareTo(a.getValue());
        if (cmp1 != 0) {
            return cmp1;
        } else {
            return a.getKey().compareTo(b.getKey());
        }
    }


}
