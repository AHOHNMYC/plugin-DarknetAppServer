package plugins.darknetappserver;

import freenet.client.HighLevelSimpleClient;
import freenet.clients.http.*;
import freenet.io.comm.PeerParseException;
import freenet.io.comm.ReferenceSignatureVerificationException;
import freenet.node.DarknetPeerNode;
import freenet.node.FSParseException;
import freenet.node.PeerNode;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.Fields;
import freenet.support.HTMLNode;
import freenet.support.Logger;
import freenet.support.SimpleFieldSet;
import freenet.support.api.HTTPRequest;
import plugins.darknetappserver.darknetapp.DarknetAppServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static freenet.clients.http.FProxyToadlet.l10n;

/**
 * Created by Nitesh on 16/6/2018.
 */
public class DarknetAppToadlet extends Toadlet implements LinkEnabledCallback {
    private PluginRespirator pr;
    private static final String PATH = "/addFriendsFromApp";
    private static int newTempDarknetRefs = 0;

    public DarknetAppToadlet(HighLevelSimpleClient highLevelSimpleClient, PluginRespirator pr) {
        super(highLevelSimpleClient);
        this.pr = pr;
    }

    @Override
    public boolean isEnabled(ToadletContext ctx) {
        return true;
    }

    @Override
    public void handleMethodGET(URI uri, HTTPRequest request, ToadletContext ctx) throws ToadletContextClosedException, IOException, RedirectException {
        PageNode page = pr.getPageMaker().getPageNode("Darknet App Server", false, null);
        HTMLNode pageNode = page.outer;
        HTMLNode contentNode = page.content;

        //if (DarknetAppServer.newDarknetPeersCount>0)
            drawNewDarknetPeersAuthBox(contentNode, ctx, false, PATH);
        this.writeHTMLReply(ctx, 200, "OK", pageNode.generate());
    }


    public void handleMethodPOST(URI uri, HTTPRequest request, ToadletContext ctx)
            throws ToadletContextClosedException, IOException {
        /**
         * Handle authorized/ rejected newly received references (from DarknetAppServer)
         */
        if (request.isPartSet("addNew")) {
            Properties prop = new Properties();
            try {
                File file =  new File(DarknetAppServer.filename);
                prop.load(new FileInputStream(file));
            } catch (FileNotFoundException ex) {
                Logger.error(ctx, "Darknet App New Peers File Not Found",ex);
            } catch (IOException ex) {
            }
            int addedNodes = 0;
            Map<ConnectionsToadlet.PeerAdditionReturnCodes,Integer> results=new HashMap<ConnectionsToadlet.PeerAdditionReturnCodes, Integer>();
            for(int i=1;i<=newTempDarknetRefs;i++) {
                String auth = request.getPartAsStringFailsafe("auth"+i, 250).trim();
                if (auth.equals("reject")) continue;
                String privateComment = request.getPartAsStringFailsafe("peerPrivateNote"+i, 250).trim();

                String trustS = request.getPartAsStringFailsafe("trust"+i, 10);
                DarknetPeerNode.FRIEND_TRUST trust = null;
                if(trustS != null && !trustS.equals(""))
                    trust = DarknetPeerNode.FRIEND_TRUST.valueOf(trustS);
                String visibilityS = request.getPartAsStringFailsafe("visibility"+i, 10);
                DarknetPeerNode.FRIEND_VISIBILITY visibility = null;
                if(visibilityS != null && !visibilityS.equals(""))
                    visibility = DarknetPeerNode.FRIEND_VISIBILITY.valueOf(visibilityS);

                if(trust == null) {
                    // FIXME: Layering violation. Ideally DarknetPeerNode would do this check.
                    this.sendErrorPage(ctx, 200, l10n("noTrustLevelAddingFriendTitle"), l10n("noTrustLevelAddingFriend"));
                    return;
                }

                if(visibility == null) {
                    // FIXME: Layering violation. Ideally DarknetPeerNode would do this check.
                    this.sendErrorPage(ctx, 200, l10n("noVisibilityLevelAddingFriendTitle"), l10n("noVisibilityLevelAddingFriend"));
                    return;
                }

                String reftext = prop.getProperty("newPeer"+i);
                StringBuilder ref = new StringBuilder(reftext.replaceAll(".*?((?:[\\w,\\.]+\\=[^\r\n]+?)|(?:End))[ \\t]*(?:\\r?\\n)+", "$1\n"));
                ref = new StringBuilder(ref.toString().trim());
                int idx;
                while((idx = ref.indexOf("\r\n")) > -1) {
                    ref.deleteCharAt(idx);
                }
                while((idx = ref.indexOf("\r")) > -1) {
                    // Mac's just use \r
                    ref.setCharAt(idx, '\n');
                }
                String nodeToAdd = ref.toString();
                String[] split = nodeToAdd.split("\n");
                StringBuffer sb = new StringBuffer(nodeToAdd.length());
                boolean first = true;
                for(String s : split) {
                    if(s.equals("End")) break;
                    if(s.indexOf('=') > -1) {
                        if(!first)
                            sb.append('\n');
                    } else {
                        // Try appending it - don't add a newline.
                        // This will make broken refs work sometimes.
                    }
                    sb.append(s);
                    first = false;
                }
                nodeToAdd = sb.toString();
                ConnectionsToadlet.PeerAdditionReturnCodes result= addNewNode(nodeToAdd.trim().concat("\nEnd"), privateComment, trust, visibility);
                //Store the result
                Integer prev = results.get(result);
                if(prev == null) prev = Integer.valueOf(0);
                results.put(result, prev+1);
            }
            PageNode page = ctx.getPageMaker().getPageNode(l10n("reportOfNodeAddition"), ctx);
            HTMLNode pageNode = page.outer;
            HTMLNode contentNode = page.content;

            //We create a table to show the results
            HTMLNode detailedStatusBox=new HTMLNode("table");
            //Header of the table
            detailedStatusBox.addChild(new HTMLNode("tr")).addChildren(new HTMLNode[]{new HTMLNode("th",l10n("resultName")),new HTMLNode("th",l10n("numOfResults"))});
            HTMLNode statusBoxTable=detailedStatusBox.addChild(new HTMLNode("tbody"));
            //Iterate through the return codes
            for(ConnectionsToadlet.PeerAdditionReturnCodes returnCode: ConnectionsToadlet.PeerAdditionReturnCodes.values()){
                if(results.containsKey(returnCode)){
                    //Add a <tr> and 2 <td> with the name of the code and the number of occasions it happened. If the code is OK, we use green, red elsewhere.
                    statusBoxTable.addChild(new HTMLNode("tr","style","color:"+(returnCode== ConnectionsToadlet.PeerAdditionReturnCodes.OK?"green":"red"))).addChildren(new HTMLNode[]{new HTMLNode("td",l10n("peerAdditionCode."+returnCode.toString())),new HTMLNode("td",results.get(returnCode).toString())});
                }
            }

            HTMLNode infoboxContent = ctx.getPageMaker().getInfobox("infobox",l10n("reportOfNodeAddition"), contentNode, "node-added", true);
            infoboxContent.addChild(detailedStatusBox);
            infoboxContent.addChild("p").addChild("a", "href", "/addfriend/", l10n("addAnotherFriend"));

            addHomepageLink(infoboxContent.addChild("p"));
            synchronized(DarknetAppServer.class) {
                DarknetAppServer.changeNewDarknetPeersCount(0, pr.getNode());
            }
            writeHTMLReply(ctx, 500, l10n("reportOfNodeAddition"), pageNode.generate());
        }
    }

