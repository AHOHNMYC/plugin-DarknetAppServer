# plugin-DarknetAppServer
Broadcasts Freenet node presence using a pin of self-signed certificate (SHA 256, SSL from bouncy castle) on local network. Another device (mobile) associated with this node will be able to recognize this pin and establish secure connection.

The freenet node auto-synchs itself and downloads node-refs of darknet peers. The final decision to authorize a new peer is still manual for security reasons. 

Bundled my commits from https://github.com/NiteshBharadwaj/plugin-MDNSDiscovery-official and https://github.com/NiteshBharadwaj/fred-staging into a single plugin for ease of use. Code is 5 years old but made sure it's working with the latest build 1481. Needs a little refractor and documentation.

TODO: Update the android app and test full workflow

