package prflrwrapper.prflr;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRFLR profiler. Profiles things.
 * This version works only here (due LL logging)
 * and throws no exceptions at all,
 * just leaves angry messages in the log instead.
 *
 * @author spall
 * @author Андрей Евсюков (IDK his nick)
 * @author cab404
 */
public class PRFLR {

    private static String source = null;
    private static String apiKey = null;

    /**
     * How many timers can be running at once.
     */
    public static Integer overflowCount = 100;

    /**
     * PRFLR.org's IP
     */
    private static InetAddress ip;

    /**
     * Our pocket GMS.
     */
    private static DatagramSocket socket;
    /**
     * Timer storage. Should add Timer class in some point of future.
     */
    private static ConcurrentHashMap<String, Long> timers;
    /**
     * IDK, timer counter?
     */
    private static AtomicInteger counter = new AtomicInteger(0);

    private static String TAG = "PRFLR";
    private static boolean init = false;

    /**
     * Connects to prflr.org and starts communication.
     */
    public static void init(String source, String apiKey) {
        try {
            ip = InetAddress.getByName("prflr.org");
        } catch (UnknownHostException e) {
            Log.e(TAG, e.getMessage());
        }

        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            Log.e(TAG, e.getMessage());
        }

        if (apiKey == null)
            Log.e(TAG, "ApiKey is null");
        if (source == null)
            Log.e(TAG, "Source is null");


        PRFLR.apiKey = cut(apiKey, 32);
        PRFLR.source = cut(source, 32);
        PRFLR.timers = new ConcurrentHashMap<String, Long>();

        init = true;
    }

    /**
     * Starts timer with given name.
     * Timer will be waiting until end() invoked in the same thread
     * and then dispose itself, sending all useful data
     * (delta-time, info you supplied to end(), thread name, etc.) to prflr.org
     */
    public static void begin(String timerName) {
        if (!init) return;
        //overflow control
        if (counter.incrementAndGet() > overflowCount) {
            timers.clear();
            counter.set(0);
        }
        timers.put(Long.toString(Thread.currentThread().getId()) + timerName, System.nanoTime());
    }

    public static void end(String timerName) {
        PRFLR.end(timerName, null);
    }

    /**
     * Sends all useful data
     * (delta-time, info you supplied to end(), thread name, etc.) to prflr.org
     * Should be invoked in thread with the same with thread where begin() was invoked name.
     * That's one of downsides of this realisations. You can't remove timer from another thread,
     * unless you changed it's name.
     */
    public static void end(String timerName, String info) {
        if (!init) return;
        String thread = Long.toString(Thread.currentThread().getId());

        Long startTime = timers.get(thread + timerName);

        if (startTime == null) {
            return;
        }

        counter.decrementAndGet();
        timers.remove(thread + timerName);

        Long now = System.nanoTime();
        Long precision = (long) Math.pow(10, 3);
        Double diffTime = (double) Math.round((double) (now - startTime) / 1000000 * precision) / precision;
        send(timerName, diffTime, thread, info);
    }

    private static String cut(String s, Integer maxLength) {
        if (s.length() < maxLength)
            return s;
        else
            return s.substring(0, maxLength);
    }

    private static void send(String timerName, Double time, String thread, String info) {
        try {
            byte[] raw_data = (
                    cut(thread, 32) + "|"
                            + source + "|"
                            + cut(timerName, 48) + "|"
                            + Double.toString(time) + "|"
                            + cut(info, 32) + "|"
                            + apiKey

            ).getBytes(Charset.forName("UTF-8"));
            socket.send(new DatagramPacket(raw_data, raw_data.length, ip, 4000));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}