package net.hypercubemc.iris_installer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

// Based off HanSolo's Detector, with added support for GNOME, and removed support for macOS accent colors for Java 8 compatibility. Original: https://gist.github.com/HanSolo/7cf10b86efff8ca2845bf5ec2dd0fe1d
public class DarkModeDetector {
    public enum OperatingSystem {WINDOWS, MACOS, LINUX, NONE}

    private static final String REGQUERY_UTIL = "reg query ";
    private static final String REGDWORD_TOKEN = "REG_DWORD";
    private static final String DARK_THEME_CMD = REGQUERY_UTIL + "\"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\" /v AppsUseLightTheme";
    private static final Pattern DARK_THEME_PATTERN = Pattern.compile(".*dark.*", Pattern.CASE_INSENSITIVE);

    public static boolean isDarkMode() {
        switch (getOperatingSystem()) {
            case WINDOWS:
                return isWindowsDarkMode();
            case MACOS:
                return isMacOsDarkMode();
            case LINUX:
                return (isGnome() && isGnomeDarkMode()) || (isKde() && isKdeDarkMode());
            default:
                return false;
        }
    }

    private static boolean isMacOsDarkMode() {
        String result = query("defaults read -g AppleInterfaceStyle");
        return "Dark".equals(result);
    }

    private static boolean isWindowsDarkMode() {
        try {
            String result = query(DARK_THEME_CMD);
            int p = result.indexOf(REGDWORD_TOKEN);

            if (p == -1) {
                return false;
            }

            String temp = result.substring(p + REGDWORD_TOKEN.length()).trim();
            return Integer.parseInt(temp.substring(2), 16) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isGnomeDarkMode() {
        String result = query("gsettings get org.gnome.desktop.interface gtk-theme");
        return DARK_THEME_PATTERN.matcher(result).matches();
    }

    private static boolean isGnome() {
        return getOperatingSystem() == OperatingSystem.LINUX && (
                queryResultContains("echo $XDG_CURRENT_DESKTOP", "gnome") ||
                        queryResultContains("echo $XDG_DATA_DIRS | grep -Eo 'gnome'", "gnome") ||
                        queryResultContains("ps -e | grep -E -i \"gnome\"", "gnome")
        );
    }

    private static boolean isKdeDarkMode() {
        String currentLookAndFeel = query("lookandfeeltool --current");
        if (currentLookAndFeel.isEmpty()) {
            String alternativeLookAndFeel = query("kreadconfig5 --group KDE --key LookAndFeelPackage");
            return alternativeLookAndFeel.toLowerCase().contains("dark");
        }
        return currentLookAndFeel.toLowerCase().contains("dark");
    }

    private static boolean isKde() {
        return getOperatingSystem() == OperatingSystem.LINUX && (
                queryResultContains("echo $XDG_CURRENT_DESKTOP", "KDE") ||
                        queryResultContains("echo $XDG_DATA_DIRS | grep -Eo 'kde'", "kde") ||
                        queryResultContains("ps -e | grep -E -i \"kde\"", "kde")
        );
    }

    private static boolean queryResultContains(String cmd, String subResult) {
        return query(cmd).toLowerCase().contains(subResult);
    }

    private static String query(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String actualReadLine;
                while ((actualReadLine = reader.readLine()) != null) {
                    if (stringBuilder.length() != 0)
                        stringBuilder.append('\n');
                    stringBuilder.append(actualReadLine);
                }
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private static OperatingSystem getOperatingSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (os.contains("mac")) {
            return OperatingSystem.MACOS;
        } else if (os.contains("nix") || os.contains("nux")) {
            return OperatingSystem.LINUX;
        } else {
            return OperatingSystem.NONE;
        }
    }
}