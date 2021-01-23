/* Derived from:
 * https://github.com/webbukkit/dynmap.git
 * commit bc57d443345a81ba983f594421908ac33fd07940
 * bukkit-helper/src/main/java/org/dynmap/bukkit/helper/BukkitVersionHelperGeneric.java
 * License: Apache License 2.0
 */
package dev.dhdf.polo.bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.collect.ForwardingMultimap;
import com.google.common.collect.Iterables;

/**
 * Helper for isolation of bukkit version specific issues
 */
public class BukkitHelper {
    private String obc_package; // Package used for org.bukkit.craftbukkit
    private boolean failed;
    protected static final Object[] nullargs = new Object[0];

    // CraftPlayer
    private Class<?> obc_craftplayer;
    private Method obcplayer_getprofile;
    // GameProfile
    private Class<?> cma_gameprofile;
    private Method cmaprofile_getproperties;
    // Property
    private Class<?> cma_property;
    private Method cmaproperty_getvalue;

    protected BukkitHelper() {
        failed = false;
        /* Look up base classname for bukkit server - tells us OBC package */
        obc_package = Bukkit.getServer().getClass().getPackage().getName();

        // CraftPlayer
        obc_craftplayer = getOBCClass("org.bukkit.craftbukkit.entity.CraftPlayer");
        obcplayer_getprofile = getMethod(obc_craftplayer, new String[] { "getProfile" }, new Class[0]);
        // GameProfile
        cma_gameprofile = getOBCClass("com.mojang.authlib.GameProfile");
        cmaprofile_getproperties = getMethod(cma_gameprofile, new String[] { "getProperties" }, new Class[0]);
        // Property
        cma_property = getOBCClass("com.mojang.authlib.properties.Property");
        cmaproperty_getvalue = getMethod(cma_property, new String[] { "getValue" }, new Class[0]);

        if (failed)
            throw new IllegalArgumentException("Error initializing bukkit helper - bukkit version incompatible!");
    }

    protected Class<?> getOBCClass(String classname) {
        return getClassByName(classname, "org.bukkit.craftbukkit", obc_package, false);
    }

    protected Class<?> getClassByName(String classname, String base, String mapping, boolean nofail) {
        String n = classname;
        int idx = classname.indexOf(base);
        if (idx >= 0) {
            n = classname.substring(0, idx) + mapping + classname.substring(idx + base.length());
        }
        try {
            return Class.forName(n);
        } catch (ClassNotFoundException cnfx) {
            try {
                return Class.forName(classname);
            } catch (ClassNotFoundException cnfx2) {
                if (!nofail) {
                    Logger logger = Bukkit.getServer().getLogger();
                    logger.warning("Cannot find " + classname);
                    failed = true;
                }
                return null;
            }
        }
    }

    /**
     * Get method
     */
    protected Method getMethod(Class<?> cls, String[] ids, Class<?>[] args) {
        if (cls == null)
            return null;
        for(String id : ids) {
            try {
                return cls.getMethod(id, args);
            } catch (SecurityException e) {
            } catch (NoSuchMethodException e) {
            }
        }
        Logger logger = Bukkit.getServer().getLogger();
        logger.warning("Unable to find method " + ids[0] + " for " + cls.getName());
        failed = true;
        return null;
    }
    protected Object callMethod(Object obj, Method meth, Object[] args, Object def) {
        if ((obj == null) || (meth == null))
            return def;
        try {
            return meth.invoke(obj, args);
        } catch (IllegalArgumentException iax) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        return def;
    }

    /**
     * Get texture (which includes skin URL) for player
     * @param player
     */
    public String getTexture(Player player) {
        Object profile = callMethod(player, obcplayer_getprofile, nullargs, null);
        if (profile != null) {
            Object propmap = callMethod(profile, cmaprofile_getproperties, nullargs, null);
            if ((propmap != null) && (propmap instanceof ForwardingMultimap)) {
                ForwardingMultimap<String, Object> fmm = (ForwardingMultimap<String, Object>) propmap;
                Collection<Object> txt = fmm.get("textures");
                Object textureProperty = Iterables.getFirst(fmm.get("textures"), null);
                if (textureProperty != null)
                    return (String) callMethod(textureProperty, cmaproperty_getvalue, nullargs, null);
            }
        }

        return null;
    }
}
