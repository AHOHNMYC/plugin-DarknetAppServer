package plugins.darknetappserver;

import freenet.pluginmanager.PluginHTTPException;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.api.HTTPRequest;

/**
 * Created by Nitesh on 16/6/2018.
 */
public class MDNSDiscoveryRunner implements Runnable {
    private PluginRespirator pluginRespirator;
    private MDNSDiscovery mdnsDiscovery;
    private Thread mdnsDiscoveryThread;
    public MDNSDiscoveryRunner(PluginRespirator pluginRespirator) {
        this.pluginRespirator = pluginRespirator;
    }

    @Override
    public void run() {
        mdnsDiscovery = new MDNSDiscovery();
        mdnsDiscovery.runPlugin(pluginRespirator);
    }
    public void start() {
        mdnsDiscoveryThread =  new Thread(this);
        mdnsDiscoveryThread.start();
    }

    public void terminate() {
        if (mdnsDiscoveryThread!=null) {
            if (mdnsDiscovery!=null) mdnsDiscovery.terminate();
            mdnsDiscoveryThread.interrupt();
            mdnsDiscoveryThread.stop();
        }
    }

    public void restart() {
        terminate();
        start();
    }

    public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
        return mdnsDiscovery.handleHTTPGet(request);
    }

    public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
        return mdnsDiscovery.handleHTTPPost(request);
    }
}
