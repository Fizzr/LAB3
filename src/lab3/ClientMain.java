/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lab3;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import lab3.messages.Game.*;
import lab3.messages.Setup.*;
import lab3.messages.Network.*;

/**
 *
 * @author Claes
 */
public class ClientMain extends SimpleApplication
{
    /*
     * The queue below is used to transfer incoming messages from the thread
     * that receives them (ClientNetworkMessageListener) to the jME thread.
     * This is not needed in this example - ClientNetworkMessageListener could
     * do the printout itself - but if something needs to be done with the s
     * cene graph, using enqueue and creating a Callable is vital. 
     */

    private ConcurrentLinkedQueue<String> messageQueue;
    private Client client;
    private Node canNode = new Node("Cans");
    private Node playingfieldNode = new Node("Playingfield");
    private Node player;
    private Node enemies = new Node("Enemies");
    private BitmapText info;
    private float time = 30f;
    private CreateGeos geos;
    
    
    public static void Main(String[] args)
    {
        Util.initMessages();
        ClientMain app = new ClientMain();
        app.start();
    }

    @Override
    public void simpleInitApp()
    {
        geos = new CreateGeos(assetManager);
        player = geos.createCannon();
        Quad UIQuad = new Quad(settings.getWidth(), settings.getHeight());
        Geometry BG = new Geometry("start", UIQuad);
        Material BGmat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        BGmat.setColor("Color", ColorRGBA.Blue);
        BG.setMaterial(BGmat);

    }
    
    
    public void connectToServer()
    {
        try
        {
            /*
             * Open up a connection to the server. Note how the IP address 
             * and port number is taken from the class Util. 
             */
            client = Network.connectToServer(Util.hostName, Util.portNumber);
            client.start();
        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
        /*
         * Remember to create all objects (just declaring a reference 
         * variable does not create an object to which is the refers. No, 
         * you have to create the object explicitely.)
         */
        messageQueue = new ConcurrentLinkedQueue<String>();
        /*
         * Add a listener that will handle incoming messages (network packets).
         */
        client.addMessageListener(new ClientNetworkMessageListener());
    }

    private class ClientNetworkMessageListener implements MessageListener<Client>
    {
        public void messageReceived(Client source, Message m)
        {
            if (m instanceof CansMessage)
            {
                final CansMessage message = (CansMessage) m;
                 Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        for (Spatial can : message.getCans())
                        {
                            canNode.attachChild(can);
                        }
                        return true;
                    }
                });
            }
            if (m instanceof ShootMessage)
            {
                
            }
        }
    }

    private void inputInit()
    {
        inputManager.addMapping("turnLeft", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("turnRight", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("toggleLaser", new KeyTrigger(KeyInput.KEY_L));
        inputManager.addMapping("fire", new KeyTrigger(KeyInput.KEY_SPACE));

        inputManager.addListener(actionListener, "toggleLaser", "fire");
        inputManager.addListener(analogListener, "turnLeft", "turnRight");
    }
    private ActionListener actionListener = new ActionListener()
    {
        public void onAction(String name, boolean keyPressed, float tpf)
        {
            if (keyPressed && time > 0)
            {
                if (name == "toggleLaser")
                {
                } else if (name == "fire")
                {
                }
            }
        }
    };
    private AnalogListener analogListener = new AnalogListener()
    {
        //"cuboidLeanBack", "cuboidLeanForward", "sphereShrinkRay", "sphereEnlargmentRay")
        public void onAnalog(String name, float value, float tpf)
        {
            if (time > 0)
            {
                if (name == "turnLeft")
                {
                } else if (name == "turnRight")
                {
                }
            }
        }
    };
}
