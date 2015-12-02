package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import de.unipassau.isl.evs.ssh.app.AppContainer;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;

/**
 * As this Activity also displays information like whether the light is on or not, this Activity also
 * needs to messages concerning that information.
 *
 * @author bucher
 * @author Wolfgang Popp
 */
public class MainActivity extends BoundActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String SAVED_LAST_ACTIVE_FRAGMENT = "de.unipassau.isl.evs.ssh.app.activity.SAVED_LAST_ACTIVE_FRAGMENT";

    private NavigationView navigationView = null;
    private Toolbar toolbar = null;

    public MainActivity() {
        super(AppContainer.class);
    }

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

        if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_LAST_ACTIVE_FRAGMENT)) {
            try {
                showFragmentByClass(Class.forName(savedInstanceState.getString(SAVED_LAST_ACTIVE_FRAGMENT)));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
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
        outState.putString(SAVED_LAST_ACTIVE_FRAGMENT, getCurrentFragment().getClass().getSimpleName());
    }

    // returns the currently displayed Fragment
    private Fragment getCurrentFragment(){
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    /**
     * Displays a fragment and takes care of livecycle actions like saving state when rotating the
     * screen or managing the back button behavior.
     *
     * @param clazz the class of the fragment to show
     */
    public void showFragmentByClass(Class clazz){
        Class oldFragment = getCurrentFragment().getClass();
        Fragment fragment = null;
        try {
            fragment = (Fragment) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        if (fragment != null) {
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            if (!oldFragment.isInstance(fragment)) {
                fragmentTransaction.addToBackStack(null);
            }
            fragmentTransaction.commit();
        }
    }

    // maps button resource ids to Fragment classes.
    private void showFragmentById(int id) {
        Class clazz;
        if (id == R.id.nav_home) {
            clazz = MainFragment.class;
        } else if (id == R.id.nav_door) {
            clazz = DoorFragment.class;
        } else if (id == R.id.nav_light) {
            clazz = LightFragment.class;
        } else if (id == R.id.nav_climate) {
            clazz = ClimateFragment.class;
        } else if (id == R.id.nav_holiday) {
            clazz = HolidayFragment.class;
        } else if (id == R.id.nav_status) {
            clazz = StatusFragment.class;
        } else if (id == R.id.nav_modifyPermissions) {
            clazz = ModifyPermissionFragment.class;
        } else if (id == R.id.nav_addNewUserDevice) {
            clazz = AddNewUserDeviceFragment.class;
        } else if (id == R.id.nav_addModul) {
            clazz = AddModuleFragment.class;
        } else {
            throw new IllegalArgumentException("Unknown id: " + id);
        }
        showFragmentByClass(clazz);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        showFragmentById(id);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}