package org.vaadin.testbenchsauce;

public interface Retryable<T> {
    public abstract T run();
    
}
