package tw.xserver.utils.logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {
    private final String TAG, ERRTAG;

    public Logger(final String TAG) {
        this.TAG = Color.RESET + '[' + Color.GREEN + TAG + Color.RESET + ']' + ' ';
        this.ERRTAG = Color.RESET + '[' + Color.RED + TAG + Color.RESET + ']' + ' ';
    }

    public static void LOGln(final String msg) {
        System.out.println('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + msg);
    }

    public static void LOG(final String msg) {
        System.out.print('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + msg);
    }

    public static void WARNln(final String msg) {
        System.err.println('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + msg);
    }

    public static void WARN(final String msg) {
        System.err.print('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + msg);
    }

    public void logln(final String msg) {
        System.out.println('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + TAG + msg);
    }

    public void log(final String msg) {
        System.out.print('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + TAG + msg);
    }

    public void warnln(final String msg) {
        System.err.println('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + ERRTAG + msg);
    }

    public void warn(final String msg) {
        System.err.print('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + ERRTAG + msg);
    }
}
