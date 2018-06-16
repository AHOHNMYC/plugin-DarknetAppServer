# plugin-DarknetAppServer
Broadcasts Freenet node presence using a pin of self-signed certificate (SHA 256, SSL from bouncy castle) on local network. Another device (mobile) associated with this node will be able to recognize this pin and establish secure connection.

The freenet node auto-synchs itself and downloads node-refs of darknet peers. The final decision to authorize a new peer is still manual for security reasons. 

Bonjous/zeroconf MDNS discovery similar to: https://github.com/freenet/plugin-MDNSDiscovery
