package com.teddyteh.kmusage;

/*
Copyright (C) 2017  Teddy Teh

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 *  For each KM account, schedule a background job to retrieve the usage data
 */

public class RunOnStartup extends BroadcastReceiver {

    /**
     * After the device boots, start a service to retrieve the KM usage data for each known
     * account
     *
     * @param context   Used to retrieve KM accounts
     * @param intent    ** unused **
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        //  Launch a Firebase jobservice for each KM account
        JobService.scheduleAll(context);
    }
}
