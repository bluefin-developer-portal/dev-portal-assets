package com.example.apiexamples;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import at.tecs.smartpos.PaymentService;
import at.tecs.smartpos.PrinterControl;
import at.tecs.smartpos.connector.ConnectionListener;

import at.tecs.smartpos.data.PrinterReturnCode;
import at.tecs.smartpos.data.Transaction;
import at.tecs.smartpos.exception.TransactionFieldException;

import at.tecs.smartpos.data.Response;
import at.tecs.smartpos.connector.ResponseListener;

import at.tecs.smartpos.utils.Pair;
import at.tecs.ControlParser.Command;
import at.tecs.smartpos.CardControl;
import at.tecs.smartpos.SmartPOSController;
import at.tecs.smartpos.data.RFReturnCode;

import at.tecs.smartpos.data.PrinterPrintType;
import at.tecs.smartpos.data.PrinterReturnCode;

import com.example.apiexamples.Connector;

public class APIExamples {

    private PaymentService paymentService;
    
    APIExamples() {
        String hostname = "localhost";
        int port = 9990;

        Connector connector = new Connector();
        connector.setConnection(hostname, port);


        paymentService = connector.getPaymentService();

        paymentService.connect(new ConnectionListener() {
            @Override
            public void onConnected() {

            }

            @Override
            public void onUnknownHost(UnknownHostException e) {

            }

            @Override
            public void onSocketFail(IOException e) {

            }
        });

        try {
            connector.waitForConnection();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String generatePostfix() {
        StringBuilder postfix = new StringBuilder();

        for(int i = 0; i < 3; i++) {
            Random rand = new Random();

            int randomNum = rand.nextInt(10);
            postfix.append(Integer.toString(randomNum));
        }
        return postfix.toString();


    }

    public String padWithZeros(String str) {

        StringBuilder s = new StringBuilder();

        for(int i = 0; i < 20 - str.length(); i++) {
            s.append('0');
        }

        return s + str;
    }

    public void SaleTransaction() {

        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();

        Transaction transaction = new Transaction();

        transaction.amount = "100";
        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.terminalNum = "TERMINALID";
        transaction.msgType = Transaction.MessageType.SALE;
        transaction.currency = Transaction.Currency.EUR;
        transaction.receiptNum = "1";
        transaction.txOrigin = "1";
        transaction.sourceID = "1";
        transaction.langCode = "EN";

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

        // Alternatively, use the short variant: paymentService.sale(12300000, currentDateTime + postfix, currentDateTime, 100, "EUR");

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {

                if(response.responseCode.equals("0000")) {
                    System.out.println("Transaction Approved");
                    System.out.println("response: " + response.getResponse());
                    transaction.toString(); // transaction object in JSON format - after successful transaction, you can use this to store the transaction somewhere in your application for better merchant's experience
                }

            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });

    }

    public void PRE_AUTHTransaction(){
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();

        Transaction transaction = new Transaction();

        transaction.amount = "100";
        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.terminalNum = "TERMINALID";
        transaction.msgType = Transaction.MessageType.PRE_AUTHORIZATION;
        transaction.currency = Transaction.Currency.EUR;
        transaction.receiptNum = "1";
        transaction.txOrigin = "5";
        transaction.sourceID = "1";
        transaction.langCode = "EN";

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {

                if(response.responseCode.equals("0000")) {
                    System.out.println("Transaction Approved");
                    System.out.println("response: " + response.getResponse());
                    transaction.toString(); // transaction object in JSON format - after successful transaction, you can use this to store the transaction somewhere in your application for better merchant's experience
                }

            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });
    }

