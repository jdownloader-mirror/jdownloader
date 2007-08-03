package jd.plugins;

import java.util.Vector;


public abstract class PluginForDecrypt extends Plugin{
    /**
     * Diese Methode entschl�sselt Links.
     * 
     * @param cryptedLinks Ein Vector, mit jeweils einem verschl�sseltem Link. 
     *                     Die einzelnen verschl�sselten Links werden aufgrund des Patterns  
     *                     {@link jd.plugins.Plugin#getSupportedLinks() getSupportedLinks()} herausgefiltert
     * @return Ein Vector mit Klartext-links
     */
    public abstract Vector<String> decryptLinks(Vector<String> cryptedLinks);
}
