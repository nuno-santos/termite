# Termination of the Termite service
Disabling the Termite service will prevent any further simulation to take place: no more updates are
received from the console, no more broadcast events are triggered, and all active connections will be torn
down. To disable the Termite service, unbind the service:

```java
unbindService(mConnection);
```

In both the [PeerScanner](http://www.gsd.inesc-id.pt/~wiki/courses/cmu1516/lab04/Termite-WifiP2P-PeerScanner-20160329.tgz) and [MsgSender](http://www.gsd.inesc-id.pt/~wiki/courses/cmu1516/lab04/Termite-WifiP2P-MsgSender-20160329.tgz) demo applications covered in the tutorial lessons, this is done in the “WiFi-Off” button. The service can be re-enabled in the future.