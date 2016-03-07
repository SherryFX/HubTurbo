package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents Java version
 */
public class JavaVersion implements Comparable<JavaVersion> {
    private static final Logger logger = LogManager.getLogger(JavaVersion.class.getName());

    private static final Pattern JAVA_8_VERSION_PATTERN =
            Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)\\_(\\d+)(-b(\\d+))?");

    public static final String REQUIRED_VERSION = "1.8.0_60";

    private final int discard;
    private final int major;
    private final int minor;
    private final int update;
    private final int build;

    public JavaVersion(int discard, int major, int minor, int update, int build) {
        this.discard = discard;
        this.major = major;
        this.minor = minor;
        this.update = update;
        this.build = build;
    }

    public static JavaVersion fromString(String javaVersion) {
        Matcher javaVersionMatcher = JAVA_8_VERSION_PATTERN.matcher(javaVersion);

        if (javaVersionMatcher.find()) {
            return new JavaVersion(Integer.parseInt(javaVersionMatcher.group(1)),
                    Integer.parseInt(javaVersionMatcher.group(2)),
                    Integer.parseInt(javaVersionMatcher.group(3)),
                    Integer.parseInt(javaVersionMatcher.group(4)),
                    Integer.parseInt((javaVersionMatcher.group(6) != null ? javaVersionMatcher.group(6) : "0")));
        } else {
            logger.error("Java version not valid");
            return null;
        }
    }

    public String toString() {
        return String.format("%1$d.%2$d.%3$d_%4$d-b%5$d", discard, major, minor, update, build);
    }

    public int compareTo(JavaVersion other) {
        if (this.equals(other)) {
            return 0;
        }

        return this.discard < other.discard ? -1 :
                this.discard > other.discard ? 1 :
                this.major < other.major ? -1 :
                this.major > other.major ? 1 :
                this.minor < other.minor ? -1 :
                this.minor > other.minor ? 1 :
                this.update < other.update ? -1 :
                this.update > other.update ? 1 :
                this.build < other.build ? -1 : 1;
    }

    @Override
    public int hashCode() {
        String hash = String.format("%1$d%2$d%3$d%4$03d%5$03d", discard, major, minor, update, build);

        return Integer.parseInt(hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!JavaVersion.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final JavaVersion other = (JavaVersion) obj;

        return this.discard == other.discard &&
                this.major == other.major &&
                this.minor == other.minor &&
                this.update == other.update &&
                this.build == other.build;
    }
}