    /**
     * An infobox with a form to display all newly received references from darknetAppServer and ask user authorization
     * Ideally called by drawNodeRefBox i.e."add a friend" page
     * Can also be displayed on a separate toadlet if need be
     * TODO: Adding load error for the form if the new noderefs file is in use elsewhere
     */
    protected static void drawNewDarknetPeersAuthBox(HTMLNode contentNode, ToadletContext ctx, boolean isOpennet, String formTarget) {
        if (isOpennet) return;
        HTMLNode newPeersInfobox = contentNode.addChild("div", "class", "infobox infobox-normal");
        newPeersInfobox.addChild("div", "class", "infobox-header", l10n("newPeersBoxTitle"));
        HTMLNode peerAdditionContent = newPeersInfobox.addChild("div", "class", "infobox-content");
        HTMLNode peerAdditionForm = ctx.addFormChild(peerAdditionContent, formTarget, "addPeerForm");
        synchronized (DarknetAppServer.class) {
            SimpleFieldSet fs = null;
            Properties prop = new Properties();
            try {
                File file =  new File(DarknetAppServer.filename);
                prop.load(new FileInputStream(file));
            } catch (FileNotFoundException ex) {
                Logger.error(ctx, "Darknet App New Peers File Not Found",ex);
            } catch (IOException ex) {
                //File in Use..i.e. Synchronize with mobile is happening presently
            }
            newTempDarknetRefs = DarknetAppServer.newDarknetPeersCount;
            for (int i=1;i<=newTempDarknetRefs;i++) {
                String noderef = prop.getProperty("newPeer"+i);
                if (noderef==null || noderef.isEmpty()) continue;
                peerAdditionForm.addChild("b", l10n("peerNodeReference"));
                peerAdditionForm.addChild("pre", "id", "reference", noderef + '\n');

                peerAdditionForm.addChild("b", l10n("AuthTitle"));
                peerAdditionForm.addChild("#", " ");
                peerAdditionForm.addChild("br").addChild("input", new String[] { "type", "name", "value" }, new String[] { "radio", "auth"+i, "authorize" }).addChild("b", l10n("auth"));;
                peerAdditionForm.addChild("br").addChild("input", new String[] { "type", "name", "value" }, new String[] { "radio", "auth"+i, "reject" }).addChild("b", l10n("reject"));;
                peerAdditionForm.addChild("br");
                peerAdditionForm.addChild("b", l10n("peerTrustTitle"));
                peerAdditionForm.addChild("#", " ");
                peerAdditionForm.addChild("#", l10n("peerTrustIntroduction"));
                for(DarknetPeerNode.FRIEND_TRUST trust : DarknetPeerNode.FRIEND_TRUST.valuesBackwards()) { // FIXME reverse order
                    HTMLNode input = peerAdditionForm.addChild("br").addChild("input", new String[] { "type", "name", "value" }, new String[] { "radio", "trust"+i, trust.name() });
                    input.addChild("b", l10n("peerTrust."+trust.name())); // FIXME l10n
                    input.addChild("#", ": ");
                    input.addChild("#", l10n("peerTrustExplain."+trust.name()));
                }
                peerAdditionForm.addChild("br");

                peerAdditionForm.addChild("b", l10n("peerVisibilityTitle"));
                peerAdditionForm.addChild("#", " ");
                peerAdditionForm.addChild("#", l10n("peerVisibilityIntroduction"));
                for(DarknetPeerNode.FRIEND_VISIBILITY trust : DarknetPeerNode.FRIEND_VISIBILITY.values()) { // FIXME reverse order
                    HTMLNode input = peerAdditionForm.addChild("br").addChild("input", new String[] { "type", "name", "value" }, new String[] { "radio", "visibility"+i, trust.name() });
                    input.addChild("b", l10n("peerVisibility."+trust.name())); // FIXME l10n
                    input.addChild("#", ": ");
                    input.addChild("#", l10n("peerVisibilityExplain."+trust.name()));
                }
                peerAdditionForm.addChild("br");
                peerAdditionForm.addChild("#", (l10n("enterDescription") + ' '));
                peerAdditionForm.addChild("input", new String[] { "id", "type", "name", "size", "maxlength", "value" }, new String[] { "peerPrivateNote", "text", "peerPrivateNote"+i, "16", "250", "" });
                peerAdditionForm.addChild("br");

            }
            peerAdditionForm.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", "addNew", l10n("addNew") });
        }
    }


