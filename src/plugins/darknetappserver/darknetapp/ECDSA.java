/**
 * Elliptical Curve DSA implementation from BouncyCastle
 * TODO: Store in node.crypt instead of using properties file
 */
package plugins.darknetappserver.darknetapp;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.util.Base64;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Illutionist
 */
public class ECDSA {
    private static PublicKey publickey;
    private static PrivateKey privatekey;
    private static boolean generated = false;
    private static Properties prop = new Properties();    
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    public static byte[] getPublicKey() throws UnsupportedEncodingException  {
        byte[] key = null;
        if (!generated) {
            initialize();
        }
        if (generated) key = publickey.getEncoded();
        return key;
    }
    
    public static byte[] getSignature(String text)  {
        byte[] signature = null;
        String sign = "";
        if (!generated) {
            initialize();
        }
        if (generated){
                try { 
                    Signature dsa = Signature.getInstance("SHA1withECDSA", "BC");
                    dsa.initSign(privatekey);
                    byte[] buf = text.getBytes("UTF-8");
                    dsa.update(buf, 0, buf.length);
                    signature = dsa.sign();
                    sign = new String(signature,"UTF-8");
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchProviderException ex) {
                    Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvalidKeyException ex) {
                    Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SignatureException ex) {
                    Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
            }
            }
        return signature;
    }
    public static void initialize() {
        try {
            File file = new File("ECDSAconfig.properties");
            if (!file.exists()) {
                file.createNewFile();              
                prop.load(new FileInputStream(file));
                generateProperties();
            }
            else {
                prop.load(new FileInputStream(file));
                pullProperties();
            }
        }
        catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchProviderException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private static void generateProperties() throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException, IOException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(256, random);
        KeyPair pair = keyGen.generateKeyPair();
        privatekey = pair.getPrivate();
        publickey = pair.getPublic();
        
        Base64.Encoder encoder = Base64.getMimeEncoder();
        String pri = encoder.encode(privatekey.getEncoded());
        String pub = encoder.encode(publickey.getEncoded());
        prop.setProperty("DSAprivatekey",pri);
        prop.setProperty("DSApublickey",pub);
        generated = true;
        prop.store(new FileOutputStream("ECDSAconfig.properties"), null);
        
    }
    private static void pullProperties() throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, IOException {

        String priv = prop.getProperty("DSAprivatekey");
        String publ = prop.getProperty("DSApublickey");
        Base64.Decoder decoder = Base64.getMimeDecoder();
        byte[] pri= decoder.decodeBuffer(priv);
        byte[] pub = decoder.decodeBuffer(publ);
        PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(pri);
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pub);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        privatekey =keyFactory.generatePrivate(priKeySpec);
        publickey =keyFactory.generatePublic(pubKeySpec);
        generated = true;
    }
    public static boolean verify(String data,byte[] signature,byte[] publicKey) {
        boolean verify = false;
        try {
            
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
            byte[] buf = data.getBytes("UTF-8");
            Signature sig = Signature.getInstance("SHA1withECDSA", "BC");
            sig.initVerify(pubKey);
            sig.update(buf, 0,buf.length);
            verify = sig.verify(signature);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchProviderException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SignatureException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        }
        return verify;
    }
    

    
}
