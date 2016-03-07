package tests;

import org.junit.Test;
import util.JavaVersion;

import static org.junit.Assert.assertEquals;

public class JavaVersionTest {
    @Test
    public void javaVersionParsing_Java8VersionNoBuild_ParsedVersionCorrectly() {
        String version = "1.8.0_60";
        JavaVersion expectedVersion = new JavaVersion(1, 8, 0, 60, 0);
        JavaVersion javaVersion = JavaVersion.fromString(version);
        assertEquals(expectedVersion, javaVersion);
    }

    @Test
    public void javaVersionParsing_Java8VersionWithBuild_ParsedVersionCorrectly() {
        String version = "1.8.0_60-b40";
        JavaVersion expectedVersion = new JavaVersion(1, 8, 0, 60, 40);
        JavaVersion javaVersion = JavaVersion.fromString(version);
        assertEquals(expectedVersion, javaVersion);
    }

    @Test
    public void javaVersionParsing_Java8VersionBigNumbers_ParsedVersionCorrectly() {
        String version = "10.80.100_60";
        JavaVersion expectedVersion = new JavaVersion(10, 80, 100, 60, 0);
        JavaVersion javaVersion = JavaVersion.fromString(version);
        assertEquals(expectedVersion, javaVersion);

        version = "010.080.100_60";
        expectedVersion = new JavaVersion(10, 80, 100, 60, 0);
        javaVersion = JavaVersion.fromString(version);
        assertEquals(expectedVersion, javaVersion);
    }

    @Test
    public void javaVersionParsing_Java8VersionNoUpdateNoBuild_ReturnNull() {
        String version = "1.8.0";
        JavaVersion javaVersion = JavaVersion.fromString(version);
        assertEquals(javaVersion, null);
    }

    @Test
    public void javaVersionParsing_NotJavaVersion_ReturnNull() {
        String version = "this should return null";
        JavaVersion javaVersion = JavaVersion.fromString(version);
        assertEquals(javaVersion, null);

        version = "1.8_9";
        javaVersion = JavaVersion.fromString(version);
        assertEquals(javaVersion, null);

        version = "1.0.7";
        javaVersion = JavaVersion.fromString(version);
        assertEquals(javaVersion, null);
    }

    @Test
    public void javaVersionComparable_Java8Version_HashCodesAndEqualAreCorrect() {
        String version = "1.8.0_60";
        JavaVersion expectedVersion = new JavaVersion(1, 8, 0, 60, 0);
        JavaVersion javaVersion = JavaVersion.fromString(version);
        assertEquals(expectedVersion.hashCode(), javaVersion.hashCode());
        assertEquals(true, expectedVersion.equals(javaVersion));

        version = "9.8.0_60-b10";
        expectedVersion = new JavaVersion(9, 8, 0, 60, 10);
        javaVersion = JavaVersion.fromString(version);
        assertEquals(expectedVersion.hashCode(), javaVersion.hashCode());
        assertEquals(true, expectedVersion.equals(javaVersion));
    }
}
