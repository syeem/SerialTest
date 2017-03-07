package com.structo.serialtest;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author TeckWee
 */
public class CommSerialPort implements SerialPortEventListener, ICommSerialPort {

    private static final int BAUDRATE = SerialPort.BAUDRATE_9600;
    private static final int DATABITS = SerialPort.DATABITS_8;
    private static final int STOPBITS = SerialPort.STOPBITS_1;
    private static final int PARITY = SerialPort.PARITY_EVEN;
    private static final int MASK = SerialPort.MASK_RXCHAR;
    private static final String LINE_TERMINATOR = "\r\n";
    private static final Logger LOGGER = LogManager.getLogger(CommSerialPort.class);

    private final SerialPort serialPort;
    private final StringBuilder inputMessage;

    private ICommHandler commHandler;

    public CommSerialPort(String port) {

        serialPort = new SerialPort(port);
        inputMessage = new StringBuilder();
    }

    @Override
    public boolean open() {
        try {
            if (serialPort.openPort()) {
                serialPort.setParams(BAUDRATE, DATABITS, STOPBITS, PARITY);
                serialPort.setEventsMask(MASK);
                serialPort.addEventListener(this);

                LOGGER.info("Serial communication port opened successfully");
                return true;
            }
        } catch (SerialPortException openConnectionException) {
            LOGGER.error("Exception opening comm port: " + openConnectionException);
        }

        return false;
    }

    @Override
    public void close() {

        if (!serialPort.isOpened()) {
            return;
        }

        try {
            serialPort.closePort();
        } catch (SerialPortException disconnectException) {
            LOGGER.error("Exception closing comm port: " + disconnectException);
        }
    }

    @Override
    public boolean write(String data) {

        try {
            return serialPort.writeString(data + LINE_TERMINATOR);
        } catch (SerialPortException writeException) {
            LOGGER.error("Exception writing to serial port: " + writeException);
        }

        return false;
    }

    @Override
    public boolean write(byte[] bytes) {

        try {
            return serialPort.writeBytes(bytes);
        } catch (SerialPortException writeException) {
            LOGGER.error("Exception writing to serial port: " + writeException);
        }

        return false;
    }

    @Override
    public boolean isOpened() {
        return serialPort.isOpened();
    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        System.out.println("SERIAL EVENT");
        if (serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() > 0) {
            try {

                byte[] buffer = serialPort.readBytes();
                for (byte b : buffer) {
                    if (b == '\n' && inputMessage.length() > 0) {
                        String data = inputMessage.toString().replaceAll("(\\r|\\n)", "");
                        if (commHandler != null) {
                            commHandler.handleData(data);
                        }
                        inputMessage.setLength(0);
                    } else {
                        inputMessage.append((char) b);
                    }
                }
            } catch (SerialPortException ex) {
                LOGGER.error("Exception serial port event: " + ex);
            }
        }
    }

    @Override
    public void setDataHandler(ICommHandler commHandler) {
        this.commHandler = commHandler;
    }
}
