package plugins.darknetappserver;

import freenet.client.HighLevelSimpleClient;
import freenet.clients.http.ToadletContainer;
import freenet.config.InvalidConfigValueException;
import freenet.config.NodeNeedRestartException;
import freenet.config.SubConfig;
import freenet.l10n.BaseL10n;
import freenet.node.Node;
import freenet.pluginmanager.*;
import freenet.support.PooledExecutor;
import freenet.support.api.BooleanCallback;
import freenet.support.api.HTTPRequest;
import freenet.support.api.IntCallback;
import freenet.support.api.StringCallback;
import plugins.darknetappserver.darknetapp.DarknetAppServer;

/**
 * Created by Nitesh on 16/6/2018.
 */
public class DarknetAppServerPlugin implements FredPlugin, FredPluginHTTP ,FredPluginConfigurable, FredPluginThreadless, FredPluginRealVersioned {
    private static final long version = 0;
    private DarknetAppToadlet darknetAppToadlet;
    private DarknetAppServer darknetAppServer;
    private PluginRespirator pr;
    private MDNSDiscoveryRunner mdnsDiscoveryPlugin;

    @Override
    public long getRealVersion() {
        return version;
    }

    @Override
    public synchronized void terminate() {
        darknetAppServer.setEnabled(false);
        ToadletContainer container = pr.getToadletContainer();
        container.unregister(darknetAppToadlet);
    }

    @Override
    public void runPlugin(PluginRespirator pr) {
        Node node = pr.getNode();
        PooledExecutor executor = new PooledExecutor();
        //To Start DarknetAppServer
        SubConfig darknetAppConfig = pr.getSubConfig();
        darknetAppServer = new DarknetAppServer(darknetAppConfig, node, executor);
        darknetAppConfig.finishedInitialization();
        darknetAppServer.start();
        if (darknetAppServer.isEnabled()) {
            darknetAppServer.finishStart();
        }
        this.pr = pr;
        ToadletContainer toadletContainer = pr.getToadletContainer();
        HighLevelSimpleClient highLevelSimpleClient = pr.getHLSimpleClient();
        darknetAppToadlet = new DarknetAppToadlet(highLevelSimpleClient, pr);
        toadletContainer.register(darknetAppToadlet, "FProxyToadlet.categoryFriends", darknetAppToadlet.path(), true, "Authorize from app", "Authorize from app", false, darknetAppToadlet, null);
        mdnsDiscoveryPlugin = new MDNSDiscoveryRunner(pr);
        mdnsDiscoveryPlugin.start();
    }

    @Override
    public String getString(String key) {
        return "darknetApp";
    }

    @Override
    public void setLanguage(BaseL10n.LANGUAGE newLanguage) {

    }

    @Override
    public void setupConfig(SubConfig darknetAppConfig) {
        int configItemOrder = 0;
        darknetAppConfig.register("newDarknetPeersCount",0, configItemOrder++, true, true, "newPeersCount", "newPeersCount",
                new newDarknetPeersCallback(), false);
        darknetAppConfig.register("enabled", true, configItemOrder++, true, true, "enabled", "enabled",
                new  darknetAppEnabledCallback());
        darknetAppConfig.register("port", DarknetAppServer.DEFAULT_PORT, configItemOrder++, true, true, "port", "port",
                new darknetAppPortCallback(), false);
        darknetAppConfig.register("allowedHosts", "*", configItemOrder++, true, true, "allowedHosts", "allowedHosts",
                new  darknetAppAllowedHostsCallback());
        darknetAppConfig.register("bindTo", "0.0.0.0", configItemOrder++, true, true, "bindTo", "bindTo",
                new darknetAppBindtoCallback());
    }

    @Override
    public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
        return mdnsDiscoveryPlugin.handleHTTPGet(request);
    }

    @Override
    public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
        return mdnsDiscoveryPlugin.handleHTTPPost(request);
    }

    /**
     * This is necessary to be in the config for advanced users.
     * In case of an attack where our homeNode is bombarded with new references, the user can shift this to 0 and/or disable this server (using other config option)
     * Unnecessary changing of this value might cause instability in this app and/or losing temporary peer node references
     */
    private class newDarknetPeersCallback extends IntCallback {
        @Override
        public Integer get() {
            return darknetAppServer.getNewDarknetPeerCount();
        }

        @Override
        public void set(Integer val) throws InvalidConfigValueException, NodeNeedRestartException {
            darknetAppServer.setNewDarknetPeerCount(val);
        }
    }

    // Copied from SimpleToadletServer
    private class darknetAppBindtoCallback extends StringCallback {
        @Override
        public String get() {
            return darknetAppServer.getBindTo();
        }

        @Override
        public void set(String bindTo) throws InvalidConfigValueException {
            darknetAppServer.setBindTo(bindTo);
        }
    }

    // Copied from SimpleToadletServer
    private class darknetAppAllowedHostsCallback extends StringCallback  {
        @Override
        public String get() {
            return darknetAppServer.getAllowedHosts();
        }

        @Override
        public void set(String allowedHosts) throws InvalidConfigValueException {
            darknetAppServer.setAllowedHosts(allowedHosts);
        }
    }


    private class darknetAppPortCallback extends IntCallback  {
        @Override
        public Integer get() {
            return darknetAppServer.getPort();
        }

        @Override
        public void set(Integer newPort) throws NodeNeedRestartException {
            darknetAppServer.setPort(newPort);
            mdnsDiscoveryPlugin.restart();
        }
    }

    private class darknetAppEnabledCallback extends BooleanCallback {
        @Override
        public Boolean get() {
            return darknetAppServer.isEnabled();
        }
        @Override
        public void set(Boolean val) throws InvalidConfigValueException {
            darknetAppServer.setEnabled(val);
        }
    }

}
