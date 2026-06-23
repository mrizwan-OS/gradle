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

package org.gradle.internal.jvm.inspection;

import org.gradle.api.JavaVersion;
import org.gradle.api.internal.jvm.JavaVersionParser;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.internal.serialization.Cached;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Metadata describing a JVM installation discovered on the local machine.
 * <p>
 * Implementations come in two flavours:
 * <ul>
 *   <li>{@link DefaultJvmInstallationMetadata} — a valid installation whose properties can be queried freely.</li>
 *   <li>{@link FailureInstallationMetadata} — an installation that could not be probed; only
 *       {@link #getJavaHome()}, {@link #getErrorMessage()}, {@link #getErrorCause()},
 *       {@link #getDisplayName()}, {@link #getCapabilities()}, and {@link #isValidInstallation()}
 *       are safe to call.</li>
 * </ul>
 * Use {@link #isValidInstallation()} to distinguish between them before calling property accessors.
 */
public interface JvmInstallationMetadata {

    /**
     * Creates metadata for a successfully probed JVM installation.
     *
     * @param javaHome      the root directory of the installation
     * @param javaVersion   raw value of the {@code java.version} system property
     * @param javaVendor    raw value of the {@code java.vendor} system property
     * @param runtimeName   raw value of the {@code java.runtime.name} system property
     * @param runtimeVersion raw value of the {@code java.runtime.version} system property
     * @param jvmName       raw value of the {@code java.vm.name} system property
     * @param jvmVersion    raw value of the {@code java.vm.version} system property
     * @param jvmVendor     raw value of the {@code java.vm.vendor} system property
     * @param architecture  raw value of the {@code os.arch} system property
     * @return a valid {@link DefaultJvmInstallationMetadata}
     */
    static DefaultJvmInstallationMetadata from(
        File javaHome,
        String javaVersion,
        String javaVendor,
        String runtimeName,
        String runtimeVersion,
        String jvmName,
        String jvmVersion,
        String jvmVendor,
        String architecture
    ) {
        return new DefaultJvmInstallationMetadata(javaHome, javaVersion, javaVendor, runtimeName, runtimeVersion, jvmName, jvmVersion, jvmVendor, architecture);
    }

    /**
     * Creates metadata representing a failed probe attempt described by a plain error message.
     *
     * @param javaHome     the root directory that was probed
     * @param errorMessage human-readable description of the failure
     * @return a {@link FailureInstallationMetadata} for the given home
     */
    static JvmInstallationMetadata failure(File javaHome, String errorMessage) {
        return new FailureInstallationMetadata(javaHome, errorMessage, null);
    }

    /**
     * Creates metadata representing a failed probe attempt caused by an exception.
     *
     * @param javaHome the root directory that was probed
     * @param cause    the exception that caused the probe to fail
     * @return a {@link FailureInstallationMetadata} for the given home
     */
    static JvmInstallationMetadata failure(File javaHome, Exception cause) {
        return new FailureInstallationMetadata(javaHome, cause.getMessage(), cause);
    }

    /**
     * Returns the root directory of this JVM installation (i.e., {@code JAVA_HOME}).
     */
    Path getJavaHome();

    /**
     * Parsed equivalent of {@link #getJavaVersion()}.
     */
    JavaVersion getLanguageVersion();

    /**
     * The major Java version parsed from {@link #getJavaVersion()}.
     */
    int getJavaMajorVersion();

    /**
     * A wrapper around the raw value of the toolchain vendor.
     *
     * @see org.gradle.internal.jvm.inspection.ProbedSystemProperty#JAVA_VENDOR
     */
    JvmVendor getVendor();

    /**
     * @see org.gradle.internal.jvm.inspection.ProbedSystemProperty#JAVA_VERSION
     */
    String getJavaVersion();

    /**
     * @see org.gradle.internal.jvm.inspection.ProbedSystemProperty#RUNTIME_NAME
     */
    String getRuntimeName();

    /**
     * @see org.gradle.internal.jvm.inspection.ProbedSystemProperty#RUNTIME_VERSION
     */
    String getRuntimeVersion();

    /**
     * @see org.gradle.internal.jvm.inspection.ProbedSystemProperty#VM_NAME
     */
    String getJvmName();

    /**
     * @see org.gradle.internal.jvm.inspection.ProbedSystemProperty#VM_VERSION
     */
    String getJvmVersion();

    /**
     * @see org.gradle.internal.jvm.inspection.ProbedSystemProperty#VM_VENDOR
     */
    String getJvmVendor();

    /**
     * @see org.gradle.internal.jvm.inspection.ProbedSystemProperty#OS_ARCH
     */
    String getArchitecture();

    /**
     * Returns a human-readable display name for this installation (e.g., {@code "Eclipse Temurin JDK 21 (21.0.3+9)"}).
     * <p>
     * Safe to call on both valid and failed installations.
     */
    String getDisplayName();

    /**
     * Returns the set of {@link JavaInstallationCapability capabilities} present in this installation.
     * <p>
     * Always returns an empty set for failed installations.
     */
    Set<JavaInstallationCapability> getCapabilities();

    /**
     * Returns the error message when the installation could not be probed.
     * <p>
     * Throws {@link UnsupportedOperationException} on valid installations.
     *
     * @see #isValidInstallation()
     */
    String getErrorMessage();

    /**
     * Returns the exception that caused this installation to fail probing, or {@code null} if the
     * failure was recorded as a plain message.
     * <p>
     * Throws {@link UnsupportedOperationException} on valid installations.
     *
     * @see #isValidInstallation()
     */
    Throwable getErrorCause();

    /**
     * Returns {@code true} if the installation was successfully probed and its metadata properties
     * can be queried, {@code false} if probing failed.
     */
    boolean isValidInstallation();

    class DefaultJvmInstallationMetadata implements JvmInstallationMetadata {

        private final Path javaHome;
        private final JavaVersion languageVersion;
        private final int javaMajorVersion;
        private final String javaVersion;
        private final String javaVendor;
        private final String runtimeName;
        private final String runtimeVersion;
        private final String jvmName;
        private final String jvmVersion;
        private final String jvmVendor;
        private final String architecture;

        private final Cached<Set<JavaInstallationCapability>> capabilities = Cached.of(this::gatherCapabilities);

        private DefaultJvmInstallationMetadata(
            File javaHome,
            String javaVersion,
            String javaVendor,
            String runtimeName,
            String runtimeVersion,
            String jvmName,
            String jvmVersion,
            String jvmVendor,
            String architecture
        ) {
            this.javaHome = javaHome.toPath();
            this.languageVersion = JavaVersion.toVersion(javaVersion);
            this.javaMajorVersion = JavaVersionParser.parseMajorVersion(javaVersion);
            this.javaVersion = javaVersion;
            this.javaVendor = javaVendor;
            this.runtimeName = runtimeName;
            this.runtimeVersion = runtimeVersion;
            this.jvmName = jvmName;
            this.jvmVersion = jvmVersion;
            this.jvmVendor = jvmVendor;
            this.architecture = architecture;
        }

        @Override
        public Path getJavaHome() {
            return javaHome;
        }

        @Override
        public JavaVersion getLanguageVersion() {
            return languageVersion;
        }

        @Override
        public int getJavaMajorVersion() {
            return javaMajorVersion;
        }

        @Override
        public JvmVendor getVendor() {
            return JvmVendor.fromString(javaVendor);
        }

        @Override
        public String getJavaVersion() {
            return javaVersion;
        }

        @Override
        public String getRuntimeName() {
            return runtimeName;
        }

        @Override
        public String getRuntimeVersion() {
            return runtimeVersion;
        }

        @Override
        public String getJvmName() {
            return jvmName;
        }

        @Override
        public String getJvmVersion() {
            return jvmVersion;
        }

        @Override
        public String getJvmVendor() {
            return jvmVendor;
        }

        @Override
        public String getArchitecture() {
            return architecture;
        }

        @Override
        public String getDisplayName() {
            final String vendor = determineVendorName();
            return vendor + " " + determineInstallationType() + " " + getJavaMajorVersion() + " (" + getRuntimeVersion() + ")";
        }

        private String determineVendorName() {
            JvmVendor.KnownJvmVendor vendor = getVendor().getKnownVendor();
            if (vendor == JvmVendor.KnownJvmVendor.ORACLE) {
                if (jvmName != null && jvmName.contains("OpenJDK")) {
                    return "OpenJDK";
                }
            }
            return getVendor().getDisplayName();
        }

        private String determineInstallationType() {
            if (getCapabilities().containsAll(JavaInstallationCapability.JDK_CAPABILITIES)) {
                return "JDK";
            } else {
                return "JRE";
            }
        }

        @Override
        public Set<JavaInstallationCapability> getCapabilities() {
            return capabilities.get();
        }

        private Set<JavaInstallationCapability> gatherCapabilities() {
            final Set<JavaInstallationCapability> capabilities = EnumSet.noneOf(JavaInstallationCapability.class);
            if (getToolByExecutable("javac").exists()) {
                capabilities.add(JavaInstallationCapability.JAVA_COMPILER);
            }
            if (getToolByExecutable("javadoc").exists()) {
                capabilities.add(JavaInstallationCapability.JAVADOC_TOOL);
            }
            if (getToolByExecutable("jar").exists()) {
                capabilities.add(JavaInstallationCapability.JAR_TOOL);
            }
            if (getToolByExecutable("native-image").exists()) {
                capabilities.add(JavaInstallationCapability.NATIVE_IMAGE);
            }
            boolean isJ9vm = jvmName.contains("J9");
            if (isJ9vm) {
                capabilities.add(JavaInstallationCapability.J9_VIRTUAL_MACHINE);
            }
            return capabilities;
        }

        private File getToolByExecutable(String name) {
            return new File(new File(javaHome.toFile(), "bin"), OperatingSystem.current().getExecutableName(name));
        }

        @Override
        public String getErrorMessage() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Throwable getErrorCause() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValidInstallation() {
            return true;
        }

        @Override
        public String toString() {
            return "DefaultJvmInstallationMetadata{" +
                    "languageVersion=" + languageVersion +
                    ", javaVersion='" + javaVersion + '\'' +
                    ", javaVendor='" + javaVendor + '\'' +
                    ", runtimeName='" + runtimeName + '\'' +
                    ", runtimeVersion='" + runtimeVersion + '\'' +
                    ", jvmName='" + jvmName + '\'' +
                    ", jvmVersion='" + jvmVersion + '\'' +
                    ", jvmVendor='" + jvmVendor + '\'' +
                    ", architecture='" + architecture + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DefaultJvmInstallationMetadata that = (DefaultJvmInstallationMetadata) o;
            return Objects.equals(javaHome, that.javaHome) && Objects.equals(javaVersion, that.javaVersion) && Objects.equals(javaVendor, that.javaVendor) && Objects.equals(runtimeName, that.runtimeName) && Objects.equals(runtimeVersion, that.runtimeVersion) && Objects.equals(jvmName, that.jvmName) && Objects.equals(jvmVersion, that.jvmVersion) && Objects.equals(jvmVendor, that.jvmVendor) && Objects.equals(architecture, that.architecture);
        }

        @Override
        public int hashCode() {
            return Objects.hash(javaHome, javaVersion, javaVendor, runtimeName, runtimeVersion, jvmName, jvmVersion, jvmVendor, architecture);
        }
    }

    class FailureInstallationMetadata implements JvmInstallationMetadata {

        private final File javaHome;
        private final String errorMessage;
        @Nullable
        private final Exception cause;

        private FailureInstallationMetadata(File javaHome, String errorMessage, @Nullable Exception cause) {
            this.javaHome = javaHome;
            this.errorMessage = errorMessage;
            this.cause = cause;
        }

        @Override
        public Path getJavaHome() {
            return javaHome.toPath();
        }

        @Override
        public JavaVersion getLanguageVersion() {
            throw unsupportedOperation();
        }

        @Override
        public int getJavaMajorVersion() {
            throw unsupportedOperation();
        }

        @Override
        public JvmVendor getVendor() {
            throw unsupportedOperation();
        }

        @Override
        public String getJavaVersion() {
            throw unsupportedOperation();
        }

        @Override
        public String getRuntimeName() {
            throw unsupportedOperation();
        }

        @Override
        public String getRuntimeVersion() {
            throw unsupportedOperation();
        }

        @Override
        public String getJvmName() {
            throw unsupportedOperation();
        }

        @Override
        public String getJvmVersion() {
            throw unsupportedOperation();
        }

        @Override
        public String getJvmVendor() {
            throw unsupportedOperation();
        }

        @Override
        public String getArchitecture() {
            throw unsupportedOperation();
        }

        @Override
        public String getDisplayName() {
            return "Invalid installation: " + getErrorMessage();
        }

        @Override
        public Set<JavaInstallationCapability> getCapabilities() {
            return Collections.emptySet();
        }

        private UnsupportedOperationException unsupportedOperation() {
            return new UnsupportedOperationException("Installation is not valid. Original error message: " + getErrorMessage());
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public Throwable getErrorCause() {
            return cause;
        }

        @Override
        public boolean isValidInstallation() {
            return false;
        }
    }

}
