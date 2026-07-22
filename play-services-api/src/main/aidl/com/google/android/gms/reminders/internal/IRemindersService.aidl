package com.google.android.gms.reminders.internal;

import com.google.android.gms.reminders.internal.IRemindersCallbacks;

import com.google.android.gms.reminders.AccountState;
import com.google.android.gms.reminders.CreateReminderOptionsInternal;
import com.google.android.gms.reminders.LoadRemindersOptions;
import com.google.android.gms.reminders.ReindexDueDatesOptions;
import com.google.android.gms.reminders.UpdateRecurrenceOptions;
import com.google.android.gms.reminders.model.CustomizedSnoozePresetEntity;
import com.google.android.gms.reminders.model.TaskEntity;
import com.google.android.gms.reminders.model.TaskIdEntity;

interface IRemindersService {
    void loadReminders(IRemindersCallbacks callbacks, in LoadRemindersOptions options) = 0;
    void addListener(IRemindersCallbacks callbacks) = 1;
    void createReminder(IRemindersCallbacks callbacks, in TaskEntity task) = 2;
    void updateReminder(IRemindersCallbacks callbacks, in TaskEntity task) = 3;
    void deleteReminder(IRemindersCallbacks callbacks, in TaskIdEntity taskId) = 4;
    void bumpReminder(IRemindersCallbacks callbacks, in TaskIdEntity taskId) = 5;
    void hasUpcomingReminders(IRemindersCallbacks callbacks) = 6;
    void createRecurrence(IRemindersCallbacks callbacks, in TaskEntity task) = 7;
    void updateRecurrence(IRemindersCallbacks callbacks, String s1, in TaskEntity task, in UpdateRecurrenceOptions options) = 8;
    void deleteRecurrence(IRemindersCallbacks callbacks, String s1, in UpdateRecurrenceOptions options) = 9;
    void changeRecurrence(IRemindersCallbacks callbacks, String s1, in TaskEntity task, in UpdateRecurrenceOptions options) = 10;
    void makeTaskRecurring(IRemindersCallbacks callbacks, in TaskEntity task) = 11;
    void makeRecurrenceSingleInstance(IRemindersCallbacks callbacks, String s1, in TaskEntity task, in UpdateRecurrenceOptions options) = 12;
    void clearListeners() = 13;
    void batchUpdateReminders(IRemindersCallbacks callbacks, in List<TaskEntity> tasks) = 14;
    void createReminderWithOptions(IRemindersCallbacks callbacks, in TaskEntity task, in CreateReminderOptionsInternal options) = 15;
    void getCustomizedSnoozePreset(IRemindersCallbacks callbacks) = 16;
    void setCustomizedSnoozePreset(IRemindersCallbacks callbacks, in CustomizedSnoozePresetEntity preset) = 17;
    void setAccountState(IRemindersCallbacks callbacks, in AccountState accountState) = 18;
    void getAccountState(IRemindersCallbacks callbacks) = 19;
    void checkReindexDueDatesNeeded(IRemindersCallbacks callbacks, in ReindexDueDatesOptions options) = 20;
    void reindexDueDates(IRemindersCallbacks callbacks, in ReindexDueDatesOptions options) = 21;
}