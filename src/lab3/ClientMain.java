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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
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
    /*
     * Below Queue solves issue of not being able to index balls arriving from
     * different players simultaneously. Now each player indexes their own shots
     * and thus we can keep a correct and consistent index over all clients.
     */
    private List<List<Spatial>> playerBallList = new ArrayList<List<Spatial>>(Util.MAX_PLAYERS);
    private Client client;
    private Node canNode;
    private Node playingfieldNode = new Node("Playingfield");
    private Node player;
    private Node enemies = new Node("Enemies");
    private Node cannonballNode = new Node("Cannonballs");
    private BitmapText info;
    private float time = 30f;
    private CreateGeos geos;
    private boolean ready = false;
    private int STATE = Util.CLIENT_IDLE;
    private int shotIndex = 0;
    Material BGmat;

    public static void main(String[] args)
    {
        Util.initMessages();
        ClientMain app = new ClientMain();
        app.start();
    }

    public ClientMain()
    {
    }

    @Override
    public void simpleInitApp()
    {
        geos = new CreateGeos(assetManager);
        player = geos.createCannon();
        Quad UIQuad = new Quad(settings.getWidth(), settings.getHeight());
        Geometry BG = new Geometry("start", UIQuad);
        BGmat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        BGmat.setColor("Color", ColorRGBA.Blue);
        BG.setMaterial(BGmat);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        info = new BitmapText(guiFont, false);
        info.setSize(guiFont.getCharSet().getRenderedSize() * 1.5f);
        setInfo("Connecting...");
        guiNode.attachChild(BG);
        guiNode.attachChild(info);
        rootNode.attachChild(guiNode);
        connectToServer();
        inputInit();
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
            /*
             * Add a listener that will handle incoming messages (network packets).
             */
            client.addMessageListener(new ClientNetworkMessageListener());

        } catch (IOException ex)
        {
            setInfo("  Error connecting!\nPress Enter once to retry");
            STATE = Util.CLIENT_DECLINED;
        }
        /*
         * Remember to create all objects (just declaring a reference 
         * variable does not create an object to which is the refers. No, 
         * you have to create the object explicitely.)
         */
        messageQueue = new ConcurrentLinkedQueue<String>();
    }

    private class ClientNetworkMessageListener implements MessageListener<Client>
    {

        public void messageReceived(Client source, Message m)
        {
            if (m instanceof ConnectionMessage)
            {
                ConnectionMessage message = (ConnectionMessage) m;
                if (message.getConnect())
                {
                    if (STATE != Util.CLIENT_WAITING) //If for some reason CansMessage arrived and terminated before ConnectionMessage
                    {
                        STATE = Util.CLIENT_LOADING;
                    }
                } else
                {
                    STATE = Util.CLIENT_DECLINED;
                    setInfo(message.getMessage());
                }
            }
            if (m instanceof CansMessage)
            {
                final CansMessage message = (CansMessage) m;
                Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        canNode = message.getCans();
                        STATE = Util.CLIENT_WAITING;
                        return true;
                    }
                });
            }

            if (m instanceof StartMessage)
            {
                Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        STATE = Util.CLIENT_PLAYIING;
                        guiNode.setCullHint(Spatial.CullHint.Always);
                        return true;
                    }
                });
            }
            if (m instanceof ShootMessage)
            {
                final ShootMessage message = (ShootMessage) m;
                Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        cannonballNode.attachChild(message.getCannonball());
                        playerBallList.get(message.getPlayer()).add(message.getBallID(), message.getCannonball());
                        return true;
                    }
                });
            }
            if (m instanceof HitMessage)
            {
                final HitMessage message = (HitMessage) m;
                Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        //Move can
                        canNode.getChild(message.getHitCan()).setLocalTranslation(message.getNewTranslation());
                        //Remove ball from cannonballNode
                        playerBallList.get(message.getPlayer()).get(message.getBallID()).removeFromParent();

                        //SCORE PLAYER??
                        return true;
                    }
                });
            }
        }
    }

    private void inputInit()
    {
        inputManager.addMapping("turnLeft", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("turnRight", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("toggleLaser", new KeyTrigger(KeyInput.KEY_L));
        inputManager.addMapping("fire", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("enter", new KeyTrigger(KeyInput.KEY_RETURN));

        inputManager.addListener(actionListener, "toggleLaser", "fire", "enter");
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
                    if (STATE == Util.CLIENT_PLAYIING)
                    {
                        Geometry cBall = geos.createcannonball(player);
                        cannonballNode.attachChild(cBall);
                        playerBallList.get(client.getId()).add(shotIndex, cBall);
                        client.send(new ShootMessage(cBall, shotIndex, client.getId()));
                        shotIndex++;
                    }
                } else if (name == "enter")
                {
                    if (STATE == Util.CLIENT_WAITING && !ready)
                    {
                        ready = true;
                        client.send(new ReadyMessage());
                    } else if (STATE == Util.CLIENT_DECLINED)
                    {
                        BGmat.setColor("Color", ColorRGBA.Orange);
                        setInfo("Retrying");
                        connectToServer();
                    }
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
                } 
                else if (name == "turnRight")
                {
                }
            }
        }
    };
    
    private void setInfo(String message)
    {
        info.setText(message);
        info.setLocalTranslation(settings.getWidth() / 2 - info.getLineWidth()/2, settings.getHeight() / 2 - info.getLineHeight()/2, 0);
    }
}
