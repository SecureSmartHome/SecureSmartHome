package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import de.unipassau.isl.evs.ssh.app.R;

/**
 * As this Activity also displays information like wether the light is on or not, this Activity also
 * needs to messages concerning that information.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String SAVED_LAST_ACTIVE_FRAGMENT = "de.unipassau.isl.evs.ssh.app.activity.SAVED_LAST_ACTIVE_FRAGMENT";

    //TODO toolbar
    private NavigationView navigationView = null;
    private Toolbar toolbar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Set the fragment initially

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null || !savedInstanceState.containsKey(SAVED_LAST_ACTIVE_FRAGMENT)) {
            MainFragment fragment = new MainFragment();
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_LAST_ACTIVE_FRAGMENT, true);
    }

    /**
     * Displays a fragment and takes care of livecycle actions like saving state when rotating the
     * screen or managing the back button behavior.
     *
     * @param id the resource id of the fragment
     */
    public void showFragmentByID(int id) {
        Fragment fragment;
        if (id == R.id.nav_home) {
            fragment = new MainFragment();
        } else if (id == R.id.nav_door) {
            fragment = new DoorFragment();
        } else if (id == R.id.nav_light) {
            fragment = new LightFragment();
        } else if (id == R.id.nav_climate) {
            fragment = new ClimateFragment();
        } else if (id == R.id.nav_holiday) {
            fragment = new HolidayFragment();
        } else if (id == R.id.nav_status) {
            fragment = new StatusFragment();
        } else if (id == R.id.nav_modifyPermissions) {
            fragment = new ModifyPermissionFragment();
        } else if (id == R.id.nav_addNewUserDevice) {
            fragment = new AddNewUserDeviceFragment();
        } else if (id == R.id.nav_addModul) {
            fragment = new AddModulFragment();
        } else {
            throw new IllegalArgumentException("Unknown id: " + id);
        }
        android.support.v4.app.FragmentTransaction fragmentTransaction =
                getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        showFragmentByID(id);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}