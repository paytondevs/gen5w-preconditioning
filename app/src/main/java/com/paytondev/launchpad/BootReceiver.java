package com.paytondev.launchpad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Restores a schedule the user armed before the head unit restarted. It never starts
 * conditioning itself and does nothing at all when no schedule is stored.
 */
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        String stored = context.getSharedPreferences(PreconditioningService.PREFS, Context.MODE_PRIVATE)
            .getString(PreconditioningService.SCHEDULE_KEY, null);
        if (PreconditioningSchedule.decode(stored) == null) return;
        context.startService(new Intent(context, PreconditioningService.class)
            .setAction(PreconditioningService.SCHEDULE_RESTORE));
    }
}
