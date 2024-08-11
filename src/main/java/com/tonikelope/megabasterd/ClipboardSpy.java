/*
 __  __                  _               _               _ 
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/                                         
© Perpetrated by tonikelope since 2016
 */
package com.tonikelope.megabasterd;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

/**
 * @author tonikelope
 */
public class ClipboardSpy implements Runnable, SecureSingleThreadNotifiable, ClipboardChangeObservable {

    private static final int SLEEP = 250;
    private static final Logger LOG = Logger.getLogger(ClipboardSpy.class.getName());

//    private final Clipboard _sysClip;

    private volatile boolean _notified;

    private final ConcurrentLinkedQueue<ClipboardChangeObserver> _observers;

//    private Transferable _contents;

    private final Object _secure_notify_lock;

    private volatile boolean _enabled;

    public ClipboardSpy() {
//        _sysClip = getDefaultToolkit().getSystemClipboard();
        this._notified = false;
        this._enabled = false;
//        _contents = null;
        this._secure_notify_lock = new Object();
        this._observers = new ConcurrentLinkedQueue<>();
    }

//    @Override
//    public Transferable getContents() {
//        return _contents;
//    }

    private void _setEnabled(final boolean enabled) {

        this._enabled = enabled;

        boolean monitor_clipboard = true;

        final String monitor_clipboard_string = DBTools.selectSettingValue("clipboardspy");

        if (monitor_clipboard_string != null) {
            monitor_clipboard = monitor_clipboard_string.equals("yes");
        }

        if (this._enabled && monitor_clipboard) {

//            _contents = getClipboardContents();

            this.notifyChangeToMyObservers();

//            gainOwnership(_contents);

            LOG.log(Level.INFO, "{0} Monitoring clipboard ON...", Thread.currentThread().getName());

        } else if (monitor_clipboard) {
            LOG.log(Level.INFO, "{0} Monitoring clipboard OFF...", Thread.currentThread().getName());
        }
    }

    @Override
    public void secureNotify() {
        synchronized (this._secure_notify_lock) {

            this._notified = true;

            this._secure_notify_lock.notify();
        }
    }

    @Override
    public void secureWait() {

        synchronized (this._secure_notify_lock) {
            while (!this._notified) {

                try {
                    this._secure_notify_lock.wait(1000);
                } catch (final InterruptedException ex) {
                    LOG.log(SEVERE, ex.getMessage());
                }
            }

            this._notified = false;
        }
    }

    @Override
    public void run() {

        this.secureWait();
    }

//    @Override
//    public void lostOwnership(Clipboard c, Transferable t) {
//
//        if (_enabled) {
//
//            _contents = getClipboardContents();
//
//            notifyChangeToMyObservers();
//
//            gainOwnership(_contents);
//        }
//    }

//    private Transferable getClipboardContents() {
//
//        boolean error;
//
//        Transferable c = null;
//
//        do {
//            error = false;
//
//            try {
//
//                c = _sysClip.getContents(this);
//
//            } catch (Exception ex) {
//
//                error = true;
//
//                try {
//                    sleep(SLEEP);
//                } catch (InterruptedException ex1) {
//                    LOG.log(SEVERE, ex1.getMessage());
//                }
//            }
//
//        } while (error);
//
//        return c;
//    }

//    private void gainOwnership(Transferable t) {
//
//        boolean error;
//
//        do {
//            error = false;
//
//            try {
//
//                _sysClip.setContents(t, this);
//
//            } catch (Exception ex) {
//
//                error = true;
//
//                try {
//                    sleep(SLEEP);
//                } catch (InterruptedException ex1) {
//                    LOG.log(SEVERE, ex1.getMessage());
//                }
//            }
//
//        } while (error);
//
//    }

    @Override
    public void attachObserver(final ClipboardChangeObserver observer) {

        if (!this._observers.contains(observer)) {

            this._observers.add(observer);
        }

        if (!this._observers.isEmpty() && !this._enabled) {

            this._setEnabled(true);
        }
    }

    @Override
    public void detachObserver(final ClipboardChangeObserver observer) {

        if (this._observers.contains(observer)) {

            this._observers.remove(observer);

            if (this._observers.isEmpty() && this._enabled) {

                this._setEnabled(false);
            }
        }
    }

    @Override
    public void notifyChangeToMyObservers() {

        this._observers.forEach((o) -> {
            o.notifyClipboardChange();
        });
    }

}
