package com.ontrac.warehouse.Utilities;

import android.app.Activity;

import com.ontrac.warehouse.Utilities.Zebra.UIHelper;
import com.ontrac.warehouse.Utilities.UX.General;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

public class LabelPrinterHelper {
    UIHelper helper;

    public LabelPrinterHelper(UIHelper helper) {this.helper = helper;}

    public  void sendFile(String zplString, String ipAddress) {
        Connection connection = null;
        try {
            int port = 9100;
            connection = new TcpConnection(ipAddress, port);
        } catch (NumberFormatException e) {
            helper.showErrorDialogOnGuiThread("Port number is invalid");
            return;
        }
        try {
            connection.open();
            ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
            SendLabelZPL(connection, zplString);
            connection.close();
        } catch (ConnectionException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } catch (ZebraPrinterLanguageUnknownException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } finally {
            helper.dismissLoadingDialog();
        }
    }

    public void SendLabelZPL(Connection printer, String routingLabel) {
        try {
            byte[] configLabel = null;
            printer.write(routingLabel.getBytes());
        } catch (ConnectionException e1) {
            helper.showErrorDialogOnGuiThread("Error sending file to printer");
        }
    }

}
