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
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
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
    private List<String> playerNames;
    private Client client;
    private Node canNode = new Node("cans");
    private Node playingfieldNode = new Node("Playingfield");
    private Node players = new Node("Enemies");
    private Node player;
    private Node cannonballNode = new Node("Cannonballs");
    private BitmapText info;
    private float time = 30f;
    private CreateGeos geos;
    private boolean ready = false;
    private boolean closing = false;
    private int STATE = Util.CLIENT_IDLE;
    private int shotIndex = 0;
    private int playerIndex;
    Material BGmat;

    public static void main(String[] args)
    {
        Util.initMessages();
        ClientMain app = new ClientMain();
        app.setPauseOnLostFocus(false);
        app.start();
    }

    public ClientMain()
    {
    }

    @Override
    public void simpleInitApp()
    {
        geos = new CreateGeos(assetManager);
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
        newMatch();
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
            client.addClientStateListener(new clientStateListener());
            client.addMessageListener(new ClientNetworkMessageListener());

        } catch (IOException ex)
        {
            setInfo("  Error connecting!\nPress Enter once to retry");
            STATE = Util.CLIENT_DISCONNECTED;
        }
        /*
         * Remember to create all objects (just declaring a reference 
         * variable does not create an object to which is the refers. No, 
         * you have to create the object explicitely.)
         */
        messageQueue = new ConcurrentLinkedQueue<String>();
    }

    public void simpleUpdate(float tpf)
    {
        for (Spatial ball : cannonballNode.getChildren())
        {
            ball.move(ball.getLocalRotation().getRotationColumn(2).mult(tpf * Util.CANNONBALL_SPEED));
            System.out.println(ball.getWorldTranslation().toString());
            if (ball.getWorldTranslation().distance(playingfieldNode.getWorldTranslation()) > Util.PLAYINGFIELD_RADIUS + Util.DEAD_MARGIN)
            {
                ball.removeFromParent();
            }
        }
    }

    private void newMatch()
    {
        //Set everything to default values. Somewhat redundant, but easy
        rootNode.detachAllChildren();
        rootNode.attachChild(guiNode);
        playerBallList = new ArrayList<List<Spatial>>(Util.MAX_PLAYERS);
        for (int i = 0; i < Util.MAX_PLAYERS; i++)
        {
            playerBallList.add(new ArrayList<Spatial>());
        }
        canNode = new Node("cans");
        playingfieldNode = new Node("Playingfield");
        playingfieldNode.attachChild(geos.createPlayingfield());
        players = new Node("Enemies");
        cannonballNode = new Node("Cannonballs");
        time = 30f;
        ready = false;
        shotIndex = 0;
        cam.setLocation(new Vector3f(0f, 20f, 0f));
        flyCam.setMoveSpeed(80);
        rootNode.attachChild(playingfieldNode);
        rootNode.attachChild(canNode);
        rootNode.attachChild(players);
        rootNode.attachChild(cannonballNode);
    }

    private class clientStateListener implements ClientStateListener
    {

        public void clientConnected(Client c)
        {
        }

        public void clientDisconnected(Client c, DisconnectInfo info)
        {
            if (!closing)
            {
                STATE = Util.CLIENT_DISCONNECTED;
                setInfo(info.reason);
                guiNode.setCullHint(Spatial.CullHint.Inherit);
                client = null;
            }
        }
    }

    private class ClientNetworkMessageListener implements MessageListener<Client>
    {

        public void messageReceived(Client source, Message m)
        {
            if (m instanceof AliveMessage)
            {
                client.send(new AliveMessage());
            }
            if (m instanceof ConnectionMessage)
            {
                ConnectionMessage message = (ConnectionMessage) m;
                if (message.getConnect())
                {
                    if (STATE != Util.CLIENT_WAITING) //If for some reason CansMessage arrived and terminated before ConnectionMessage
                    {
                        STATE = Util.CLIENT_LOADING;
                        setInfo("Loading Level");
                    }
                } else
                {
                    STATE = Util.CLIENT_DISCONNECTED;
                }
            }
            if (m instanceof CansMessage)
            {
                final CansMessage message = (CansMessage) m;
                Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        for (int i = 0; i < message.getTranslations().size(); i++)
                        {
                            Spatial can = geos.createCan(message.getValue(i));
                            can.setLocalTranslation(message.getTranslation(i));
                            canNode.attachChild(can);
                        }
                        STATE = Util.CLIENT_WAITING;
                        setInfo("Press Enter when Ready");
                        return true;
                    }
                });
            }
            if (m instanceof ReadyMessage)
            {
                if (STATE == Util.CLIENT_WAITING && ready)
                {
                    final ReadyMessage message = (ReadyMessage) m;
                    Future result = ClientMain.this.enqueue(new Callable()
                    {
                        public Object call() throws Exception
                        {
                            setInfo(message.getReadyPlayers() + " out of " + message.getTotalPlayers() + " ready!");
                            return true;
                        }
                    });
                }
            }
            if (m instanceof StartMessage)
            {
                final StartMessage message = (StartMessage) m;
                Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        playerNames = message.getPlayerNames();
                        playerIndex = message.getIndex();
                        float fractal = FastMath.TWO_PI / playerNames.size();
                        for (int i = 0; i < playerNames.size(); i++)
                        {
                            Node cannon = geos.createCannon();
                            cannon.rotate(0, fractal * i, 0);
                            cannon.move(cannon.getLocalRotation().getRotationColumn(2).mult(Util.PLAYINGFIELD_RADIUS));
                            cannon.move(0, 0.1f, 0);
                            if (i == playerIndex)
                            {
                                player = cannon;
                                cam.setLocation(cannon.localToWorld(new Vector3f(0, 50, 80), Vector3f.ZERO));
                                cam.lookAt(playingfieldNode.getWorldTranslation(), new Vector3f(0,1,0));
                            }
                            players.attachChild(cannon);
                        }
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
                        Geometry cBall = geos.createcannonball(message.getRotation(), message.getTranslation());
                        cannonballNode.attachChild(cBall);
                        playerBallList.get(message.getPlayer()).add(message.getBallID(), cBall);
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
                if (name == "toggleLaser" && STATE == Util.CLIENT_PLAYIING)
                {
                    player.getChild("laser").setCullHint((player.getChild("laser").getCullHint() == Spatial.CullHint.Dynamic) ? Spatial.CullHint.Always : Spatial.CullHint.Dynamic);
                } else if (name == "fire")
                {
                    if (STATE == Util.CLIENT_PLAYIING && playerBallList.get(playerIndex).size() <= Util.MAX_CANNONBALL)
                    {
                        Geometry cBall = geos.createcannonball(player.getLocalRotation(), player./*getChild("cannonballStartNode").*/getWorldTranslation());
                        cannonballNode.attachChild(cBall);
                        playerBallList.get(playerIndex).add(shotIndex, cBall); 
                        client.send(new ShootMessage(player.getLocalRotation(), player./*getChild("cannonballStartNode").*/getWorldTranslation(), shotIndex, client.getId()));
                        shotIndex++;
                        Sphere c = new Sphere(10, 10, 10);//Util.CANNONBALL_RESOLUTION,Util.CANNONBALL_RESOLUTION,Util.CANNONBALL_RADIUS);
                        Geometry bBall = new Geometry("cannonball", c);
                        Material matC = new Material (assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
                        //matC.setColor("Color", ColorRGBA.Yellow);
                        bBall.setMaterial(matC);
                        //bBall.setLocalRotation(player.getLocalRotation());
                        bBall.setLocalTranslation(new Vector3f(0,0,0));//player.getChild("cannonballStartNode").getWorldTranslation());
                        rootNode.attachChild(bBall);
                        System.out.println(player.getWorldTranslation().toString());
                        System.out.println(player.getChild("cannonballStartNode").getWorldTranslation().toString());
                        System.out.println(cBall.getWorldTranslation().toString());
                        System.out.println(cannonballNode.getWorldTranslation().toString());
                        System.out.println("---------------");
                    }
                } else if (name == "enter")
                {
                    if (STATE == Util.CLIENT_WAITING && !ready)
                    {
                        ready = true;
                        System.out.println("entered");
                        client.send(new ReadyMessage());
                        System.out.println("entered2");
                    } else if (STATE == Util.CLIENT_DISCONNECTED)
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
            if (time > 0 && STATE == Util.CLIENT_PLAYIING)
            {
                if (name == "turnLeft")
                {
                    player.rotate(0, tpf, 0);
                } else if (name == "turnRight")
                {
                    player.rotate(0, -tpf, 0);
                }
            }
        }
    };

    private void setInfo(String message)
    {
        info.setText(message);
        info.setLocalTranslation(settings.getWidth() / 2 - info.getLineWidth() / 2, settings.getHeight() / 2 - info.getLineHeight() / 2, 0);
    }

    @Override
    public void destroy()
    {
        closing = true;
        if (client != null)
        {
            client.close();
        }
        super.destroy();
    }
}
