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

package de.unipassau.isl.evs.ssh.core.database.dto;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.google.common.base.Strings;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.R;
import de.unipassau.isl.evs.ssh.core.sec.Permission;

/**
 * A DTO representing permissions. A permission has a name and may only be for a specific module.
 *
 * @author Leon Sell
 */
public class PermissionDTO implements Serializable {
    private de.unipassau.isl.evs.ssh.core.sec.Permission permission;
    private String moduleName;

    public PermissionDTO(de.unipassau.isl.evs.ssh.core.sec.Permission permission) {
        this.permission = permission;
    }

    public PermissionDTO(de.unipassau.isl.evs.ssh.core.sec.Permission permission, String moduleName) {
        this.permission = permission;
        this.moduleName = moduleName;
    }

    public de.unipassau.isl.evs.ssh.core.sec.Permission getPermission() {
        return permission;
    }

    public void setPermission(de.unipassau.isl.evs.ssh.core.sec.Permission permission) {
        this.permission = permission;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PermissionDTO that = (PermissionDTO) o;

        if (permission != that.permission) return false;
        return !(moduleName != null ? !moduleName.equals(that.moduleName) : that.moduleName != null);

    }

    @Override
    public int hashCode() {
        int result = permission != null ? permission.hashCode() : 0;
        result = 31 * result + (moduleName != null ? moduleName.hashCode() : 0);
        return result;
    }

    /**
     * Returns {@link Permission#toLocalizedString(Context)} if permission is binary ({@code moduleName == null}),
     * adds module name with localized preposition if permission is ternary.
     *
     * @return the localized Name of this PermissionDTO.
     */
    @NonNull
    public String toLocalizedString(Context context) {
        // @author Phil Werli
        Resources res = context.getResources();
        String forModule = null;
        String permissionString = permission.toLocalizedString(context);
        if (moduleName != null) {
            forModule = String.format(res.getString(R.string.forModule), permissionString, moduleName);
        }
        return forModule == null ? permissionString : forModule;
    }

    @Override
    public String toString() {
        return "PermissionDTO " + permission.toString() + (Strings.isNullOrEmpty(moduleName) ?
                "" : " (module " + moduleName + ")");
    }
}
