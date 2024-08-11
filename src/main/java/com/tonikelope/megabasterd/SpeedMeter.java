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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.tonikelope.megabasterd.MiscTools.formatBytes;

/**
 * @author tonikelope
 */
public class SpeedMeter implements Runnable {

    public static final int SLEEP = 3000;
    public static final int CHUNK_SPEED_QUEUE_MAX_SIZE = 20;
    private static final Logger LOG = Logger.getLogger(SpeedMeter.class.getName());
    //    private final JLabel _speed_label;
//    private final JLabel _rem_label;
    private final TransferenceManager _trans_manager;
    private final ConcurrentHashMap<Transference, HashMap> _transferences;
    private long _speed_counter;
    private long _speed_acumulator;
    private volatile long _max_avg_global_speed;

    SpeedMeter(final TransferenceManager trans_manager) {
//        this._speed_label = sp_label;
//        this._rem_label = rem_label;
        this._trans_manager = trans_manager;
        this._transferences = new ConcurrentHashMap<>();
        this._speed_counter = 0L;
        this._speed_acumulator = 0L;
        this._max_avg_global_speed = 0L;
    }

    private long _getAvgGlobalSpeed() {
        return Math.round((double) this._speed_acumulator / this._speed_counter);
    }

    public void attachTransference(final Transference transference) {

        final HashMap<String, Object> properties = new HashMap<>();

        properties.put("last_progress", transference.getProgress());
        properties.put("no_data_count", 0);

        this._transferences.put(transference, properties);

    }

    public void detachTransference(final Transference transference) {

        if (this._transferences.containsKey(transference)) {
            this._transferences.remove(transference);
        }

    }

    public long getMaxAvgGlobalSpeed() {

        return this._max_avg_global_speed;
    }

    private String calcRemTime(final long seconds) {
        final int days = (int) TimeUnit.SECONDS.toDays(seconds);

        final long hours = TimeUnit.SECONDS.toHours(seconds)
                - TimeUnit.DAYS.toHours(days);

        final long minutes = TimeUnit.SECONDS.toMinutes(seconds)
                - TimeUnit.DAYS.toMinutes(days)
                - TimeUnit.HOURS.toMinutes(hours);

        final long secs = TimeUnit.SECONDS.toSeconds(seconds)
                - TimeUnit.DAYS.toSeconds(days)
                - TimeUnit.HOURS.toSeconds(hours)
                - TimeUnit.MINUTES.toSeconds(minutes);

        return String.format("%dd %d:%02d:%02d", days, hours, minutes, secs);
    }

    private long calcTransferenceSpeed(final Transference transference, final HashMap properties) {

        final long sp;
        final long progress = transference.getProgress();
        long last_progress = (long) properties.get("last_progress");

        int no_data_count = (int) properties.get("no_data_count");

        if (transference.isPaused()) {

            sp = 0;

        } else if (progress > last_progress) {

            final double sleep_time = ((double) SLEEP * (no_data_count + 1)) / 1000;

            final double current_speed = (progress - last_progress) / sleep_time;

            sp = last_progress > 0 ? Math.round(current_speed) : 0;

            last_progress = progress;

            no_data_count = 0;

        } else if (transference instanceof Download) {

            sp = -1;

            no_data_count++;

        } else {

            sp = 0;

            no_data_count++;
        }

        properties.put("last_progress", last_progress);

        properties.put("no_data_count", no_data_count);

        this._transferences.put(transference, properties);

        return sp;
    }

    @Override
    public void run() {
        long global_speed, global_progress, global_size;
        int percent;
        boolean visible = false;

//        _speed_label.setVisible(true);
//        _rem_label.setVisible(true);
//        _speed_label.setText("");
//        _rem_label.setText("");

        do {

            try {

                if (!this._transferences.isEmpty()) {

                    visible = true;

                    global_speed = 0L;

                    for (final Map.Entry<Transference, HashMap> trans_info : this._transferences.entrySet()) {

                        final long trans_sp = this.calcTransferenceSpeed(trans_info.getKey(), trans_info.getValue());

                        if (trans_sp >= 0) {
                            global_speed += trans_sp;
                        }

                        if (trans_sp > 0) {

//                            trans_info.getKey().getView().updateSpeed(formatBytes(trans_sp) + "/s", true);

                        } else {

//                            trans_info.getKey().getView().updateSpeed("------", true);

                        }
                    }

                    global_size = this._trans_manager.get_total_size();

                    global_progress = this._trans_manager.get_total_progress();

                    percent = (int) Math.floor(((double) global_progress / global_size) * 100);

                    if (global_speed > 0) {

                        this._speed_counter++;
                        this._speed_acumulator += global_speed;

                        final long avg_global_speed = this._getAvgGlobalSpeed();

                        if (avg_global_speed > this._max_avg_global_speed) {
                            this._max_avg_global_speed = avg_global_speed;
                        }

//                        _speed_label.setText(formatBytes(global_speed) + "/s");
//
//                        _rem_label.setText(formatBytes(global_progress) + "/" + formatBytes(global_size) + " @ " + formatBytes(avg_global_speed) + "/s @ " + calcRemTime((long) Math.floor((global_size - global_progress) / avg_global_speed)));
                        LOG.info(percent + "% @ " + formatBytes(global_progress) + "/" + formatBytes(global_size) + " @ " + formatBytes(avg_global_speed) + "/s @ " + this.calcRemTime((long) Math.floor((global_size - global_progress) / avg_global_speed)));
                    } else {

//                        _speed_label.setText("------");
//                        _rem_label.setText(formatBytes(global_progress) + "/" + formatBytes(global_size) + " @ --d --:--:--");
                        LOG.info(percent + "% @ " + formatBytes(global_progress) + "/" + formatBytes(global_size) + " @ --d --:--:--");
                    }

                } else if (visible) {

//                    _speed_label.setText("");
//                    _rem_label.setText("");
                    visible = false;
                }

                Thread.sleep(SLEEP);

            } catch (final InterruptedException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }

        } while (true);
    }
}
