package com.mm.mm;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.lifecycle.ViewModelProvider;

/**
 * MainActivity — Activity utama.
 *
 * INSTRUKSI:
 * 1. Tulis logika di onCreate()
 * 2. Hubungkan dengan layout: res/layout/activity_main.xml
 * 3. Add new classes as needed
 */
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getStatusText().observe(this, text -> {
            // TODO: Update UI dengan data dari ViewModel
        });

        NavController navController =
            Navigation.findNavController(this, R.id.nav_host_fragment);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        NavigationUI.setupWithNavController(bottomNav, navController);

        // TODO: Write your code here
    }
}
