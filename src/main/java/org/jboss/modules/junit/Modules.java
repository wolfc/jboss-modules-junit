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
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class Modules extends Runner {
    private final Runner delegate;

    public Modules(Class<?> cls) throws InitializationError {
        final String path = getPathOf(cls);
        System.setProperty("module.path", path);
        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        ModuleIdentifier identifier = ModuleIdentifier.create(cls.getName());
        try {
            Module module = moduleLoader.loadModule(identifier);
            Class<?> other = module.getClassLoader().loadClass(cls.getName());
            this.delegate = new JUnit4(other);
        } catch (ModuleLoadException e) {
            throw new InitializationError(e);
        } catch (ClassNotFoundException e) {
            throw new InitializationError(e);
        }
    }

    @Override
    public Description getDescription() {
        return delegate.getDescription();
    }

    private static String getPathOf(Class<?> cls) {
        return cls.getProtectionDomain().getCodeSource().getLocation().getFile();
    }

    @Override
    public void run(RunNotifier notifier) {
        delegate.run(notifier);
    }
}
