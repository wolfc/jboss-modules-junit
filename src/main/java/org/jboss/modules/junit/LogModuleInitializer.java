/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.modules.junit;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.log.JDKModuleLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.LogManager;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class LogModuleInitializer {
    private static ClassLoader doSetContextClassLoader(final ClassLoader classLoader) {
        try {
            return Thread.currentThread().getContextClassLoader();
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    static void initialize(final ModuleLoader loader, final ModuleIdentifier logManagerModuleIdentifier) throws ModuleLoadException, IOException {
        final ModuleClassLoader classLoader = loader.loadModule(logManagerModuleIdentifier).getClassLoader();
        final InputStream stream = classLoader.getResourceAsStream("META-INF/services/java.util.logging.LogManager");
        if (stream != null) {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String name = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    final int i = line.indexOf('#');
                    if (i != -1) {
                        line = line.substring(0, i);
                    }
                    line = line.trim();
                    if (line.length() == 0) continue;
                    name = line;
                    break;
                }
                if (name != null) {
                    System.setProperty("java.util.logging.manager", name);
                    final ClassLoader old = setContextClassLoader(classLoader);
                    try {
                        if (LogManager.getLogManager().getClass() == LogManager.class) {
                            System.err.println("WARNING: Failed to load the specified logmodule " + logManagerModuleIdentifier);
                        } else {
                            Module.setModuleLogger(new JDKModuleLogger());
                        }
                    } finally {
                        setContextClassLoader(old);
                    }
                } else {
                    System.err.println("WARNING: No log manager services defined in specified logmodule " + logManagerModuleIdentifier);
                }
            } finally {
                try {
                    stream.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        } else {
            System.err.println("WARNING: No log manager service descriptor found in specified logmodule " + logManagerModuleIdentifier);
        }
    }

    private static ClassLoader setContextClassLoader(final ClassLoader classLoader) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return doSetContextClassLoader(classLoader);
                }
            });
        }
        return doSetContextClassLoader(classLoader);
    }
}
