package com.example.inventory.domain;

import java.util.concurrent.locks.ReentrantLock;

public final class SkuStock {

    private final String sku;
    private int onHand;
    private int reserved;
    private final ReentrantLock lock = new ReentrantLock();

    public SkuStock(String sku, int onHand) {
        this.sku = sku;
        this.onHand = onHand;
        this.reserved = 0;
    }

    public String sku() {
        return sku;
    }

    public int onHand() {
        lock.lock();
        try {
            return onHand;
        } finally {
            lock.unlock();
        }
    }

    public int reserved() {
        lock.lock();
        try {
            return reserved;
        } finally {
            lock.unlock();
        }
    }

    public int available() {
        lock.lock();
        try {
            return onHand - reserved;
        } finally {
            lock.unlock();
        }
    }

    public boolean tryReserve(int units) {
        lock.lock();
        try {
            if (units > onHand - reserved) {
                return false;
            }
            reserved += units;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void release(int units) {
        lock.lock();
        try {
            reserved = Math.max(0, reserved - units);
        } finally {
            lock.unlock();
        }
    }

    public boolean tryCommit(int units) {
        lock.lock();
        try {
            if (units > reserved || units > onHand) {
                return false;
            }
            reserved -= units;
            onHand -= units;
            return true;
        } finally {
            lock.unlock();
        }
    }
}
