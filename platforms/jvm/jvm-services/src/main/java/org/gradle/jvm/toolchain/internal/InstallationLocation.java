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

import org.gradle.api.Describable;

import java.io.File;

/**
 * Represents the filesystem location of a JVM installation along with how it was discovered.
 * <p>
 * Locations are created via the three static factory methods, which reflect how the installation
 * was found: explicitly configured by the user ({@link #userDefined}), discovered automatically
 * by a toolchain detection supplier ({@link #autoDetected}), or downloaded and provisioned on
 * demand ({@link #autoProvisioned}).
 */
public class InstallationLocation implements Describable {

    /**
     * Creates a location that was explicitly configured by the user (e.g., via
     * {@code jvm.toolchains.installations} or the {@code org.gradle.java.installations.paths} property).
     * <p>
     * Problems with user-defined locations are reported at a higher severity than auto-detected ones.
     *
     * @param location the root directory of the JVM installation
     * @param source   a human-readable label describing where this location came from
     * @return a new user-defined {@code InstallationLocation}
     */
    public static InstallationLocation userDefined(File location, String source) {
        return new InstallationLocation(location, source, false, false);
    }

    /**
     * Creates a location discovered automatically by an {@link InstallationSupplier} (e.g., from
     * the current JVM, environment variables, OS-specific paths, or version manager directories).
     *
     * @param location the root directory of the JVM installation
     * @param source   a human-readable label describing the detection mechanism
     * @return a new auto-detected {@code InstallationLocation}
     */
    public static InstallationLocation autoDetected(File location, String source) {
        return new InstallationLocation(location, source, true, false);
    }

    /**
     * Creates a location for a JVM that was downloaded and installed by Gradle's toolchain
     * auto-provisioning support.
     *
     * @param location the root directory where the JVM was provisioned
     * @param source   a human-readable label describing the provisioning source
     * @return a new auto-provisioned {@code InstallationLocation}
     */
    public static InstallationLocation autoProvisioned(File location, String source) {
        return new InstallationLocation(location, source, true, true);
    }

    private final File location;

    private final String source;

    private final boolean autoDetected;

    private final boolean autoProvisioned;

    private InstallationLocation(File location, String source, boolean autoDetected, boolean autoProvisioned) {
        this.location = location;
        this.source = source;
        this.autoDetected = autoDetected;
        this.autoProvisioned = autoProvisioned;
    }

    /**
     * Returns the root directory of this JVM installation.
     */
    public File getLocation() {
        return location;
    }

    @Override
    public String getDisplayName() {
        return "'" + location.getAbsolutePath() + "' (" + source + ")" + (autoDetected? " auto-detected" : "") + (autoProvisioned? " auto-provisioned" : "");
    }

    /**
     * Returns a human-readable label describing how this location was discovered (e.g.,
     * {@code "environment variable 'JAVA_HOME'"} or {@code "Toolchain Repositories"}).
     */
    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    /**
     * Flag for if this location was auto-detected, i.e. not explicitly defined by the user.
     *
     * This is used to lower the severity of issues related to this location.
     */
    public boolean isAutoDetected() {
        return autoDetected;
    }

    /**
     * Returns {@code true} if this location was downloaded and provisioned on demand by Gradle.
     * Auto-provisioned locations are always also {@link #isAutoDetected() auto-detected}.
     */
    public boolean isAutoProvisioned() {
        return autoProvisioned;
    }

    /**
     * Returns a copy of this location pointing at a different directory while keeping the same
     * source label and auto-detection flags.
     *
     * @param location the new root directory to use
     * @return a new {@code InstallationLocation} with the updated path
     */
    public InstallationLocation withLocation(File location) {
        return new InstallationLocation(location, source, autoDetected, autoProvisioned);
    }
}
