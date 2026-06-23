/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.jvm.toolchain.internal;

import org.gradle.internal.service.scopes.Scope;
import org.gradle.internal.service.scopes.ServiceScope;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Strategy interface for discovering JVM installation locations on the local machine.
 * <p>
 * Each implementation is responsible for a single discovery mechanism (e.g., reading an
 * environment variable, scanning a well-known OS directory, or querying a version manager).
 * Implementations are registered as build-scoped services and called once per build to
 * collect the full set of candidate toolchain locations.
 * <p>
 * The {@link #get()} method (inherited from {@link Supplier}) returns the discovered
 * {@link InstallationLocation} set, which may be empty when no installations are found.
 * The {@link #getSourceName()} string is used in diagnostics and display names.
 */
@ServiceScope(Scope.Build.class)
public interface InstallationSupplier extends Supplier<Set<InstallationLocation>> {

    /**
     * Returns a short human-readable label that identifies this supplier's detection mechanism
     * (e.g., {@code "environment variable 'JAVA_HOME'"} or {@code "Jabba"}).
     * Used in {@link InstallationLocation#getSource()} display names and warning messages.
     */
    String getSourceName();
}
