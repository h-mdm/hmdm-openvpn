/*
 * Copyright (c) 2012-2018 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import java.util.Collection;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;

public class DefaultVPNListPreference extends ListPreference {
    public DefaultVPNListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setVPNs(context);
    }

    private void setVPNs(Context c) {
        ProfileManager pm = ProfileManager.getInstance(c);
        Collection<VpnProfile> profiles = pm.getProfiles();
        CharSequence[] entries = new CharSequence[profiles.size() + 1];
        CharSequence[] entryValues = new CharSequence[profiles.size() + 1];;

        entries[0] = c.getString(R.string.novpn_selected);
        entryValues[0] = "";

        int i=1;
        for (VpnProfile p: profiles)
        {
            entries[i]=p.getName();
            entryValues[i]=p.getUUIDString();
            i++;
        }

        setEntries(entries);
        setEntryValues(entryValues);
    }
}
