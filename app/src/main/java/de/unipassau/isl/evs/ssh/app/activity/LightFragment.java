package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppLightHandler;
import de.unipassau.isl.evs.ssh.app.handler.HandlerUpdateListener;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * This fragment allows to display information contained in light messages which are received from the IncomingDispatcher.
 * Furthermore it generates a light messages as instructed by the UI and passes it to the OutgoingRouter.
 *
 * @author Phil
 */
public class LightFragment extends Fragment {

    private HandlerUpdateListener handlerUpdateListener;


    public LightFragment() {
        // Required empty public constructor
    }


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_light, container, false);
       ListView list = (ListView) root.findViewById(R.id.lightButtonContainer);


       final Map<Module, Boolean> lightModulesStatus = getLightModules();

       final ArrayList<Module> lightModules = Lists.newArrayList(lightModulesStatus.keySet());
       Collections.sort(lightModules, new Comparator<Module>() {
           @Override
           public int compare(Module lhs, Module rhs) {
               if(lhs.getName() == null) {
                   return  rhs.getName() == null ? 0: 1;
               }
               if(rhs.getName() == null) {
                   return  -1;
               }
               return lhs.getName().compareTo(rhs.getName());
           }
       });

       list.setAdapter(new BaseAdapter() {
           @Override
           public int getCount() {
               return lightModules.size();
           }

           @Override
           public Module getItem(int position) {
               return lightModules.get(position);
           }

           @Override
           public long getItemId(int position) {
               return getItem(position).getName().hashCode(); // TODO nullcheck
           }

           @Override
           public boolean hasStableIds() {
               return true;
           }

           @Override
           public View getView(int position, View convertView, ViewGroup parent) {
               //create linearlayout as defined in lightbutton.xml file
               LinearLayout lightButtonLayout;
               if(convertView == null) {
                   lightButtonLayout = (LinearLayout) inflater.inflate(R.layout.lightbutton, null, false);
               } else {
                   lightButtonLayout = (LinearLayout) convertView;
               }

               final Module m = getItem(position);

               TextView textView = (TextView) lightButtonLayout.findViewById(R.id.lightButtonTextView);
               Button button = (Button) lightButtonLayout.findViewById(R.id.lightButtonButton);
               button.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View v) {
                       ((MainActivity) getActivity()).getContainer().require(AppLightHandler.KEY).toggleLight(m);
                   }
               });
               // set up TextView
               char[] name = getName(m);
               textView.setText(name, 0, name.length);

               // set up ImageView and button
               ImageView imageViewOn = (ImageView) lightButtonLayout.findViewById(R.id.lightButtonImageViewOn);
               ImageView imageViewOff = (ImageView) lightButtonLayout.findViewById(R.id.lightButtonImageViewOff);
               if (lightModulesStatus.get(m)) {
                   imageViewOff.setVisibility(View.GONE);
                   imageViewOn.setVisibility(View.VISIBLE);
                   button.setText(getResources().getString(R.string.string_turn_off));
                   button.setBackgroundColor(getResources().getColor(R.color.button_red));
               } else {
                   imageViewOn.setVisibility(View.GONE);
                   imageViewOff.setVisibility(View.VISIBLE);
                   button.setText(getResources().getString(R.string.string_turn_on));
                   button.setBackgroundColor(getResources().getColor(R.color.button_green));
               }

               return lightButtonLayout;
           }
       });

       return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handlerUpdateListener = new LightHandlerUpdateListener();
        ((MainActivity) getActivity()).getContainer().require(AppLightHandler.KEY).removeHandlerUpdateListener(
                handlerUpdateListener);
    }

    private char[] getName(Module m) {
        return m.getName().toCharArray();
    }

    private Map<Module, Boolean> getLightModules() {
        return ((MainActivity) getActivity()).getContainer().require(AppLightHandler.KEY).getAllLightModuleStates();
    }

    private class LightHandlerUpdateListener implements HandlerUpdateListener {

        @Override
        public void updatePerformed() {
            AppLightHandler lightHandler = ((MainActivity) getActivity()).getContainer().require(AppLightHandler.KEY);
            for (Module m : getLightModules().keySet()) {
                lightHandler.isModuleOn(m);
            }
        }
    }
}