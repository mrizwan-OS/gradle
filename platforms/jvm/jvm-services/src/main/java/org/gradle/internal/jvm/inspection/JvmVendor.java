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

import java.util.regex.Pattern;

/**
 * Represents the vendor of a JVM installation, wrapping the raw {@code java.vendor} system property.
 * <p>
 * Instances are obtained via {@link #fromString(String)}. The {@link #getKnownVendor()} method maps
 * the raw value to a well-known {@link KnownJvmVendor} constant, or {@link KnownJvmVendor#UNKNOWN}
 * when no match can be found.
 */
public interface JvmVendor {

    /**
     * Enumeration of JVM vendors that Gradle recognises by name.
     * <p>
     * Each constant carries an <em>indicator string</em> (the canonical form used for matching),
     * a regex pattern applied to the raw {@code java.vendor} value, and a human-readable display
     * name. Use {@link #UNKNOWN} as the sentinel for unrecognised vendors.
     */
    enum KnownJvmVendor {
        ADOPTIUM("adoptium", "temurin|adoptium|eclipse foundation", "Eclipse Temurin"),
        ADOPTOPENJDK("adoptopenjdk", "aoj|adoptopenjdk", "AdoptOpenJDK"),
        AMAZON("amazon", "amazon|corretto", "Amazon Corretto"),
        APPLE("apple", "Apple"),
        AZUL("azul systems", "azul|zulu", "Azul Zulu"),
        BELLSOFT("bellsoft", "bellsoft|liberica", "BellSoft Liberica"),
        GRAAL_VM("graalvm community", "graalvm|graal vm", "GraalVM Community"),
        HEWLETT_PACKARD("hewlett-packard", "hp|hewlett", "HP-UX"),
        IBM("ibm", "ibm|semeru|international business machines corporation", "IBM"),
        JETBRAINS("jetbrains", "jbr|jetbrains", "JetBrains"),
        MICROSOFT("microsoft", "Microsoft"),
        ORACLE("oracle", "Oracle"),
        SAP("sap se", "sap", "SAP SapMachine"),
        TENCENT("tencent", "tencent|kona", "Tencent"),
        UNKNOWN("gradle", "Unknown Vendor");

        private final String indicatorString;
        private final Pattern indicatorPattern;
        private final String displayName;

        KnownJvmVendor(String indicatorString, String displayName) {
            this.indicatorString = indicatorString;
            this.indicatorPattern = Pattern.compile(indicatorString, Pattern.CASE_INSENSITIVE);
            this.displayName = displayName;
        }

        KnownJvmVendor(String indicatorString, String pattern, String displayName) {
            this.indicatorString = indicatorString;
            this.indicatorPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            this.displayName = displayName;
        }

        private String getDisplayName() {
            return displayName;
        }

        static KnownJvmVendor parse(String rawVendor) {
            if (rawVendor == null) {
                return UNKNOWN;
            }
            for (KnownJvmVendor jvmVendor : KnownJvmVendor.values()) {
                if (jvmVendor.name().equals(rawVendor)) {
                    return jvmVendor;
                }
                if (jvmVendor.indicatorString.equals(rawVendor)) {
                    return jvmVendor;
                }
                if (jvmVendor.indicatorPattern.matcher(rawVendor).find()) {
                    return jvmVendor;
                }
            }
            return UNKNOWN;
        }

        public JvmVendor asJvmVendor() {
            return JvmVendor.fromString(indicatorString);
        }
    }

    /**
     * Returns the raw, unmodified value of the {@code java.vendor} system property for this installation.
     */
    String getRawVendor();

    /**
     * Maps the raw vendor string to a {@link KnownJvmVendor} constant.
     * Returns {@link KnownJvmVendor#UNKNOWN} when no known vendor matches.
     */
    KnownJvmVendor getKnownVendor();

    /**
     * Returns a human-readable display name for this vendor (e.g., {@code "Eclipse Temurin"} or
     * the raw vendor string for unknown vendors).
     */
    String getDisplayName();

    /**
     * Creates a {@link JvmVendor} by wrapping the given raw vendor string.
     * <p>
     * The returned instance performs pattern matching lazily — the raw string is stored as-is and
     * {@link #getKnownVendor()} resolves the {@link KnownJvmVendor} on demand.
     *
     * @param vendor the raw value of the {@code java.vendor} system property; may be {@code null},
     *               in which case {@link #getKnownVendor()} returns {@link KnownJvmVendor#UNKNOWN}
     * @return a {@link JvmVendor} wrapping the given string
     */
    static JvmVendor fromString(String vendor) {
        return new JvmVendor() {

            @Override
            public String getRawVendor() {
                return vendor;
            }

            @Override
            public KnownJvmVendor getKnownVendor() {
                return KnownJvmVendor.parse(vendor);
            }

            @Override
            public String getDisplayName() {
                final KnownJvmVendor knownVendor = getKnownVendor();
                if(knownVendor != KnownJvmVendor.UNKNOWN) {
                    return knownVendor.getDisplayName();
                }
                return getRawVendor();
            }
        };
    }

}
