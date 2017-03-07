/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.structo.serialtest;

/**
 *
 * @author teckwee
 */
public interface ICommSerialPort {

    boolean open();

    void close();

    boolean isOpened();

    boolean write(String data);

    boolean write(byte[] bytes);

    void setDataHandler(ICommHandler commSerialHandler);
}
