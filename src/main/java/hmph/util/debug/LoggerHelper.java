package hmph.util.debug;

public class LoggerHelper {
    public static boolean isThatDebugMode = true;

    public static void betterPrint(String msg, LogType type) {
        String prefix = "";
        String color = "";

        switch (type) {
            case INFO:
                prefix = "[INFO] ";
                color = "\u001B[34m"; // blue
                break;
            case WARNING:
                prefix = "[WARNING] ";
                color = "\u001B[33m"; // yellow
                break;
            case ERROR:
                prefix = "[ERROR] ";
                color = "\u001B[31m"; // red
                break;
            case DEBUG:
                if (isThatDebugMode) {
                    prefix = "[DEBUG] ";
                    color = "\u001B[36m"; // cyan
                } else return;
                break;
            case RENDERING:
                prefix = "[RENDERING] ";
                color = "\u001B[35m"; // magenta
                break;
        }

        String reset = "\u001B[0m";
        System.out.println(color + prefix + msg + reset);
    }

    public enum LogType {
        INFO,
        WARNING,
        ERROR,
        DEBUG,
        RENDERING
    }
}