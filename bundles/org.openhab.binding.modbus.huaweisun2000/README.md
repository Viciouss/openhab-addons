# Huawei Sun 2000(L) Inverter

Huawei Sun 2000 inverter family has support for Modbus communication via TCP to read some properties and state
registers.
See [this thread](https://community.openhab.org/t/reading-data-from-huawei-inverter-sun-2000-3ktl-10ktl-via-modbus-tcp-and-rtu/87670/101?page=6)
for more information on the topic.

## Supported Things

| Thing          | Description                        |
|----------------|------------------------------------|
| huawei-sun2000 | Huawei Sun 2000(L) Inverter Series |

In the modbus interface definitions document, there are a lot of inverter models under this device family, e.g. JP,
M0-M4, L0-L1, H0-H3, CN and many more. Chances are that if your inverter is from the Sun 2000 series, that this binding
can support it.

For the device to offer a modbus connection in the first place, you need a current firmware for the SDongleA (contact
Huawei directly for this or the person who installed your inverter) and you have to enable the modbus interface in the
administration part of the initial setup communication configuration. More information on that can be found in
the [Huawei Support Forum](https://forum.huawei.com/enterprise/en/modbus-tcp-guide/thread/789585-100027).

Attention: In the app, if you want to connect to the SDongleA for an update of the firmware, you need to be in your home
network. If you want to get to the configuration of your inverter instead, the device has a separate WiFi network that
you need to join before connecting to the initial setup interface. The credentials for this WiFi are on the device. 

## Configuration

You first need to set up a Modbus TCP bridge according to
the [Modbus documentation](https://www.openhab.org/addons/bindings/modbus/). Things in this extension will use the
selected bridge to connect to the device.

For the configuration of the TCP bridge, these values should be working for the Sun 2000:

* Port: 502
* Id: 1
* Discovery: false
* RTU Encoding: false
* Time Between Transactions: 1300
* Time Between Reconnections: 0
* Maximum Connection Tries: 1
* Reconnect Again After: 0
* Timeout for Establishing the Connection: 10000
