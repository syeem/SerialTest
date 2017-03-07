package com.structo.serialtest;

import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Regan
 */
public class CommHandler implements ICommHandler {

    private enum Response {

        Success,
        Error,
        Data,
    }

    private static final int DEFAULT_COMM_TIMEOUT = 5000;                           // In milliseconds
    private static final int STATUS_POLLING_TIME = 100;                             // In milliseconds
    private static final int COMMAND_TIMEOUT = DEFAULT_COMM_TIMEOUT + 1000;         // In milliseconds
    private static final int RETRY_COUNT = 5;
    private static final int UNIFIEDCOMM_DATA_LENGTH = 5;                           // Unified command to PLC returns data of fixed length
    private static final Logger LOGGER = LogManager.getLogger(CommHandler.class);

    private final ReentrantLock lock;

    private ICommSerialPort commSerialPort;
    private CountDownLatch latchForResponse;
    private Response response;
    private Timer timer;
    private String receivedData;

    public CommHandler() {
        lock = new ReentrantLock(true);
    }

    public boolean connect(ICommSerialPort commSerialPort) {

        this.commSerialPort = commSerialPort;
        commSerialPort.open();
        if (commSerialPort.isOpened()) {
            System.out.println("Serial Port Opened");
            commSerialPort.setDataHandler(this);
            return true;
        }
        return false;
    }

    public void disconnect() {

        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void handleData(String data) {
        System.out.println("Handle received data");
        receivedData = data;
        System.out.println(data);
        latchForResponse.countDown();
    }

    public Pair<Boolean, String> sendCommand(String command) {

        try {
            if (lock.tryLock(COMMAND_TIMEOUT, TimeUnit.MILLISECONDS)) {
                try {
                    return sendCommand(command, false);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException ex) {
            LOGGER.info("Exception sending command: " + command);
        }

        LOGGER.debug("Send command FAILED, failed to get lock: " + command);
        return new Pair(false, "");
    }

    private Pair<Boolean, String> sendCommand(String command, boolean retry) {

        for (int count = 0; count < RETRY_COUNT + 1; count++) {

            try {

                latchForResponse = new CountDownLatch(1);
                if (commSerialPort.write(hexStringToByteArray(command))) {

                    if (!latchForResponse.await(DEFAULT_COMM_TIMEOUT, TimeUnit.MILLISECONDS)) {

                        if (!retry) {
                            break;
                        }

                        LOGGER.info("Command timeout: " + command + " Retrying (" + (count + 1) + ")");
                        continue;
                    }

                    if (response == Response.Success || response == Response.Data) {
                        if (retry) {
                            LOGGER.debug("Send command OK: " + command + " Received: " + receivedData);
                        }
                        return new Pair(true, receivedData);
                    } else if (response == Response.Error) {
                        LOGGER.debug("Send command ERROR: " + command + " Received: " + receivedData);
                        return new Pair(false, receivedData);
                    }

                } else {

                    if (!retry) {
                        break;
                    }

                    Thread.sleep(100);  // makes sense to wait a while before trying this since the command failed immediately
                    LOGGER.info("Retrying command: " + command + " (" + (count + 1) + ")");
                }
            } catch (InterruptedException ex) {
                LOGGER.info("Exception sending command: " + command + " Retrying (" + (count + 1) + ")");
            }
        }

        LOGGER.debug("Send command FAILED: " + command);
        return new Pair(false, "");
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        System.out.println("Converting String to byte array for " + s);
        return data;
    }
}
