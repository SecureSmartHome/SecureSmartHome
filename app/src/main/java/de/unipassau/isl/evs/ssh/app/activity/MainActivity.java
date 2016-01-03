package de.unipassau.isl.evs.ssh.app.activity;

import android.app.NotificationManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import de.unipassau.isl.evs.ssh.app.AppContainer;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppNotificationHandler;
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
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String SAVED_LAST_ACTIVE_FRAGMENT = MainActivity.class.getName() + ".SAVED_LAST_ACTIVE_FRAGMENT";
    private NavigationView navigationView = null;
    private Toolbar toolbar = null;
    private LinearLayout overlayDisconnected;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;

    private boolean fragmentInitialized = false;
    private Bundle savedInstanceState;

    private ClientConnectionListener connectionListener = new ClientConnectionListener() {
        @Override
        public void onMasterFound() {

        }

        @Override
        public void onClientConnecting() {

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
                    showDisconnectedOverlay();
                }
            });
        }

        @Override
        public void onClientRejected(String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showConnectionOverlay("Connection refused. Maybe the system has been reset?");
                }
            });
        }
    };

    private void showConnectionOverlay(String text) {
        overlayDisconnected.setVisibility(View.VISIBLE);
        TextView textView = (TextView) overlayDisconnected.findViewById(R.id.overlay_text);
        textView.setText(text);
    }

    private void showDisconnectedOverlay(){
        showConnectionOverlay("No Connection to Secure Smart Home Server.");
    }

    public MainActivity() {
        super(AppContainer.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(this.savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        setContentView(R.layout.activity_main);

        //Set the fragment initially
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        overlayDisconnected = (LinearLayout) findViewById(R.id.overlay_disconnected);

        //Initialise Notifications
        notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setAutoCancel(true);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //Initialise NavigationDrawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (!fragmentInitialized && getContainer() != null) {
            fragmentInitialized = true;
            showFragmentByClass(getInitialFragment());
        }
    }

    @Override
    public void onContainerConnected(Container container) {
        final Fragment fragment = getCurrentFragment();
        if (fragment instanceof BoundFragment) {
            ((BoundFragment) fragment).onContainerConnected(container);
        }
        container.require(AppNotificationHandler.KEY).addNotificationObjects(notificationBuilder, notificationManager);

        if (!fragmentInitialized) {
            fragmentInitialized = true;
            showFragmentByClass(getInitialFragment());
        }

        Client client = container.require(Client.KEY);
        client.addListener(connectionListener);

        if (container.require(NamingManager.KEY).isMasterKnown() && !client.isConnectionEstablished()) {
            showDisconnectedOverlay();
        }
    }

    private Class getInitialFragment() {
        if (!requireComponent(NamingManager.KEY).isMasterIDKnown()) {
            return WelcomeScreenFragment.class;
        }
        if (getIntent() != null && getIntent().getAction() != null) {
            switch (getIntent().getAction()) {
                case "ClimateFragment":
                    return ClimateFragment.class;
                case "StatusFragment":
                    return StatusFragment.class;
                case "LightFragment":
                    return LightFragment.class;
                case "DoorFragment":
                    return DoorFragment.class;
            }
        }
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_LAST_ACTIVE_FRAGMENT)) {
            try {
                return Class.forName(savedInstanceState.getString(SAVED_LAST_ACTIVE_FRAGMENT));
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "Could not load Fragment from saved instance state", e);
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

        requireComponent(Client.KEY).removeListener(connectionListener);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_LAST_ACTIVE_FRAGMENT, getCurrentFragment().getClass().getName());
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
        Fragment fragment = null;
        try {
            fragment = (Fragment) clazz.newInstance();
            if (bundle != null) {
                fragment.setArguments(bundle);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        if (fragment != null) {
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
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