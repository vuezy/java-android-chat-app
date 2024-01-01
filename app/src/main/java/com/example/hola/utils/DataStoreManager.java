package com.example.hola.utils;

import android.content.Context;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import io.reactivex.rxjava3.core.Single;

public class DataStoreManager {

    private static RxDataStore<Preferences> dataStore;
    private static DataStoreManager instance = null;

    private DataStoreManager() {}

    public static DataStoreManager getInstance(Context context) {
        if (instance == null)
            instance = new DataStoreManager();

        if (dataStore == null)
            initDataStore(context);

        return instance;
    }

    private static void initDataStore(Context context) {
        dataStore = new RxPreferenceDataStoreBuilder(context, Constants.KEY_USER).build();
    }

    public <T> void putValue(String key, T value) {
        Preferences.Key<T> prefsKey = new Preferences.Key<>(key);
        dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutablePreferences = prefs.toMutablePreferences();
            mutablePreferences.set(prefsKey, value);
            return Single.just(mutablePreferences);
        }).blockingSubscribe();
    }

    public <T> T getValue(String key) {
        Preferences.Key<T> prefsKey = new Preferences.Key<>(key);
        try {
            Single<T> value = dataStore.data().firstOrError()
                    .map(prefs -> prefs.get(prefsKey));
            return value.blockingGet();
        }
        catch (Exception e) {
            return null;
        }
    }

    public <T> void remove(String key) {
        Preferences.Key<T> prefsKey = new Preferences.Key<>(key);
        dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutablePreferences = prefs.toMutablePreferences();
            mutablePreferences.remove(prefsKey);
            return Single.just(mutablePreferences);
        }).blockingSubscribe();
    }

    public void clear() {
        dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutablePreferences = prefs.toMutablePreferences();
            mutablePreferences.clear();
            return Single.just(mutablePreferences);
        }).blockingSubscribe();
    }
}
