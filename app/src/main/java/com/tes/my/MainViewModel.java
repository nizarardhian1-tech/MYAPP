package com.tes.my;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * MainViewModel — ViewModel untuk MainActivity.
 * TODO: Add data and business logic here
 */
public class MainViewModel extends ViewModel {

    private final MutableLiveData<String> statusText =
            new MutableLiveData<>("Ready");

    public LiveData<String> getStatusText() { return statusText; }

    public void updateStatus(String text) { statusText.setValue(text); }
}