    public void refundTransaction() {
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();

        Transaction transaction = new Transaction();

        transaction.amount = "100";
        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.terminalNum = "TERMINALID";
        transaction.msgType = Transaction.MessageType.CREDIT_NOTE;
        transaction.currency = Transaction.Currency.EUR;
        transaction.receiptNum = "1";
        transaction.txOrigin = "1";
        transaction.sourceID = "1";
        transaction.langCode = "EN";

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

// Alternatively, use the short variant: paymentService.refund(12300000, currentDateTime + postfix, currentDateTime, 100, "EUR");

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {

                if(response.responseCode.equals("0000")) {
                    System.out.println("Transaction Approved");
                    System.out.println("response: " + response.getResponse());
                    transaction.toString(); // transaction object in JSON format - after successful transaction, you can use this to store the transaction somewhere in your application for better merchant's experience
                }

            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });
    }

    public void SalePlusTip() {
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();

        Transaction transaction = new Transaction();

        transaction.amount = "100";
        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.terminalNum = "TERMINALID";
        transaction.msgType = Transaction.MessageType.TIP_SALE;
        transaction.currency = Transaction.Currency.EUR;
        transaction.receiptNum = "1";
        transaction.ecrdata = "TIP=10;"; // Tip in cents e.g. 10 = 0.10
        transaction.txOrigin = "1";
        transaction.sourceID = "1";
        transaction.langCode = "EN";

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

// Alternatively, use the short variant: paymentService.sale(12300000, currentDateTime + postfix, currentDateTime, 100, 10, "EUR");

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {

                if(response.responseCode.equals("0000")) {
                    System.out.println("Transaction Approved");
                    System.out.println("response: " + response.getResponse());
                    transaction.toString(); // transaction object in JSON format - after successful transaction, you can use this to store the transaction somewhere in your application for better merchant's experience
                }

            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });
    }

    public void CaptureTransaction() {
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();

        Transaction transaction = new Transaction();

        transaction.amount = "100";
        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.terminalNum = "TERMINALID";
        transaction.msgType = "0017";
        transaction.currency = Transaction.Currency.EUR;
        transaction.receiptNum = "1";
        transaction.txOrigin = "4";
        transaction.sourceID = "1";
        transaction.langCode = "EN";
        transaction.cardNum = "TXID" + padWithZeros("20240515230015056"); // The original transaction Id is padded with zeros for the id to have the length of 20

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {

                if(response.responseCode.equals("0000")) {
                    System.out.println("Transaction Approved");
                    System.out.println("response: " + response.getResponse());
                    transaction.toString(); // transaction object in JSON format - after successful transaction, you can use this to store the transaction somewhere in your application for better merchant's experience
                }

            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });

    }

    public void CancelTransaction() {
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();

        Transaction transaction = new Transaction();

        transaction.amount = "100";
        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.terminalNum = "TERMINALID";
        transaction.msgType = Transaction.MessageType.CANCEL;
        transaction.currency = Transaction.Currency.EUR;
        transaction.receiptNum = "1";
        transaction.txOrigin = "2";
        transaction.sourceID = "1";
        transaction.originInd = "2";
        transaction.langCode = "EN";
        transaction.cardNum = "TXID" + padWithZeros("20240515230015056"); // The original transaction Id is padded with zeros for the id to have the length of 20

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {

                if(response.responseCode.equals("0000")) {
                    System.out.println("Transaction Cancelled");
                    System.out.println("response: " + response.getResponse());
                    transaction.toString(); // transaction object in JSON format - after successful transaction, you can use this to store the transaction somewhere in your application for better merchant's experience
                }

            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });

    }

    public void getTransactionStatus() {
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();

        Transaction transaction = new Transaction();

        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.msgType = Transaction.MessageType.GET_TERMINAL_STATUS;

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {
                System.out.println("response: " + response.getResponse());
            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });

    }

    public void getConnectionStatus() {
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();


        Transaction transaction = new Transaction();

        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.msgType = Transaction.MessageType.CONNECTION_STATUS;

        try {
            // Alternatively
            // paymentService.connectionStatus(String transactionID, String dateTime)
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {
                System.out.println("response: " + response.getResponse());
            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });

    }

    public void ReconciliationRequest() {
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();

        Transaction transaction = new Transaction();

        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.terminalNum = "TERMINALID";
        transaction.msgType = Transaction.MessageType.RECONCILIATION_REQUEST;

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {

                if(response.responseCode.equals("0000")) {
                    // Handle response.getReconciliationResponse()
                    System.out.println(response.getReconciliationResponse());
                }

            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });
    }

    public void AbortTransaction() {
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();


        Transaction transaction = new Transaction();

        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.terminalNum = "TERMINALID";
        transaction.msgType = Transaction.MessageType.ABORT;
        transaction.currency = Transaction.Currency.EUR;
        transaction.receiptNum = "1";
        transaction.txOrigin = "1";
        transaction.sourceID = "1";

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {

                if(response.responseCode.equals("0000")) {
                    System.out.println("Transaction Aborted");
                    System.out.println("response: " + response.getResponse());
                }

            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });

    }

    public void CardBalanceRequest() {
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();

        Transaction transaction = new Transaction();

        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.terminalNum = "TERMINALID";
        transaction.msgType = Transaction.MessageType.BALANCE;
        transaction.originInd = "0";
        transaction.txOrigin = "7";
        transaction.sourceID = "1";

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {

                if(response.responseCode.equals("0000")) {
                    String cardBalance = response.balanceAmount; // Received card balance.
                    System.out.println("Card Balance: " + cardBalance);
                }

            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });
    }

    public void QRBarCodeScanner() {

        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();

        Transaction transaction = new Transaction();

        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.terminalNum = "TERMINALID";
        transaction.msgType = Transaction.MessageType.SCANNER;

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {

                if(response.responseCode.equals("0000")) {
                    String scanData = response.getScanData();
                    // Use scanData
                }

            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });


    }

    public void OfflineTransactionMode() {
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();

        Transaction transaction = new Transaction();

        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.terminalNum = "TERMINALID";
        transaction.msgType = "0019";
        transaction.ecrdata = "FTO=X;"; // where X: 1 - enable offline transactions, 0 - disable

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {

                if(response.responseCode.equals("0000")) {
                    System.out.println("Offline transactions turned on/off");
                    System.out.println("response: " + response.getResponse());
                }

            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });

    }

    public void OfflineTransactionPendingRequest() {
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        String postfix = generatePostfix();

        Transaction transaction = new Transaction();

        transaction.ID = currentDateTime + postfix;
        transaction.dateTime = currentDateTime;
        transaction.terminalNum = "TERMINALID";
        transaction.msgType = "0020";

        try {
            paymentService.sendTransaction(transaction);
        } catch (TransactionFieldException e) {
            throw new RuntimeException(e);
        }

        paymentService.listen(new ResponseListener() {
            @Override
            public void onResponseReceived(Response response) {

                if(response.responseCode.equals("0000")) {
                    String receiptFooter = response.receiptFooter; // OFFLINE_TX:X, where X: the number of pending offline transactions
                    // Use receiptFooter
                }

            }
            @Override
            public void onReadFailed() {
                // ...
            }

            @Override
            public void onDisconnected() {
                // ...
            }

        });

    }

    public void RFCommunicationReadBlock() {
        SmartPOSController smartPOSController = new SmartPOSController();
// The RF interface opens for 10,000 milliseconds, accepting M0 card types
// This operation is asynchronous, manageable with the OpenListener interface for handling its stages
        smartPOSController.openCardReader(10000, Command.CARD_TYPE.M0, new SmartPOSController.OpenCardReaderListener() {
            @Override
            public void onDetected(CardControl cardControl, int cardType, byte[] UUID) {
                // Here with instance of CardControl is possible to communicate with card through RF interface.
                RFReturnCode authenticationResult = cardControl.authenticateM0(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08});
                if(authenticationResult == RFReturnCode.SUCCESS) {
                    // Writing test data to block with ID 0x01
                    RFReturnCode writeResult = cardControl.writeBlock(0x01, new byte[] {0x01, 0x02, 0x03, 0x04, 0x05});
                    if(writeResult == RFReturnCode.SUCCESS) {
                        // Reading from block with ID 0x01
                        Pair<RFReturnCode, byte[]> readResult = cardControl.readBlock(0x01);
                        if(readResult.first == RFReturnCode.SUCCESS && readResult.second != null) {
                            // Printing read data
                            System.out.println("Read data : " + Arrays.toString(readResult.second));
                        }
                    }
                }
                // When the operations with RF communication are done, the interface is to be closed.
                cardControl.close();
            }
            @Override
            public void onError(RFReturnCode returnCode) {
                // Handle return code
            }
        });
    }

    public void RFCommunicationRFTransmit() {
        SmartPOSController smartPOSController = new SmartPOSController();
// The RF interface opens for 10,000 milliseconds, accepting M0 card types
// This operation is asynchronous, manageable with the OpenListener interface for handling its stages
        smartPOSController.openCardReader(10000, Command.CARD_TYPE.M0, new SmartPOSController.OpenCardReaderListener() {
            @Override
            public void onDetected(CardControl cardControl, int cardType, byte[] UUID) {
                // Here with instance of CardControl is possible to communicate with card through RF interface.
                ArrayList<byte[]> request = new ArrayList<>();
                // Read data from block 5 on Ultralight C card
                request.add(Utils.calcCrc(new byte[]{0x30,(byte) 0x05}));
                // Write data to block 5 on Ultralight C card
                request.add(Utils.calcCrc(new byte[]{(byte) 0xA2, 0x05, 0x01, 0x02, 0x03, 0x04}));
                // Perform communication with card
                Pair<RFReturnCode, ArrayList<byte[]>> responseTmp = cardControl.transmit(request);
                // When the operations with RF communication are done, the interface is to be closed.
                cardControl.close();
            }
            @Override
            public void onError(RFReturnCode returnCode) {
                // Handle return code
            }
        });
    }

    public void SmartPOSControllerPrinter() {
        String imageBuffer = "Qk3+EgAAAAAAAD4AAAAoAAAAgAEAAGQAAAABAAEAAAAAAMASAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP///wD/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////wAB////////8////////////////////////////////////////////////////gAAP///////8/////wB////////////////wH///////z///////////////////gAAH///////8/////AAf///8AAAAAf////4AAP/////AAB//////////////////gAAB///////8/////AAf///8AAAAAH////gAAB////4AAAf/////////////////gAAA///////8/////AAf///8AAAAAD///8AAAAf///gAAAH/////////////////gAAAf//////8/////AAf///8AAAAAD///4AAAAH//+AAAAB/////////////////gAAAP//////8/////AAf///8AAAAAB///gAAAAD//4AAAAA/////////////////gAAAP//////8/////AAf///8AAAAAB///AAAAAA//gAAAAAf////////////////gAAAH//////8/////AAf///8AAAAAB//+AAAAAAf/AAAAAAP////////////////gAAAH//////8/////AAf///8AAAAAB//8AAAAAAP+AAAAAAH////////////////gAAAD//////8/////AAf///8AAAAAB//4AAAAAAP+AAAAAAD////////////////gAAAD//////8/////AAf///8AAAAAB//wAAAAAAP+AAAAAAD////////////////gAAAB//////8/////AAf///8AB//////gAAH4AAf+AAD/gAB////////////////gAAAB//////8/////AAf///8AB//////AAA//gAf/AAf/4AB////////////////gAAAB//////8/////AAf///8AB//////AAD//4A//AD//+AB////////////////gAAAB//////8/////AAf///8AB/////+AAP//+B//gH///AA////////////////gAAAB//////8/////AAf///8AB/////+AAf///D//wf///AA////////////////gAAAB//////8/////AAf///8AB/////8AA////////////AA////////////////gAAAB//////8/////AAf///8AB/////8AB////////////gA////////////////gAAAB//////8/////AAf///8AB/////4AB////////////gA////////////////gAAAB//////8/////AAf///8AB/////4AD////////////gA////////////////gAAAB//////8/////AAf///8AB/////wAH////////////AA////////////////gAAAB//////8/////AAf///8AB/////wAH////////////AA////////////////gAAAB//////8/////AAf///8AB/////wAH////////////AA////////////////gAAAB//////8/////AAf///8AB/////gAP///////////+AA////////////////gAAAB//////8/////AAf///8AB/////gAP///////////8AA////////////////gAAAB//////8/////AAf///8AB/////gAP///////////wAB////////////////gAAAB//////8/////AAf///8AB/////gAf///////////gAB////////////////gAAAB//////8/////AAf///8AB/////gAf//////////+AAB////////////////gAAAB//////8/////AAf///8AB/////gAf//////////4AAD////////////////gAAAB//////8/////AAf///8AAAAP//AAf//////////gAAH////////////////gAAAB//////8/////AAf///8AAAAH//AAf/////////8AAAP////////////////gAAAB//////8/////AAf///8AAAAB//AAf/////////wAAAP////////////////gAAAB//////8/////AAf///8AAAAB//AAf/////////AAAAf////////////////gAAABqr////8/////AAf///8AAAAA//AAf////////8AAAB/////////////////gAAABVVf///8/////AAf///8AAAAA//AAf////////wAAAD/////////////////gAAABqqv///8/////AAf///8AAAAA//AA/////////gAAAP/////////////////gAAABVVV///8/////AAf///8AAAAA//AAf////////AAAAf/////////////////gAAABqqq///8/////AAf///8AAAAA//AAf///////+AAAB//////////////////gAAABVVVf//8/////AAf///8AAAAA//AAf///////8AAAH//////////////////gAAABqqqv//8/////AAf///8AAAAA//AAf///////4AAA///////////////////gAAABVUVX//8/////AAf///8AB/////gAf///////wAAD///////////////////gAAABqqqr//8/////AAf///8AB/////gAf///////wAAP///////////////////gAAABVVVV//8/////AAf///8AB/////gAf///////gAA////////////////////gAAABqqqr//8/////AAf///8AB/////gAP///////gAD////////////////////gAAABVVVU//8/////AAf///8AB/////gAP///////AAH////////////////////gAAABqqqq//8/////AAf///8AB/////wAP///////AAf////////////////////gAAABVVVVf/8/////AAf///8AB/////wAH///////AAf////////////////////gAAABqqqq//8/////AAf///8AB/////wAH///////AA/////////////////////gAAABVVVVf/8/////AAf///8AB/////4AD///////AA///////////////////4igAAABqqqqv/8/////AAf///8AB/////4AD///////AB//////////////////8AAgAAABVVVVf/8/////AAf///8AB/////4AB///////AB//////////////////oqKgAAABqqqqv/8/////AAf///8AB/////8AA///////AB/////////////////+AAAgAAABVVVVf/8/////AAf///8AB/////8AA///////AA/////////////////8iIigAAABqqqqv/8/////AAf///8AB/////+AAP///H//AA/////////////////4AAAgAAABVVVVf/8/////AAf///8AB/////+AAH//8B//AA///w/////////////6qqqgAAABqqqqv/8/////AAf///8AB//////AAD//wB//gAf//Af////////////gAAAwAAABVUVVf/8/////AAf///8AB//////gAAf/AA//gAH/8AP////////////IiIiQAAABqqqqv/8/+AABAAf///8AB//////gAAAAAAf/gAA/AAP////////////AAAAYAAABVVVVf/8/+AAAAAAAB/8AAAAAf//wAAAAAAf/wAAAAAH///////////+ioqKoAAABqqqqv/8/+AAAAAAAA/8AAAAAH//4AAAAAAP/wAAAAAH///////////+AAAAMAAABVVVVf/8/+AAAAAAAAf8AAAAAD//8AAAAAAP/4AAAAAD///////////+IiIiMAAABqqqqv/8/+AAAAAAAAf8AAAAAD//+AAAAAAf/8AAAAAD///////////8AAAAGAAABVVVVf/8/+AAAAAAAAP8AAAAAB///AAAAAA//+AAAAAH///////////+qqqqrAAABqqqqv/8/+AAAAAAAAP8AAAAAB///wAAAAD///AAAAAP///////////4AAAABgAABVVVVf/8//AAAAAAAAP8AAAAAB///4AAAAH///gAAAA////////////6IiIiI4AABqqqqv/8//AAAAAAAAP8AAAAAB///+AAAAf///4AAAD////////////4AAAAAMAABVVVVf/8//gAAAAAAAP8AAAAAB////wAAB////+AAAP////////////6ioqKivgABqqqq//8//4AAAAAAAP+AAAAAB////+AAf/////wAD/////////////4AAAAAA/////////////////////////////////////////////////////////6IiIiIiIiJ//////////////////////////////////////////////////////4AAAAAAAAB//////////////////////////////////////////////////////6qqqqqqqqr//////////////////////////////////////////////////////4AAAAAAAAD//////////////////////////////////////////////////////+IiIiIiIiP//////////////////////////////////////////////////////+AAAAAAAAP///////////////////////////////////////////////////////ioqKioqK//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////8=";
        String receiptTicket = "         KUNDENBELEG\n\n\nTID: 88091137   MID: 102003557\nDate: 07/05/2021     Time: 15:30\n\n\nSALE                            \nVISA CREDIT                     \nPAN: ************0119          \nPAN SEQ NO: 01                  \nVISA ACQUIRER TEST/CARD 01     \n\n\nSTAN: 154714                   \nTXID: 20210507153032           \nORIG. TXID: 20210507153032     \nAPPROVAL CODE: 213031          \nINVOICE:1                      \nAC: F46E3CEA8232A966           \nAID: A0000000031010                \n\n\nEUR                        1.00\n\n\n           Authorized\n";

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SmartPOSController smartPOSController = new SmartPOSController();
                    smartPOSController.openPrinter(new SmartPOSController.OpenPrinterListener() {
                        @Override
                        public void onOpened(PrinterControl printerControl) {
                            PrinterReturnCode ret = printerControl.getStatus();
                            if (ret != PrinterReturnCode.SUCCESS) {
                                printerControl.close();
                                return;
                            }
                            ret = printerControl.print(imageBuffer, PrinterPrintType.IMAGE);
                            if (ret != PrinterReturnCode.SUCCESS) {
                                printerControl.close();
                                return;
                            }
                            printerControl.feedLine(2);
                            ret = printerControl.print(receiptTicket, PrinterPrintType.TEXT);
                            if (ret != PrinterReturnCode.SUCCESS) {
                                printerControl.close();
                                return;
                            }
                            printerControl.feedLine(2);
                            ret = printerControl.print("https://www.tecs.at/", PrinterPrintType.QR_SMALL);
                            if (ret != PrinterReturnCode.SUCCESS) {
                                printerControl.close();
                                return;
                            }
                            printerControl.feedLine(10);
                            printerControl.close();
                        }
                        @Override
                        public void onError(PrinterReturnCode printerReturnCode) {
                            System.out.println("PRINTER ERROR");
                            System.out.println(printerReturnCode);
                            smartPOSController.closePrinter();
                        }
                    });

                } finally {

                }

            }
        });
        t1.start();

    }

    
}
