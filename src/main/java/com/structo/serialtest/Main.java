/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.structo.serialtest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;

/**
 *
 * @author SyeemMorshed
 */
public class Main {

    private static final String PORT = "COM5";
    private static SerialPort serialPort;
    private static List<String> tags;
    private static final int TAG_LENGTH = 24; //actual tag data length (without white space)
    private static final int HEADER_LENGTH = 12;
    private static final int DATA_LENGTH = 62; //the length of data to be read for a tag (includes white space)
    private static final String HEADER_START = "A0"; //the header starts with this string

    private static final byte[] COMMAND_INIT = new byte[]{(byte) 0xA0, (byte) 0x04, (byte) 0x01, (byte) 0x74, (byte) 0x00, (byte) 0xE7};
    private static final byte[] COMMAND_READ = new byte[]{(byte) 0xA0, (byte) 0x04, (byte) 0x01, (byte) 0x89, (byte) 0x01, (byte) 0xD1};

    private static final int BAUDRATE = SerialPort.BAUDRATE_115200;
    private static final int DATABITS = SerialPort.DATABITS_8;
    private static final int STOPBITS = SerialPort.STOPBITS_1;
    private static final int PARITY = SerialPort.PARITY_NONE;

    public static void main(String[] args) throws InterruptedException, IOException, SerialPortException {
        System.out.println("Start");
        tags = new ArrayList<>();

        String input = "start";
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        initSerialPort();
        while (!input.equals("stop")) {
            input = br.readLine();
            if (input.equals("init")) {
                serialPort.writeBytes(COMMAND_INIT);
            } else if (input.equals("read")) {
                tags.clear();
                serialPort.writeBytes(COMMAND_READ);
            } else {
                break;
            }
        }
        for (String s : tags) {
            if (s != null) {
                System.out.println(getTagId(s));
            }
        }
        serialPort.closePort();
    }

    private static StringBuilder inputMessage;

    private static void initSerialPort() throws SerialPortException, InterruptedException {
        inputMessage = new StringBuilder();
        serialPort = new SerialPort(PORT);

        serialPort.openPort();
        serialPort.setParams(BAUDRATE, DATABITS, STOPBITS, PARITY);

        serialPort.addEventListener((SerialPortEvent spe) -> {
            try {
                String value = serialPort.readHexString();
                if (value == null) {
                    return;
                }

                if (value.startsWith(HEADER_START)) {
                    if (!inputMessage.toString().isEmpty() && inputMessage.toString().length() == DATA_LENGTH) {
                        System.out.println(inputMessage.toString().length());
                        tags.add(inputMessage.toString());
                    }
                    inputMessage.setLength(0);
                    inputMessage.append(value);
                } else {
                    inputMessage.append(" ");
                    inputMessage.append(value);
                }
            } catch (SerialPortException ex) {
                //LOGGER.error("Exception during serial event: " + ex);
            }
        });
    }

    private static String getTagId(String tag) {
        int substringStart = HEADER_LENGTH + 2; //need to include +2 for PC
        return tag.replace(" ", "").substring(substringStart, substringStart + TAG_LENGTH);
    }

}
