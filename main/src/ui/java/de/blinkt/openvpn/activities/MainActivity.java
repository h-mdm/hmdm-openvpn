/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.viewpager.widget.ViewPager;

import com.hmdm.HeadwindMDM;
import com.hmdm.MDMService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.fragments.AboutFragment;
import de.blinkt.openvpn.fragments.FaqFragment;
import de.blinkt.openvpn.fragments.GeneralSettings;
import de.blinkt.openvpn.fragments.GraphFragment;
import de.blinkt.openvpn.fragments.LogFragment;
import de.blinkt.openvpn.fragments.SendDumpFragment;
import de.blinkt.openvpn.fragments.Utils;
import de.blinkt.openvpn.fragments.VPNProfileList;
import de.blinkt.openvpn.views.ScreenSlidePagerAdapter;


public class MainActivity extends BaseActivity implements HeadwindMDM.EventHandler {

    private static final String FEATURE_TELEVISION = "android.hardware.type.television";
    private static final String FEATURE_LEANBACK = "android.software.leanback";
    //private TabLayout mTabs;
    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;

    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);


        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), this);

        /* Toolbar and slider should have the same elevation */
        disableToolbarElevation();


        mPagerAdapter.addTab(R.string.vpn_list_title, VPNProfileList.class);
        mPagerAdapter.addTab(R.string.graph, GraphFragment.class);

        mPagerAdapter.addTab(R.string.generalsettings, GeneralSettings.class);
        mPagerAdapter.addTab(R.string.faq, FaqFragment.class);

        if (SendDumpFragment.getLastestDump(this) != null) {
            mPagerAdapter.addTab(R.string.crashdump, SendDumpFragment.class);
        }


        if (isDirectToTV())
            mPagerAdapter.addTab(R.string.openvpn_log, LogFragment.class);

        mPagerAdapter.addTab(R.string.about, AboutFragment.class);
        mPager.setAdapter(mPagerAdapter);

        //mTabs =  findViewById(R.id.sliding_tabs);
        //mTabs.setViewPager(mPager);

        HeadwindMDM headwindMDM = HeadwindMDM.getInstance();
        if (!headwindMDM.isConnected()) {
            headwindMDM.connect(this, this);
        }
    }

    private boolean isDirectToTV() {
        return (getPackageManager().hasSystemFeature(FEATURE_TELEVISION)
                || getPackageManager().hasSystemFeature(FEATURE_LEANBACK));
    }


    private void disableToolbarElevation() {
        ActionBar toolbar = getSupportActionBar();
        toolbar.setElevation(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent() != null) {
            String page = getIntent().getStringExtra("PAGE");
            if ("graph".equals(page)) {
                mPager.setCurrentItem(1);
            }
            setIntent(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.show_log) {
            Intent showLog = new Intent(this, LogWindow.class);
            startActivity(showLog);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        System.out.println(data);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HeadwindMDM headwindMDM = HeadwindMDM.getInstance();
        if (headwindMDM.isConnected()) {
            headwindMDM.disconnect(this);
        }
    }

    @Override
    public void onHeadwindMDMConnected() {
        MDMService.Log.i(null, "Connected to Headwind MDM");
        loadConfigFromMdm();
    }

    @Override
    public void onHeadwindMDMDisconnected() {
    }

    @Override
    public void onHeadwindMDMConfigChanged() {
        loadConfigFromMdm();
    }

    private void loadConfigFromMdm() {
        String vpnName = MDMService.Preferences.get("vpn_name", null);
        String vpnConfig = MDMService.Preferences.get("vpn_config", null);
        String vpnNamesToRemove = MDMService.Preferences.get("remove", "").trim();
        boolean removeOtherVpns = "1".equals(MDMService.Preferences.get("remove_all", "0"));
        boolean connect = "1".equals(MDMService.Preferences.get("connect", "0"));
        String alwaysOnString = MDMService.Preferences.get("always_on", null);
        Boolean alwaysOn = alwaysOnString == null ? null : "1".equals(alwaysOnString);

        if (vpnName == null || vpnConfig == null) {
            // Nothing to do - the admin should configure name and configuration file
            MDMService.Log.v(null, "VPN configuration not set");
            return;
        }

        ProfileManager profileManager = ProfileManager.getInstance(MainActivity.this);
        VpnProfile existingProfile = profileManager.getProfileByName(vpnName);
        if (existingProfile != null) {
            // Nothing to do - the admin should change the VPN name to apply changes
            MDMService.Log.v(null, "VPN '" + vpnName + "' already exists");
            return;
            // TEST ONLY: reconnect each time
//            profileManager.removeProfile(MainActivity.this, existingProfile);
        }

        if (!vpnNamesToRemove.equals("")) {
            String[] vpns = vpnNamesToRemove.split(",");
            for (String vpn : vpns) {
                vpn = vpn.trim();
                VpnProfile vpnProfile = profileManager.getProfileByName(vpn);
                if (vpnProfile != null) {
                    profileManager.removeProfile(this, vpnProfile);
                }
            }
        }

        if (removeOtherVpns) {
            Collection<VpnProfile> vpns = profileManager.getProfiles();
            for (VpnProfile vpnProfile : vpns) {
                if (vpnProfile.getName().equals(vpnName)) {
                    continue;
                }
                profileManager.removeProfile(this, vpnProfile);
            }
        }

        File file = new File(Environment.getExternalStorageDirectory(), vpnConfig);
        if (!file.exists()) {
            MDMService.Log.w(null, "VPN config file doesn't exist: " + vpnConfig);
            return;
        }

        loadVPNFromFile(file, vpnName, connect, alwaysOn);
    }

    private void loadVPNFromFile(final File file, final String profileName, final boolean connect, final Boolean alwaysOn) {
        final ConfigConverter configConverter = new ConfigConverter();
        new AsyncTask<Void, Void, Void>() {

            VpnProfile vpnProfile = null;
            String embeddedPwFile = null;

            @Override
            protected Void doInBackground(Void... voids) {
                FileInputStream fileInputStream;
                try {
                    fileInputStream = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    // We shouldn't be here because we have checked the file existence
                    e.printStackTrace();
                    return null;
                }

                configConverter.doImport(fileInputStream);
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                vpnProfile = configConverter.getResult();
                vpnProfile.mName = profileName;
                embeddedPwFile = configConverter.getEmbeddedPwFile();

                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                if (vpnProfile == null) {
                    MDMService.Log.w(null, "Failed to import VPN profile from " + file.getPath());
                    return;
                }
                MDMService.Log.v(null, "VPN profile " + vpnProfile.getName() + " imported");
                saveVpnProfile();
                mPagerAdapter.notifyDataSetChanged();
                if (connect) {
                    connectVpn();
                }
                if (alwaysOn != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (alwaysOn) {
                        ProfileManager.setAlwaysOnVPN(MainActivity.this, vpnProfile);
                        boolean result = Utils.setAlwaysOnVpn(MainActivity.this, true);
                        MDMService.Log.i(null, result ? "VPN profile " + vpnProfile.getName() + " is always on" :
                                "Failed to set VPN profile " + vpnProfile.getName() + " as always-on");
                    } else {
                        ProfileManager.setAlwaysOnVPN(MainActivity.this, null);
                        boolean result = Utils.setAlwaysOnVpn(MainActivity.this, false);
                        MDMService.Log.i(null, result ? "Always-on VPN cleared" :
                                "Failed to clear always-on VPN");
                    }
                }
            }

            private void saveVpnProfile() {
                ProfileManager profileManager = ProfileManager.getInstance(MainActivity.this);
                if (embeddedPwFile != null && !TextUtils.isEmpty(embeddedPwFile)) {
                    ConfigParser.useEmbbedUserAuth(vpnProfile, embeddedPwFile);
                }
                profileManager.addProfile(vpnProfile);
                profileManager.saveProfile(MainActivity.this, vpnProfile);
                profileManager.saveProfileList(MainActivity.this);
            }

            private void connectVpn() {
                Intent intent = new Intent(MainActivity.this, LaunchVPN.class);
                intent.putExtra(LaunchVPN.EXTRA_KEY, vpnProfile.getUUID().toString());
                intent.setAction(Intent.ACTION_MAIN);
                startActivity(intent);
            }
        }.execute();
    }

}
