/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import de.unipassau.isl.evs.ssh.app.R;

/**
 * MainFragment gives the user an overview over the most important functions.
 * Other functions can be accessed through the Navigation Drawer.
 *
 * @author Andreas Bucher
 */
public class MainFragment extends BoundFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout mLinearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_main,
                container, false);
        final AppMainActivity parent = (AppMainActivity) getActivity();

        ImageButton doorButton = (ImageButton) mLinearLayout.findViewById(R.id.doorButtonOpen);
        doorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(DoorFragment.class);
            }
        });
        ImageButton lightButton = (ImageButton) mLinearLayout.findViewById(R.id.lightButtonOn);
        lightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(LightFragment.class);
            }
        });
        ImageButton climateButton = (ImageButton) mLinearLayout.findViewById(R.id.climateButton);
        climateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(ClimateFragment.class);
            }
        });
        ImageButton holidayButton = (ImageButton) mLinearLayout.findViewById(R.id.holidayButton);
        holidayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(HolidayFragment.class);
            }
        });
        ImageButton statusButton = (ImageButton) mLinearLayout.findViewById(R.id.statusButton);
        statusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(StatusFragment.class);
            }
        });
        ImageButton usersButton = (ImageButton) mLinearLayout.findViewById(R.id.usersButton);
        usersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(ListGroupFragment.class);
            }
        });
        return mLinearLayout;
    }
}
