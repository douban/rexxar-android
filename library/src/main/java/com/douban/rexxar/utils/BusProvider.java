package com.douban.rexxar.utils;

import android.os.Bundle;

import de.greenrobot.event.EventBus;

/**
 * Created by GoogolMo on 7/10/14.
 */
public final class BusProvider {

    public static EventBus getInstance() {
        return EventBus.getDefault();
    }

    private BusProvider() {

    }

    public static class BusEvent {
        public int eventId;
        public Bundle data;

        public BusEvent(int eventId, Bundle data) {
            this.eventId = eventId;
            this.data = data;
        }
    }
}
