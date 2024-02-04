package tw.xserver.utils.logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {
    private final String TAG, ERRTAG;

    public Logger(final String TAG) {
        this.TAG = Color.RESET + '[' + Color.GREEN + TAG + Color.RESET + ']' + ' ';
        this.ERRTAG = Color.RESET + '[' + Color.RED + TAG + Color.RESET + ']' + ' ';
    }

    public static <T> void LOGln(final T msg) {
        System.out.println('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + msg.toString());
    }

    public static <T> void LOG(final T msg) {
        System.out.print('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + msg.toString());
    }

    public static <T> void WARNln(final T msg) {
        System.err.println('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + msg.toString());
    }

    public static <T> void WARN(final T msg) {
        System.err.print('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + msg.toString());
    }

    public <T> void logln(final T msg) {
        System.out.println('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + TAG + msg.toString());
    }

    public <T> void log(final T msg) {
        System.out.print('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + TAG + msg.toString());
    }

    public <T> void warnln(final T msg) {
        System.err.println('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + ERRTAG + msg.toString());
    }

    public <T> void warn(final T msg) {
        System.err.print('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + ERRTAG + msg.toString());
    }
}
