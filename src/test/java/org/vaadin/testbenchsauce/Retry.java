package org.vaadin.testbenchsauce;

import org.openqa.selenium.WebDriverException;

public class Retry <T>{
    public static final int RETRY_MILLIS = 250;
    public static final int TIMEOUT_MILLIS = 20000;
    private final Retryable<T> retryable;

    public Retry(Retryable<T> retryable) {
        this.retryable = retryable;
    }

    public T run() {
        Throwable lastException = null;
        long startMillis = System.currentTimeMillis();
        long elapsedMillis = 0;
        while (elapsedMillis < TIMEOUT_MILLIS) {
            try {
                return retryable.run();
            } catch (WebDriverException e) {
                lastException = e;
                sleep();
            } catch (UnsupportedOperationException e) { //happens on resizeViewPortTo()
                lastException = e;
                sleep();
            }
            elapsedMillis = System.currentTimeMillis() - startMillis;
        }
        throw new RuntimeException("Vaadin page unreachable, timed out.", lastException);
    }

    private void sleep() {
        System.out.println("Page temporarily unreachable.., trying again after a short wait...");
        try {
            Thread.sleep(RETRY_MILLIS);
        } catch (InterruptedException ignored) {
        }
    }
}
