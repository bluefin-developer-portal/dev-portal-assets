package com.example.smartpos4;


import java.io.Console;
import java.net.UnknownHostException;
import java.io.IOException;
import java.lang.Exception;
import java.lang.InterruptedException;

import at.tecs.smartpos.PaymentService;
import at.tecs.smartpos.connector.ConnectionListener;
import at.tecs.smartpos.data.Transaction;
import at.tecs.smartpos.exception.TransactionFieldException;

public class Connector {

    private PaymentService paymentService;

    Connector() {
        paymentService = new PaymentService();
    }
    public void setConnection(String hostname, int port) {
        paymentService.setHostname(hostname);
        paymentService.setPort(port);
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public void waitForConnection() throws InterruptedException {
        while(!paymentService.isConnected()) {
            System.out.println("NOT CONNECTED");
            Thread.sleep(1000);
        }
    }
}

