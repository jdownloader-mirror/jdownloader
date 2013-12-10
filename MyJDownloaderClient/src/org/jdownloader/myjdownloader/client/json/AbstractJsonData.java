package org.jdownloader.myjdownloader.client.json;

/**
 * Copyright (c) 2009 - 2013 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.storage
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

/**
 * @author Thomas
 * 
 */
public abstract class AbstractJsonData implements JsonFactoryInterface {
    private static final HashMap<Class<?>, Collection<GetterSetter>> GETTER_SETTER_CACHE = new HashMap<Class<?>, Collection<GetterSetter>>();

    public static boolean isBoolean(final Type type) {
        return type == Boolean.class || type == boolean.class;
    }

    public static boolean isNotEmpty(final String ip) {
        return !(ip == null || ip.trim().length() == 0);
    }

    /**
     * @return
     */
    public static Collection<GetterSetter> getGettersSetteres( Class<?> clazz) {
        Collection<GetterSetter> ret = GETTER_SETTER_CACHE.get(clazz);
        if (ret != null) { return ret; }
        final Class<?> org = clazz;
        synchronized (GETTER_SETTER_CACHE) {
            ret = GETTER_SETTER_CACHE.get(clazz);
            if (ret != null) { return ret; }
            final HashMap<String, GetterSetter> map = new HashMap<String, GetterSetter>();
            while (clazz != null) {
                for (final Method m : clazz.getDeclaredMethods()) {
                    String key = null;
                    boolean getter = false;
                    if (m.getName().startsWith("is") && isBoolean(m.getReturnType()) && m.getParameterTypes().length == 0) {
                        key = m.getName().substring(2);
                        getter = true;

                    } else if (m.getName().startsWith("get") && m.getParameterTypes().length == 0) {
                        key = m.getName().substring(3);
                        getter = true;

                    } else if (m.getName().startsWith("set") && m.getParameterTypes().length == 1) {
                        key = m.getName().substring(3);
                        getter = false;

                    }

                    if (isNotEmpty(key)) {
                        GetterSetter v = map.get(key);
                        if (v == null) {
                            v = new GetterSetter(key);
                            map.put(key, v);
                        }
                        if (getter) {
                            v.setGetter(m);
                        } else {
                            v.setSetter(m);
                        }
                        Field field;
                        try {
                            field = clazz.getField(key.substring(0, 1).toLowerCase(Locale.ENGLISH) + key.substring(1));
                            v.setField(field);
                        } catch (final NoSuchFieldException e) {
                        }

                    }
                }
                clazz = clazz.getSuperclass();
            }
            GETTER_SETTER_CACHE.put(org, map.values());
            return GETTER_SETTER_CACHE.get(org);
        }

    }

    @Override
    public String toJsonString() {
        final HashMap<String, Object> map = new HashMap<String, Object>();
        Object obj = null;
        try {
            final AbstractJsonData empty = getClass().newInstance();

            for (final GetterSetter gs : getGettersSetteres(getClass())) {

                obj = gs.get(this);
                if (obj == gs.get(empty)) {
                    continue;
                }
                map.put(gs.getKey(), obj);

            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        return MyJDJsonMapper.HANDLER.objectToJSon(map);
    }
}
