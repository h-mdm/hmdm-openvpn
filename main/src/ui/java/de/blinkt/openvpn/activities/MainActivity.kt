/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.activities

import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
import com.hmdm.HeadwindMDM
import com.hmdm.MDMService
import de.blinkt.openvpn.LaunchVPN
import de.blinkt.openvpn.R
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.fragments.AboutFragment
import de.blinkt.openvpn.fragments.FaqFragment
import de.blinkt.openvpn.fragments.GeneralSettings
import de.blinkt.openvpn.fragments.GraphFragment
import de.blinkt.openvpn.fragments.ImportRemoteConfig.Companion.newInstance
import de.blinkt.openvpn.fragments.LogFragment
import de.blinkt.openvpn.fragments.SendDumpFragment
import de.blinkt.openvpn.fragments.Utils
import de.blinkt.openvpn.fragments.VPNProfileList
import de.blinkt.openvpn.views.ScreenSlidePagerAdapter
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException


class MainActivity : BaseActivity(), HeadwindMDM.EventHandler {
    private lateinit var mPager: ViewPager
    private lateinit var mPagerAdapter: ScreenSlidePagerAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)


        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager)
        mPagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager, this)

        /* Toolbar and slider should have the same elevation */disableToolbarElevation()
        mPagerAdapter.addTab(R.string.vpn_list_title, VPNProfileList::class.java)
        mPagerAdapter.addTab(R.string.graph, GraphFragment::class.java)
        mPagerAdapter.addTab(R.string.generalsettings, GeneralSettings::class.java)
        mPagerAdapter.addTab(R.string.faq, FaqFragment::class.java)
        if (SendDumpFragment.getLastestDump(this) != null) {
            mPagerAdapter.addTab(R.string.crashdump, SendDumpFragment::class.java)
        }
        if (isAndroidTV)
            mPagerAdapter.addTab(R.string.openvpn_log, LogFragment::class.java)
        mPagerAdapter.addTab(R.string.about, AboutFragment::class.java)
        mPager.setAdapter(mPagerAdapter)

        val headwindMDM = HeadwindMDM.getInstance()
        if (!headwindMDM.isConnected) {
            headwindMDM.connect(this, this)
        }
    }

    private fun disableToolbarElevation() {
        supportActionBar?.elevation = 0f
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        if (intent != null) {
            val action = intent.action
            if (Intent.ACTION_VIEW == action) {
                val uri = intent.data
                uri?.let { checkUriForProfileImport(it) }
            }
            val page = intent.getStringExtra("PAGE")
            if ("graph" == page) {
                mPager.currentItem = 1
            }
            setIntent(null)
        }
    }

    private fun checkUriForProfileImport(uri: Uri) {
        if ("openvpn" == uri.scheme && "import-profile" == uri.host) {
            var realUrl = uri.encodedPath + "?" + uri.encodedQuery
            if (!realUrl.startsWith("/https://")) {
                Toast.makeText(
                    this,
                    "Cannot use openvpn://import-profile/ URL that does not use https://",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            realUrl = realUrl.substring(1)
            startOpenVPNUrlImport(realUrl)
        }
    }

    private fun startOpenVPNUrlImport(url: String) {
        val asImportFrag = newInstance(url)
        asImportFrag.show(supportFragmentManager, "dialog")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.show_log) {
            val showLog = Intent(this, LogWindow::class.java)
            startActivity(showLog)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        val headwindMDM = HeadwindMDM.getInstance()
        if (headwindMDM.isConnected) {
            headwindMDM.disconnect(this)
        }
    }

    override fun onHeadwindMDMConnected() {
        MDMService.Log.i(null, "Connected to Headwind MDM")
        loadConfigFromMdm()
    }

    override fun onHeadwindMDMDisconnected() {}

    override fun onHeadwindMDMConfigChanged() {
        loadConfigFromMdm()
    }

    private fun loadConfigFromMdm() {
        val vpnName = MDMService.Preferences.get("vpn_name", null)
        val vpnConfig = MDMService.Preferences.get("vpn_config", null)
        val vpnNamesToRemove = MDMService.Preferences.get("remove", "").trim { it <= ' ' }
        val removeOtherVpns = "1" == MDMService.Preferences.get("remove_all", "0")
        val connect = "1" == MDMService.Preferences.get("connect", "0")
        val alwaysOnString = MDMService.Preferences.get("always_on", null)
        val alwaysOn = if (alwaysOnString == null) null else "1" == alwaysOnString
        if (vpnName == null || vpnConfig == null) {
            // Nothing to do - the admin should configure name and configuration file
            MDMService.Log.v(null, "VPN configuration not set")
            return
        }
        val profileManager = ProfileManager.getInstance(this@MainActivity)
        val existingProfile = profileManager.getProfileByName(vpnName)
        if (existingProfile != null) {
            // Nothing to do - the admin should change the VPN name to apply changes
            MDMService.Log.v(null, "VPN '$vpnName' already exists")
            return
            // TEST ONLY: reconnect each time
//            profileManager.removeProfile(MainActivity.this, existingProfile);
        }
        if (vpnNamesToRemove != "") {
            val vpns = vpnNamesToRemove.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            for (vpn in vpns) {
                val vpnTrim = vpn.trim { it <= ' ' }
                val vpnProfile = profileManager.getProfileByName(vpnTrim)
                if (vpnProfile != null) {
                    profileManager.removeProfile(this, vpnProfile)
                }
            }
        }
        if (removeOtherVpns) {
            val vpns = profileManager.profiles
            for (vpnProfile in vpns) {
                if (vpnProfile.name == vpnName) {
                    continue
                }
                profileManager.removeProfile(this, vpnProfile)
            }
        }
        val file = File(Environment.getExternalStorageDirectory(), vpnConfig)
        if (!file.exists()) {
            MDMService.Log.w(null, "VPN config file doesn't exist: $vpnConfig")
            return
        }
        loadVPNFromFile(file, vpnName, connect, alwaysOn)
    }

    private fun loadVPNFromFile(
        file: File,
        profileName: String,
        connect: Boolean,
        alwaysOn: Boolean?
    ) {
        val configConverter = ConfigConverter()
        LoadVPNFromFileClass(
            file,
            profileName,
            connect,
            alwaysOn,
            configConverter,
            mPagerAdapter,
            this
        )
        .execute()
    }

    class LoadVPNFromFileClass(
        val file: File,
        val profileName: String,
        val connect: Boolean,
        val alwaysOn: Boolean?,
        val configConverter: ConfigConverter,
        val mPagerAdapter: ScreenSlidePagerAdapter,
        val mainActivity: MainActivity,
        var vpnProfile: VpnProfile? = null,
        var embeddedPwFile: String? = null,
    ) : AsyncTask<Void?, Void?, Void?>() {

        protected override fun doInBackground(vararg voids: Void?): Nothing? {
            val fileInputStream: FileInputStream
            fileInputStream = try {
                FileInputStream(file)
            } catch (e: FileNotFoundException) {
                // We shouldn't be here because we have checked the file existence
                e.printStackTrace()
                return null
            }
            configConverter.doImport(fileInputStream)
            try {
                fileInputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            vpnProfile = configConverter.getResult()
            vpnProfile!!.mName = profileName
            embeddedPwFile = configConverter.getEmbeddedPwFile()
            return null
        }

        protected override fun onPostExecute(v: Void?) {
            if (vpnProfile == null) {
                MDMService.Log.w(null, "Failed to import VPN profile from " + file.path)
                return
            }
            MDMService.Log.v(null, "VPN profile " + vpnProfile!!.name + " imported")
            saveVpnProfile()
            mPagerAdapter.notifyDataSetChanged()
            if (connect) {
                connectVpn()
            }
            if (alwaysOn != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (alwaysOn) {
                    ProfileManager.setAlwaysOnVPN(mainActivity, vpnProfile)
                    val result: Boolean = Utils.setAlwaysOnVpn(mainActivity, true)
                    MDMService.Log.i(
                        null,
                        if (result) "VPN profile " + vpnProfile!!.name + " is always on" else "Failed to set VPN profile " + vpnProfile!!.name + " as always-on"
                    )
                } else {
                    ProfileManager.setAlwaysOnVPN(mainActivity, null)
                    val result: Boolean = Utils.setAlwaysOnVpn(mainActivity, false)
                    MDMService.Log.i(
                        null,
                        if (result) "Always-on VPN cleared" else "Failed to clear always-on VPN"
                    )
                }
            }
        }

        private fun saveVpnProfile() {
            val profileManager = ProfileManager.getInstance(mainActivity)
            if (embeddedPwFile != null && !TextUtils.isEmpty(embeddedPwFile)) {
                ConfigParser.useEmbbedUserAuth(vpnProfile, embeddedPwFile)
            }
            profileManager.addProfile(vpnProfile)
            ProfileManager.saveProfile(mainActivity, vpnProfile)
            profileManager.saveProfileList(mainActivity)
        }

        private fun connectVpn() {
            val intent = Intent(
                mainActivity,
                LaunchVPN::class.java
            )
            intent.putExtra(LaunchVPN.EXTRA_KEY, vpnProfile!!.uuid.toString())
            intent.action = Intent.ACTION_MAIN
            mainActivity.startActivity(intent)
        }
    }
}