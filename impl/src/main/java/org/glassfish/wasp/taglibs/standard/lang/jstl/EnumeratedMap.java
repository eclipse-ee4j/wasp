/*
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.wasp.taglibs.standard.lang.jstl;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * <p>
 * This is a Map implementation driven by a data source that only provides an enumeration of keys and a getValue(key)
 * method. This class must be subclassed to implement those methods.
 *
 * <p>
 * Some of the methods may incur a performance penalty that involves enumerating the entire data source. In these cases,
 * the Map will try to save the results of that enumeration, but only if the underlying data source is immutable.
 *
 * @author Nathan Abramson - Art Technology Group
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 **/

public abstract class EnumeratedMap<K, V> implements Map<K, V> {
    // -------------------------------------
    // Member variables
    // -------------------------------------

    Map<K, V> mMap;

    // -------------------------------------
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    // -------------------------------------
    @Override
    public boolean containsKey(Object pKey) {
        return getValue(pKey) != null;
    }

    // -------------------------------------
    @Override
    public boolean containsValue(Object pValue) {
        return getAsMap().containsValue(pValue);
    }

    // -------------------------------------
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return getAsMap().entrySet();
    }

    // -------------------------------------
    @Override
    public V get(Object pKey) {
        return getValue(pKey);
    }

    // -------------------------------------
    @Override
    public boolean isEmpty() {
        return !enumerateKeys().hasMoreElements();
    }

    // -------------------------------------
    @Override
    public Set<K> keySet() {
        return getAsMap().keySet();
    }

    // -------------------------------------
    @Override
    public V put(K pKey, V pValue) {
        throw new UnsupportedOperationException();
    }

    // -------------------------------------
    @Override
    public void putAll(Map<? extends K, ? extends V> pMap) {
        throw new UnsupportedOperationException();
    }

    // -------------------------------------
    @Override
    public V remove(Object pKey) {
        throw new UnsupportedOperationException();
    }

    // -------------------------------------
    @Override
    public int size() {
        return getAsMap().size();
    }

    // -------------------------------------
    @Override
    public Collection<V> values() {
        return getAsMap().values();
    }

    // -------------------------------------
    // Abstract methods
    // -------------------------------------
    /**
     *
     * Returns an enumeration of the keys
     **/
    public abstract Enumeration<K> enumerateKeys();

    // -------------------------------------
    /**
     *
     * Returns true if it is possible for this data source to change
     **/
    public abstract boolean isMutable();

    // -------------------------------------
    /**
     *
     * Returns the value associated with the given key, or null if not found.
     **/
    public abstract V getValue(Object pKey);

    // -------------------------------------
    /**
     *
     * Converts the MapSource to a Map. If the map is not mutable, this is cached
     **/
    public Map<K, V> getAsMap() {
        if (mMap != null) {
            return mMap;
        } else {
            Map<K, V> m = convertToMap();
            if (!isMutable()) {
                mMap = m;
            }
            return m;
        }
    }

    // -------------------------------------
    /**
     *
     * Converts to a Map
     **/
    Map<K, V> convertToMap() {
        Map<K, V> ret = new HashMap<>();
        for (Enumeration<K> e = enumerateKeys(); e.hasMoreElements();) {
            K key = e.nextElement();
            V value = getValue(key);
            ret.put(key, value);
        }
        return ret;
    }

    // -------------------------------------
}
