package com.vaadin.demo.dashboard;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.ErrorEvent;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Notification;
import org.apache.commons.lang.math.RandomUtils;

public class DashboardErrorHandler extends DefaultErrorHandler {
    private static final Logger logger = Logger.getLogger(DashboardErrorHandler.class.getName());
    
    @Override
    public void error(ErrorEvent errorEvent) {
        Throwable throwable = errorEvent.getThrowable();
        if (throwable instanceof SocketException) {
            // Most likely client browser closed socket
            logger.info("SocketException in CommunicationManager. Most likely client (browser) closed socket.");
            return;
        }
        long id = RandomUtils.nextInt(99999);

        Throwable rootThrowable = throwable.getCause();
        
        if( rootThrowable != null ) {
            while( rootThrowable.getCause() != null ){
                rootThrowable = rootThrowable.getCause();
            }
        } else {
            rootThrowable = throwable;
        }
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        rootThrowable.printStackTrace(printWriter);
        String exceptionString = stringWriter.toString();
        String exceptionHtml = "<pre id=\"systemErrorDetailsId\" style=\"display:none;\">" + VaadinServlet.safeEscapeForHtml(exceptionString)+ "</pre>";

        Notification notification = new Notification("Cheesy Looking System Error Dialog", "System Error Occurred - Error ID: " + id + exceptionHtml, Notification.Type.ERROR_MESSAGE, true);
        notification.setDelayMsec(5000);//TODO: would be nicer to use a version of Vaadin's red internal error popup, not sure how to get this in.
        notification.show(Page.getCurrent());

        // also print the error on console
        logger.log(Level.SEVERE, "", throwable);
    }
}
