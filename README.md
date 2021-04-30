OpenVPN for Headwind MDM

Description
------------
VPN service for Android devices managed by [Headwind MDM](https://h-mdm.com).

Settings
--------
The following application settings could be set up to manage OpenVPN remotely:

**vpn_name** - name of the VPN connection (required)

**vpn_config** - path to the .ovpn file containing an OpenVPN profile relative to the storage (required). Example: vpn_config=/client.ovpn  (the client.ovpn is located at the root directory of the device storage)

**connect** - set to 1 if the VPN connection should start just after loading the app. Available values: 1 and 0

**always_on** - set to 1 to set the VPN profile as "always-on", 0 to clear the always-on profile. Leave this setting empty to keep the always-on setting untouched.

**remove** - comma-separated list of VPN profiles which should be removed

**remove_all** - set to 1 to remove all other VPN profiles


Note: new settings are applied only if a new vpn_name is different from the previous one!

FAQ
-----
Search for "openvpn" or "vpn" tag in our QA database: https://qa.h-mdm.com

Controlling from external apps
------------------------------

There is the AIDL API for real controlling (see developing section). Due to high demand also the Activities `de.blinkt.openvpn.api.DisconnectVPN` and `de.blinkt.openvpn.api.ConnectVPN` exist. It uses `de.blinkt.openvpn.api.profileName` as extra for the name of the VPN profile.

Note to administrators
------------------------

You make your life and that of your users easier if you embed the certificates into the .ovpn file. You or the users can mail the .ovpn as a attachment to the phone and directly import and use it. Also downloading and importing the file works. The MIME Type should be application/x-openvpn-profile. 

Inline files are supported since OpenVPN 2.1rc1 and documented in the  [OpenVPN 2.3 man page](https://community.openvpn.net/openvpn/wiki/Openvpn23ManPage) (under INLINE FILE SUPPORT) 

(Using inline certifaces can also make your life on non-Android platforms easier since you only have one file.)

For example `ca mycafile.pem` becomes
```
  <ca>
  -----BEGIN CERTIFICATE-----
  MIIHPTCCBSWgAwIBAgIBADANBgkqhkiG9w0BAQQFADB5MRAwDgYDVQQKEwdSb290
  [...]
  -----END CERTIFICATE-----
  </ca>
```
Footnotes
-----------
Please note that OpenVPN used by this project is under GPLv2. 

Please read the doc/README before asking questions or starting development.
