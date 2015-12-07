package de.unipassau.isl.evs.ssh.app.activity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import de.unipassau.isl.evs.ssh.app.AppContainer;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.AddGroupDialog;
import de.unipassau.isl.evs.ssh.app.dialogs.EditGroupDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppNotificationHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;

/**
 * As this Activity also displays information like whether the light is on or not, this Activity also
 * needs to messages concerning that information.
 *
 * @author bucher
 * @author Wolfgang Popp
 */
public class MainActivity extends BoundActivity
        implements NavigationView.OnNavigationItemSelectedListener, EditGroupDialog.EditGroupDialogListener, AddGroupDialog.AddGroupDialogListener {

    private static final String SAVED_LAST_ACTIVE_FRAGMENT = "de.unipassau.isl.evs.ssh.app.activity.SAVED_LAST_ACTIVE_FRAGMENT";
    private static final int uniqueID = 037735;
    private NavigationView navigationView = null;
    private Toolbar toolbar = null;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;

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

        //Initialise fragmentTransaction
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
            super.onBackPressed();
            getSupportFragmentManager().popBackStackImmediate();
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
        Class oldFragment = getCurrentFragment().getClass();
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

    @Override
    public void onContainerConnected(Container container) {
        final Fragment fragment = getCurrentFragment();
        if (fragment instanceof BoundFragment) {
            ((BoundFragment) fragment).onContainerConnected(container);
        }
        container.require(AppNotificationHandler.KEY).addNotificationObjects(notificationBuilder, notificationManager);
    }

    @Override
    public void onContainerDisconnected() {
        final Fragment fragment = getCurrentFragment();
        if (fragment instanceof BoundFragment) {
            ((BoundFragment) fragment).onContainerDisconnected();
        }
    }

    //TODO remove, just for testing!
    public void notificationButtonClicked(View view) {
        //Build notification
        notificationBuilder.setSmallIcon(R.drawable.ic_home_light);
        notificationBuilder.setContentTitle("Climate Warning!");
        notificationBuilder.setWhen(System.currentTimeMillis());
        notificationBuilder.setColor(2718207);
        notificationBuilder.setContentText("Please open a Window, Humidity too high.");

        //TODO does not work for fragments
        //If Notification is clicked send to this Page
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction("OpenClimateFragment");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);

        //TODO own method: sendNotification(uniqueID){}
        //Send notification out to Device
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(uniqueID, notificationBuilder.build());
    }

    @Override
    public void onDialogPositiveClick(AddGroupDialog dialog) {
        EditText editText = (EditText) dialog.getView().findViewById(R.id.add_group_dialog_group_name);
        String groupName = editText.getText().toString();
        Spinner spinner = (Spinner) dialog.getView().findViewById(R.id.add_group_dialog_spinner);
        String templateName = spinner.getSelectedItem().toString();

        Group newGroup = new Group(groupName, templateName);

        getComponent(AppUserConfigurationHandler.KEY).addGroup(newGroup);
    }

    @Override
    public void onDialogPositiveClick(EditGroupDialog dialog) {
        EditText editText = (EditText) dialog.getView().findViewById(R.id.edit_group_dialog_group_name);
        String newGroupName = editText.getText().toString();
        Spinner spinner = (Spinner) dialog.getView().findViewById(R.id.edit_group_dialog_spinner);
        String newTemplateName = (String) spinner.getSelectedItem();
        String oldGroupName = editText.getHint().toString();
        String oldTemplateName = editText.getText().toString();

        Group newGroup = new Group(newGroupName, newTemplateName);
        Group oldGroup = new Group(oldGroupName, oldTemplateName);

        if (!(newGroupName.equals(oldGroupName) && newTemplateName.equals(oldTemplateName))) {
            getComponent(AppUserConfigurationHandler.KEY).editGroup(newGroup, oldGroup);
        }
    }

}