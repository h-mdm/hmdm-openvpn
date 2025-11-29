OpenVPN for Headwind MDM
=============
![build status](https://github.com/schwabe/ics-openvpn/actions/workflows/build.yaml/badge.svg)

Note to other developers 
------------------------
This is a spare time project that I work on my own time and pick to work what I want.
You are free use the source code of this app with the conditions (GPL) that are attached to it
but do not expect any support or anything that I do not feel like to provide. 

The goal of this project is about providing an open-source OpenVPN app for Android. It is 
NOT about creating a library to be used in other projects.

If you build something on top of this is app you MUST publish your source code according to
the license of this app (GPL).

This not personal against other developers or your software and projects. The reason that I am not 
helping or spending time to really into issues that are not part of this app itself  is that this 
is just a spare time project of mine. The number of apps that use my code is quite large and
the majority of them violates the license of my app. People create new apps that do not publish 
their source code.

I am just not willing to be the unpaid support for other people trying to make money of my code 
for free anymore. That is just not something I want to do in my spare time, so I tend to close
these tickets quite quickly. 

When the project started, I tried to help people but most people were just taking advantage of me 
and promises about open sourcing their app when they were finished were not fulfilled and 
I was just often ghosted when asking for the promises. Some people had even the audacity to 
come then back a year or two later and demand help with critical bug fixes when their app broke
with some newer Android versions. Over the time, I simply lost confidence in people that were 
hesitant to share their source code and play with open cards.

That being said, I am happy to work together with people that are have are reproducing bugs in
this app that they observed in their open-sourced fork to improve this app. 

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
