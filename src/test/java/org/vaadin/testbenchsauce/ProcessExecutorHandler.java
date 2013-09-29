package org.vaadin.testbenchsauce;

public interface ProcessExecutorHandler {
    public void onStandardOutput(String msg);
    public void onStandardError(String msg);

}
