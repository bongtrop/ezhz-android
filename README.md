# EZHZ Android

This is an android app for rooted device to help you to perform penetration testing on the applications installed on your device.

## Features

- [x] Forward network traffic of any application to interceptor proxy (inspired by [proxydroid](https://github.com/madeye/proxydroid))
- [x] Manage and install any version of Frida server on your device (inspired by [FridaLoader](https://github.com/dineshshetty/FridaLoader))
- [ ] Inspect and modify network encrypted or unencrypted shared preference data of any application

## Installation

You can download the latest release APK from [here](https://github.com/bongtrop/ezhz-android/releases). In order to install it, just install by `adb` command as follows:

```bash
adb install -r ezhz-android-vX.X.apk # replace X.X with the version number
```

## Build

You can build the APK by yourself by cloning this repository and build it with Android Studio.

## Usage

### Intercept network traffic

1. Start the app and grant root access
2. Go to `Proxy` menu
3. Set the proxy address, port, proxy type, and dns of the interceptor proxy in `Proxy Settings` section.
4. Set the applications on `Apps Proxy` that you want to intercept its network traffic or check `Global Proxy` for all applications in `Target Settings` section.
5. Start the proxy by clicking on `Proxy Switch` switch.

### Install Frida server

1. Start the app and grant root access
2. Go to `Frida` menu
3. Select the Frida server version that you want to install
4. Click on `Install` button
5. Wait until the installation is finished
6. Start the Frida server by clicking on `Execute` button

## Credits

- Thank you for amazing ideas from [proxydroid](https://github.com/madeye/proxydroid) that using `iptables` and transparent proxy to forward network traffic to any interceptor tool. I upgrade the functionality of `proxydroid` to support QUIC protocol by changing the proxy client from `redsocks` to `gost v3` and relay UDP traffic to it. Additionally, I have a problem about finding target applications in application selector activity, so I have implemented new application selector by adding search functionality to it.
- Thank you for nice ideas from [FridaLoader](https://github.com/dineshshetty/FridaLoader) that it will download the latest version of `frida-server` from Github and run it by root user. I upgrade the functionality of `FridaLoader` to support multiple versions of `frida-server` and make it to be foreground service in order to be easier to manage.