    private ConnectionsToadlet.PeerAdditionReturnCodes addNewNode(String nodeReference, String privateComment, DarknetPeerNode.FRIEND_TRUST trust, DarknetPeerNode.FRIEND_VISIBILITY visibility){
        SimpleFieldSet fs;

        try {
            nodeReference = Fields.trimLines(nodeReference);
            fs = new SimpleFieldSet(nodeReference, false, true, true);
            if(!fs.getEndMarker().endsWith("End")) {
                Logger.error(this, "Trying to add noderef with end marker \""+fs.getEndMarker()+"\"");
                return ConnectionsToadlet.PeerAdditionReturnCodes.WRONG_ENCODING;
            }
            fs.setEndMarker("End"); // It's always End ; the regex above doesn't always grok this
        } catch (IOException e) {
            Logger.error(this, "IOException adding reference :" + e.getMessage(), e);
            return ConnectionsToadlet.PeerAdditionReturnCodes.CANT_PARSE;
        } catch (Throwable t) {
            Logger.error(this, "Internal error adding reference :" + t.getMessage(), t);
            return ConnectionsToadlet.PeerAdditionReturnCodes.INTERNAL_ERROR;
        }
        PeerNode pn;
        try {
            pn = pr.getNode().createNewDarknetNode(fs, trust, visibility);
            ((DarknetPeerNode)pn).setPrivateDarknetCommentNote(privateComment);
        } catch (FSParseException e1) {
            return ConnectionsToadlet.PeerAdditionReturnCodes.CANT_PARSE;
        } catch (PeerParseException e1) {
            return ConnectionsToadlet.PeerAdditionReturnCodes.CANT_PARSE;
        } catch (ReferenceSignatureVerificationException e1){
            return ConnectionsToadlet.PeerAdditionReturnCodes.INVALID_SIGNATURE;
        } catch (Throwable t) {
            Logger.error(this, "Internal error adding reference :" + t.getMessage(), t);
            return ConnectionsToadlet.PeerAdditionReturnCodes.INTERNAL_ERROR;
        }
        if(Arrays.equals(pn.peerECDSAPubKeyHash, pr.getNode().getDarknetPubKeyHash())) {
            return ConnectionsToadlet.PeerAdditionReturnCodes.TRY_TO_ADD_SELF;
        }
        if(!this.pr.getNode().addPeerConnection(pn)) {
            return ConnectionsToadlet.PeerAdditionReturnCodes.ALREADY_IN_REFERENCE;
        }
        return ConnectionsToadlet.PeerAdditionReturnCodes.OK;
    }
    @Override
    public String path() {
        return PATH;
    }
}
