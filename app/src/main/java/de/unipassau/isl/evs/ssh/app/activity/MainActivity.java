package de.unipassau.isl.evs.ssh.app.activity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Objects;

import de.unipassau.isl.evs.ssh.app.AppContainer;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.network.ClientConnectionListener;

/**
 * As this Activity also displays information like whether the light is on or not, this Activity also
 * needs to messages concerning that information.
 *
 * @author Andreas Bucher
 * @author Wolfgang Popp
 */
public class MainActivity extends BoundActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final String KEY_NOTIFICATION_FRAGMENT = "NOTIFICATION_FRAGMENT";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_LAST_FRAGMENT = "LAST_FRAGMENT";
    private LinearLayout overlayDisconnected;

    private boolean wasRejected = false;
    private boolean fragmentInitialized = false;
    private Bundle savedInstanceState;

    private ClientConnectionListener connectionListener = new ClientConnectionListener() {
        @Override
        public void onMasterFound() {
        }

        @Override
        public void onClientConnecting(String host, int port) {
        }

        @Override
        public void onClientConnected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    overlayDisconnected.setVisibility(View.GONE);
                }
            });
        }

        @Override
        public void onClientDisconnected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!wasRejected) {
                        showDisconnectedOverlay();
                    }
                }
            });
        }

        @Override
        public void onClientRejected(String message) {
            shutdownService();
            wasRejected = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showConnectionOverlay(getString(R.string.warn_client_rejected));
                }
            });
        }
    };

    public MainActivity() {
        super(AppContainer.class);
    }

    private void showConnectionOverlay(String text) {
        overlayDisconnected.setVisibility(View.VISIBLE);
        TextView textView = (TextView) overlayDisconnected.findViewById(R.id.overlay_text);
        textView.setText(text);
    }

    private void showDisconnectedOverlay() {
        showConnectionOverlay(getString(R.string.warn_no_connection_to_master));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        setContentView(R.layout.activity_main);

        //Set the fragment initially
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        overlayDisconnected = (LinearLayout) findViewById(R.id.overlay_disconnected);

        //Initialise Notifications
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setAutoCancel(true);

        //Initialise NavigationDrawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        showInitialFragment();
    }

    @Override
    public void onContainerConnected(Container container) {
        final Fragment fragment = getCurrentFragment();
        if (fragment instanceof BoundFragment) {
            ((BoundFragment) fragment).onContainerConnected(container);
        }
        showInitialFragment();

        Client client = container.require(Client.KEY);
        client.addListener(connectionListener);

        if (container.require(NamingManager.KEY).isMasterKnown() && !client.isConnectionEstablished()) {
            showDisconnectedOverlay();
        }
    }

    private void showInitialFragment() {
        if (!fragmentInitialized && getContainer() != null) {
            fragmentInitialized = true;
            final Class initialFragment = getInitialFragment();
            if (initialFragment != null) {
                showFragmentByClass(initialFragment);
            }
        }
    }

    @Nullable
    private Class getInitialFragment() {
        NamingManager manager = getComponent(NamingManager.KEY);
        if (manager == null) {
            Log.i(TAG, "Container not yet connected.");
            return null;
        }
        if (!manager.isMasterIDKnown()) {
            return WelcomeScreenFragment.class;
        }

        Class clazz;
        if (getIntent() != null && (clazz = (Class) getIntent().getSerializableExtra(KEY_NOTIFICATION_FRAGMENT)) != null) {
            return clazz;
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_LAST_FRAGMENT)) {
                return null; //fragment will be added automatically by fragment manager
            }
        }
        return MainFragment.class;
    }

    @Override
    public void onContainerDisconnected() {
        final Fragment fragment = getCurrentFragment();
        if (fragment instanceof BoundFragment) {
            ((BoundFragment) fragment).onContainerDisconnected();
        }
        Client client = getComponent(Client.KEY);
        if (client == null) {
            Log.i(TAG, "Container not yet connected.");
            return;
        }
        client.removeListener(connectionListener);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (!getCurrentFragment().getClass().equals(MainFragment.class)) {
            showFragmentByClass(MainFragment.class);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        final Fragment currentFragment = getCurrentFragment();
        if (currentFragment != null) {
            outState.putString(KEY_LAST_FRAGMENT, currentFragment.getClass().getName());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // returns the currently displayed Fragment
    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    /**
     * Displays a fragment and takes care of lifecycle actions like saving state when rotating the
     * screen or managing the back button behavior.
     *
     * @param clazz the class of the fragment to show
     */
    public void showFragmentByClass(Class clazz) {
        showFragmentByClass(clazz, null);
    }

    /**
     * Displays a fragment and takes care of lifecycle actions like saving state when rotating the
     * screen or managing the back button behavior.
     *
     * @param clazz  the class of the fragment to show
     * @param bundle the bundle that is given with the new fragment
     */
    public void showFragmentByClass(Class clazz, Bundle bundle) {
        final Fragment currentFragment = getCurrentFragment();
        if (currentFragment != null && Objects.equals(clazz, currentFragment.getClass())) {
            return;
        }

        final Fragment fragment;
        try {
            fragment = (Fragment) clazz.newInstance();
            if (bundle != null) {
                fragment.setArguments(bundle);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            Log.wtf(TAG, "Could not instantiate fragment", e);
            return;
        }

        // Hide Keyboard before every fragment transaction
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (currentFragment instanceof MainFragment && fragment instanceof HolidayFragment) {
                View fragmentView = currentFragment.getView();
                if (fragmentView != null) {
                    transaction.addSharedElement(fragmentView.findViewById(R.id.holidayButton), "holidayIconTransition");
                }
                final TransitionInflater inflater = TransitionInflater.from(this);
                currentFragment.setSharedElementReturnTransition(
                        inflater.inflateTransition(R.transition.change_image_trans));
                currentFragment.setExitTransition(
                        inflater.inflateTransition(android.R.transition.explode));

                fragment.setSharedElementEnterTransition(
                        inflater.inflateTransition(R.transition.change_image_trans));
                fragment.setEnterTransition(
                        inflater.inflateTransition(android.R.transition.explode));
            } else if (fragment instanceof MainFragment && currentFragment != null) {
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            } else {
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            }
        }

        transaction.commit();
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
        } else if (id == R.id.nav_list_groups) {
            clazz = ListGroupFragment.class;
        } else if (id == R.id.nav_addNewUserDevice) {
            clazz = AddNewUserDeviceFragment.class;
        } else if (id == R.id.nav_addModule) {
            clazz = AddModuleFragment.class;
        } else if (id == R.id.light_fab) {
            clazz = AddModuleFragment.class;
        } else if (id == R.id.nav_add_odroid) {
            clazz = AddNewSlaveFragment.class;
        } else {
            throw new IllegalArgumentException("Unknown id: " + id);
        }
        showFragmentByClass(clazz);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        showFragmentById(id);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}