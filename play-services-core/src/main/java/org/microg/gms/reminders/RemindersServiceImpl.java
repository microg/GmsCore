/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.reminders;

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.reminders.AccountState;
import com.google.android.gms.reminders.CreateReminderOptionsInternal;
import com.google.android.gms.reminders.LoadRemindersOptions;
import com.google.android.gms.reminders.ReindexDueDatesOptions;
import com.google.android.gms.reminders.UpdateRecurrenceOptions;
import com.google.android.gms.reminders.internal.IRemindersCallbacks;
import com.google.android.gms.reminders.internal.IRemindersService;
import com.google.android.gms.reminders.model.CustomizedSnoozePresetEntity;
import com.google.android.gms.reminders.model.TaskEntity;
import com.google.android.gms.reminders.model.TaskIdEntity;

import java.util.List;

public class RemindersServiceImpl extends IRemindersService.Stub {
    private static final String TAG = RemindersServiceImpl.class.getSimpleName();

    @Override
    public void loadReminders(IRemindersCallbacks callbacks, LoadRemindersOptions options) throws RemoteException {
        Log.d(TAG, "unimplemented Method: loadReminders");

    }

    @Override
    public void addListener(IRemindersCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: addListener");

    }

    @Override
    public void createReminder(IRemindersCallbacks callbacks, TaskEntity task) throws RemoteException {
        Log.d(TAG, "unimplemented Method: createReminder");

    }

    @Override
    public void updateReminder(IRemindersCallbacks callbacks, TaskEntity task) throws RemoteException {
        Log.d(TAG, "unimplemented Method: updateReminder");

    }

    @Override
    public void deleteReminder(IRemindersCallbacks callbacks, TaskIdEntity taskId) throws RemoteException {
        Log.d(TAG, "unimplemented Method: deleteReminder");

    }

    @Override
    public void bumpReminder(IRemindersCallbacks callbacks, TaskIdEntity taskId) throws RemoteException {
        Log.d(TAG, "unimplemented Method: bumpReminder");

    }

    @Override
    public void hasUpcomingReminders(IRemindersCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: hasUpcomingReminders");

    }

    @Override
    public void createRecurrence(IRemindersCallbacks callbacks, TaskEntity task) throws RemoteException {
        Log.d(TAG, "unimplemented Method: createRecurrence");

    }

    @Override
    public void updateRecurrence(IRemindersCallbacks callbacks, String s1, TaskEntity task, UpdateRecurrenceOptions options) throws RemoteException {
        Log.d(TAG, "unimplemented Method: updateRecurrence");

    }

    @Override
    public void deleteRecurrence(IRemindersCallbacks callbacks, String s1, UpdateRecurrenceOptions options) throws RemoteException {
        Log.d(TAG, "unimplemented Method: deleteRecurrence");

    }

    @Override
    public void changeRecurrence(IRemindersCallbacks callbacks, String s1, TaskEntity task, UpdateRecurrenceOptions options) throws RemoteException {
        Log.d(TAG, "unimplemented Method: changeRecurrence");

    }

    @Override
    public void makeTaskRecurring(IRemindersCallbacks callbacks, TaskEntity task) throws RemoteException {
        Log.d(TAG, "unimplemented Method: makeTaskRecurring");

    }

    @Override
    public void makeRecurrenceSingleInstance(IRemindersCallbacks callbacks, String s1, TaskEntity task, UpdateRecurrenceOptions options) throws RemoteException {
        Log.d(TAG, "unimplemented Method: makeRecurrenceSingleInstance");

    }

    @Override
    public void clearListeners() throws RemoteException {
        Log.d(TAG, "unimplemented Method: clearListeners");

    }

    @Override
    public void batchUpdateReminders(IRemindersCallbacks callbacks, List<TaskEntity> tasks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: batchUpdateReminders");

    }

    @Override
    public void createReminderWithOptions(IRemindersCallbacks callbacks, TaskEntity task, CreateReminderOptionsInternal options) throws RemoteException {
        Log.d(TAG, "unimplemented Method: createReminderWithOptions");

    }

    @Override
    public void getCustomizedSnoozePreset(IRemindersCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getCustomizedSnoozePreset");

    }

    @Override
    public void setCustomizedSnoozePreset(IRemindersCallbacks callbacks, CustomizedSnoozePresetEntity preset) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setCustomizedSnoozePreset");

    }

    @Override
    public void setAccountState(IRemindersCallbacks callbacks, AccountState accountState) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setAccountState");

    }

    @Override
    public void getAccountState(IRemindersCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getAccountState");

    }

    @Override
    public void checkReindexDueDatesNeeded(IRemindersCallbacks callbacks, ReindexDueDatesOptions options) throws RemoteException {
        Log.d(TAG, "unimplemented Method: checkReindexDueDatesNeeded");

    }

    @Override
    public void reindexDueDates(IRemindersCallbacks callbacks, ReindexDueDatesOptions options) throws RemoteException {
        Log.d(TAG, "unimplemented Method: reindexDueDates");

    }
}
