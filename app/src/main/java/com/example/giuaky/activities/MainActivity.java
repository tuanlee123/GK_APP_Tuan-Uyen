package com.example.giuaky.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.giuaky.R;
import com.example.giuaky.fragments.CartFragment;
import com.example.giuaky.fragments.HomeFragment;
import com.example.giuaky.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private HomeFragment homeFragment;
    private CartFragment cartFragment;
    private ProfileFragment profileFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupFragments(savedInstanceState);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showFragment(homeFragment);
            } else if (id == R.id.nav_cart) {
                showFragment(cartFragment);
            } else if (id == R.id.nav_profile) {
                showFragment(profileFragment);
            }
            return true;
        });
    }

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("home");
            cartFragment = (CartFragment) getSupportFragmentManager().findFragmentByTag("cart");
            profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag("profile");
        }

        if (homeFragment == null) homeFragment = new HomeFragment();
        if (cartFragment == null) cartFragment = new CartFragment();
        if (profileFragment == null) profileFragment = new ProfileFragment();

        activeFragment = homeFragment;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (!homeFragment.isAdded()) {
            ft.add(R.id.fragmentContainer, homeFragment, "home");
        }
        if (!cartFragment.isAdded()) {
            ft.add(R.id.fragmentContainer, cartFragment, "cart").hide(cartFragment);
        }
        if (!profileFragment.isAdded()) {
            ft.add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment);
        }

        ft.commit();
    }

    private void showFragment(Fragment target) {
        if (target == activeFragment) return;
        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;
    }
}
