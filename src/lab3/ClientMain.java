/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lab3;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
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
import com.jme3.scene.shape.Box;
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
     * playerBallList solves the issue of not being able to index balls arriving from
     * different players simultaneously. Now each player indexes their own shots
     * and thus we can keep a correct and consistent index over all clients.
     * Note: if we define it later we can use the correct amount of player, isntead of MAX 
     */
    private List<List<Spatial>> playerBallList = new ArrayList<List<Spatial>>(Util.MAX_PLAYERS); 
    private List<RotateMessage> rotateMessageList = new ArrayList<RotateMessage>();
    private boolean[] rotateConvergence;
    private float rotateUpdate = Util.ROTATE_UPDATE;
    private String[] playerNames;
    private Client client;
    private Node canNode = new Node("cans");
    private Node playingfieldNode = new Node("Playingfield");
    private Node players = new Node("Enemies");
    private Node player;
    private Node cannonballNode = new Node("Cannonballs");
    private Node guiMenu = new Node("Gui Menu");
    private Node scoreGuiNode = new Node("Score GUI");
    private Node timeNode = new Node("timeGui");
    private Node nameNode = new Node("nameField");
    private BitmapText scoreGui;
    private BitmapText info;
    private BitmapText timeGui;
    private BitmapText nameText;
    private String myName = "";
    private int[] score;
    private float time = 30f;
    private CreateGeos geos;
    private boolean ready = false;
    private boolean closing = false;
    private int STATE = Util.CLIENT_IDLE;
    private int shotIndex = 0;
    private int playerIndex;
    Material BGmat;

    

    public class rawInputListener implements RawInputListener
    {

        public void beginInput()
        {    
        }

        public void endInput()
        {
        }

        public void onJoyAxisEvent(JoyAxisEvent evt)
        {
        }

        public void onJoyButtonEvent(JoyButtonEvent evt)
        {
        }

        public void onMouseMotionEvent(MouseMotionEvent evt)
        {
            
        }

        public void onMouseButtonEvent(MouseButtonEvent evt)
        {
        }

        public void onKeyEvent(KeyInputEvent evt)
        {
            if(!ready && evt.isPressed())
            {
                boolean a = false;
                int keyCode = evt.getKeyCode();
                if(keyCode == KeyInput.KEY_BACK && myName.length() > 0)
                {
                    myName = myName.substring(0, myName.length()-1);
                    evt.setConsumed();
                    a = true;
                }
                if (!a && keyCode == KeyInput.KEY_RETURN)
                    {
                        a = true;
                        evt.setConsumed();
                        if (STATE == Util.CLIENT_WAITING && !ready)
                        {
                            ready = true;
                            client.send(new ReadyMessage(myName));
                        }
                        else if (STATE == Util.CLIENT_DISCONNECTED)
                        {
                            BGmat.setColor("Color", ColorRGBA.Orange);
                            setInfo("Retrying");
                            connectToServer();
                        }
                    }

                char c = evt.getKeyChar();
                if(!a && c != 0)
                {
                    if(c == 27) // Esc is a character....                
                        return;
                    System.out.println("C: " + (int) c + " : " + keyCode);
                    myName = myName + c;
                    evt.setConsumed();
                    a = true;
                }
                
                System.out.println("Consumed: " + evt.isConsumed());
                if(a)
                    setName(myName);
            }
        }

        public void onTouchEvent(TouchEvent evt)
        {
        }
    }
    
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
        
        nameText = new BitmapText(guiFont, false);
        nameText.setSize(guiFont.getCharSet().getRenderedSize() * 1.5f);
        
        guiMenu.attachChild(BG);
        guiMenu.attachChild(info);
        guiMenu.attachChild(nameText);
        
        scoreGui = new BitmapText(guiFont, false);
        scoreGui.setText("Hej");
        scoreGui.setLocalTranslation(10, settings.getHeight()-10-guiFont.getCharSet().getLineHeight(), 1);
        scoreGuiNode.attachChild(scoreGui);
        
        timeGui = new BitmapText(guiFont, false);
        timeGui.setText(String.valueOf(time));
        timeGui.setLocalTranslation(10, settings.getHeight()-10, 1);
        timeNode.attachChild(timeGui);
        
        guiNode.attachChild(timeNode);
        guiNode.attachChild(guiMenu);
        guiNode.attachChild(scoreGuiNode);
        
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
        if(STATE == Util.CLIENT_PLAYIING)
        {
            time -= tpf;
            rotateUpdate -= tpf;
            if(rotateUpdate < 0)
            {
                if(rotateMessageList.size() > 0)
                {
                    client.send(rotateMessageList.get(rotateMessageList.size()-1));
                    rotateMessageList.clear();
                    rotateUpdate = Util.ROTATE_UPDATE;
                }
            }
            for(int i = 0; i < rotateConvergence.length; i++)
            {
                Spatial cannon = players.getChild(i);
                if (i == playerIndex)
                    continue;
                if(rotateConvergence[i])
                {
                    cannon.rotate(0, tpf*2*(Integer)cannon.getUserData("special"), 0);
                    if(time <= (Float) cannon.getUserData("time"))
                        rotateConvergence[i] = false;
                }
                else
                    cannon.rotate(0, tpf*(Integer)cannon.getUserData("rotateDirection"), 0);
            }
            
            for (Spatial ball : cannonballNode.getChildren())
            {
                ball.move(ball.getLocalRotation().getRotationColumn(2).mult(tpf * Util.CANNONBALL_SPEED));
                if (ball.getWorldTranslation().distance(playingfieldNode.getWorldTranslation()) > Util.PLAYINGFIELD_RADIUS + Util.DEAD_MARGIN)
                {
                    removeBall((Integer)ball.getUserData("player"), (Integer)ball.getUserData("ID"));
                }
            }
        }
        if(time < 0)
            timeGui.setText("0");
        else
            timeGui.setText(String.valueOf(time));

    }

    private void newMatch()
    {
        //Set everything to default values. Somewhat redundant, but easy (I dont remember why I do this, but too lazy to find out)
        rootNode.detachAllChildren();
        rootNode.attachChild(geos.createcannonball(Quaternion.ZERO, Vector3f.ZERO));
        playerBallList = new ArrayList<List<Spatial>>(Util.MAX_PLAYERS);
        for (int i = 0; i < Util.MAX_PLAYERS; i++)
        {
            playerBallList.add(new ArrayList<Spatial>(Util.MAX_CANNONBALL));
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
                guiMenu.setCullHint(Spatial.CullHint.Inherit);
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
            else if (m instanceof ConnectionMessage)
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
            else if (m instanceof CansMessage)
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
            else if (m instanceof ReadyMessage)
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
            else if (m instanceof StartMessage)
            {
                final StartMessage message = (StartMessage) m;
                Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        playerNames = message.getPlayerNames();
                        score = new int[playerNames.length];
                        rotateConvergence = new boolean[playerNames.length];
                        playerIndex = message.getIndex();
                        float fractal = FastMath.TWO_PI / playerNames.length;
                        for (int i = 0; i < playerNames.length; i++)
                        {
                            Node cannon = geos.createCannon();
                            cannon.rotate(0, fractal * i, 0);
                            cannon.move(cannon.getLocalRotation().getRotationColumn(2).mult(Util.PLAYINGFIELD_RADIUS));
                            cannon.move(0, 0.1f, 0);
                            cannon.setUserData("rotateDirection", 0);
                            if (i == playerIndex)
                            {
                                player = cannon;
                                cam.setLocation(cannon.localToWorld(new Vector3f(0, 50, 80), new Vector3f(0,0,0)));
                                cam.lookAt(playingfieldNode.getWorldTranslation(), new Vector3f(0,1,0));
                            }
                            players.attachChild(cannon);
                        }
                        STATE = Util.CLIENT_PLAYIING;
                        updateScore();
                        guiMenu.setCullHint(Spatial.CullHint.Always); 
                       return true;
                    }
                });
            }
            else if (m instanceof ShootMessage)
            {
                final ShootMessage message = (ShootMessage) m;
                Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        Geometry cBall = geos.createcannonball(message.getRotation(), message.getTranslation());
                        addBall(cBall, message.getBallID(), message.getPlayer());
                        return true;
                    }
                });
            }
            else if (m instanceof HitMessage)
            {
                final HitMessage message = (HitMessage) m;
                Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        //Move can
                        Spatial can = canNode.getChild(message.getHitCan());
                        can.setLocalTranslation(message.getNewTranslation());
                        removeBall(message.getPlayer(), message.getBallID());
                        //SCORE PLAYER??
                        score[message.getPlayer()] += (Integer) can.getUserData("value");
                        updateScore();
                        return true;
                    }
                });
            }
            else if (m instanceof CollisionMessage)
            {
                final CollisionMessage message = (CollisionMessage) m;
                Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        int[] players = message.getPlayers();
                        int[] ballIDs = message.getBallIDs();
                        Quaternion[] directions = message.getDirections();
                        for (int i = 0; i < players.length; i++)
                        {
                            List<Spatial> balls = playerBallList.get(players[i]);
                            int ballID = ballIDs[i];
                            for (Spatial ball : balls)
                            {
                                if((Integer) ball.getUserData("ID") == ballID)
                                {
                                    ball.rotate(directions[i]);
                                }
                            }
                        }
                        return true;
                    }
                });
            }
            else if (m instanceof WinnerMessage)
            {
                final WinnerMessage message = (WinnerMessage) m;
                Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        guiMenu.setCullHint(Spatial.CullHint.Inherit);
                        int winner = message.getWinner();
                        if(winner == -1)
                        {
                            setInfo("DRAW!");
                        }
                        else
                        {
                            setInfo(""+playerNames[winner]+" WINS!");
                        }
                        return true;
                    }
                });
            }
            else if (m instanceof  RotateMessage)
            {
                final RotateMessage message = (RotateMessage) m;
                Future result = ClientMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        int playerID = message.getPlayer();
                        Spatial cannon = players.getChild(playerID);
                        Vector3f currentDirVector = cannon.getLocalRotation().getRotationColumn(2);
                        Vector3f rotateStartVector = message.getStartRotation().getRotationColumn(2);
                        double d = Math.atan2(rotateStartVector.z, rotateStartVector.x) - Math.atan2(currentDirVector.z, currentDirVector.x);
                        float angle = (float) d; //Varför funkar det att casta här men inte linjen ovanför???? .^.
                        cannon.setUserData("rotateDirection", message.getDirection());
                        if(Math.abs(angle) < 0.1)
                        {
                            cannon.setLocalRotation(message.getStartRotation());
                            rotateConvergence[playerID] = false;
                        }
                        else if(angle > 0)  //We're currently to the LEFT of where we should be
                        {
                            rotateConvergence[playerID] = true;
                            switch(message.getDirection())
                            {
                                case -1:    //and we want to turn left
                                    System.out.println("Left - Left");
                                    cannon.setUserData("special", 0);
                                    cannon.setUserData("time", time-angle);
                                    break;
                                case 0:     //and we want to stay still
                                    System.out.println("Left - Stay");
                                    cannon.setUserData("special", -1);
                                    cannon.setUserData("time", time-(angle/2));
                                    break;
                                case 1:     //and we want to turn right
                                    System.out.println("Left - Right");
                                    cannon.setUserData("special", -1);
                                    cannon.setUserData("time", time-angle);
                                    break;
                            }
                        }
                        else if(angle < 0)  //We're currently to the RIGHT
                        {
                            rotateConvergence[playerID] = true;
                            switch(message.getDirection())
                            {
                                case -1:    //and we want to turn left
                                    System.out.println("Right - Left");
                                    cannon.setUserData("special", 1);
                                    cannon.setUserData("time", time-angle);
                                    break;
                                case 0:     //and we want to stay still
                                    System.out.println("Right - Stay");
                                    cannon.setUserData("special", 1);
                                    cannon.setUserData("time", time-(angle/2));
                                    break;
                                case 1:     //and we want to turn right
                                    System.out.println("Right - Right");
                                    cannon.setUserData("special", 0);
                                    cannon.setUserData("time", time-angle);
                                    break;
                            }
                        }
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

        inputManager.addRawInputListener(new rawInputListener());
        inputManager.addListener(actionListener, "toggleLaser", "fire", "enter", "turnLeft", "turnRight");
        inputManager.addListener(analogListener, "turnLeft", "turnRight");
    }
    private ActionListener actionListener = new ActionListener()
    {
        public void onAction(String name, boolean keyPressed, float tpf)
        {
            if(time > 0 && STATE == Util.CLIENT_PLAYIING)
            {
                if (keyPressed)
                {
                    if (name == "toggleLaser" && STATE == Util.CLIENT_PLAYIING)
                    {
                        player.getChild("laser").setCullHint((player.getChild("laser").getCullHint() == Spatial.CullHint.Dynamic) ? Spatial.CullHint.Always : Spatial.CullHint.Dynamic);
                    } else if (name == "fire")
                    {
                        if (STATE == Util.CLIENT_PLAYIING && playerBallList.get(playerIndex).size() <= Util.MAX_CANNONBALL)
                        {
                            Geometry cBall = geos.createcannonball(player.getLocalRotation(), player.getChild("cannonballStartNode").getWorldTranslation());
                            addBall(cBall, shotIndex, playerIndex); 
                            client.send(new ShootMessage(player.getLocalRotation(), player.getChild("cannonballStartNode").getWorldTranslation(), shotIndex, client.getId()));
                            shotIndex++;
                        }
                    }
                    else if(name == "turnLeft")
                    {
                        rotateMessageList.add(new RotateMessage(1, player.getLocalRotation(), playerIndex));
                    }
                    else if(name == "turnRight")
                    {
                        rotateMessageList.add(new RotateMessage(-1, player.getLocalRotation(), playerIndex));
                    }
                }
                else if(name == "turnLeft" || name == "turnRight")
                {
                    rotateMessageList.add(new RotateMessage(0, player.getLocalRotation(), playerIndex));
                }
            }
        }
    };
    private AnalogListener analogListener = new AnalogListener()
    {
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

    private void setName(String message)
    {
        nameText.setText(message);
        nameText.setLocalTranslation(settings.getWidth() / 2 - nameText.getLineWidth() / 2, settings.getHeight() / 2 + nameText.getLineHeight() / 2, 0);
    }

    
    private void setInfo(String message)
    {
        info.setText(message);
        info.setLocalTranslation(settings.getWidth() / 2 - info.getLineWidth() / 2, settings.getHeight() / 2 - info.getLineHeight() / 2, 0);
    }
    private void updateScore()
    {
        String text = "Score: \n";
        for(int i = 0; i < score.length; i++)
        {
            text+= playerNames[i] +": "+ score[i] + "\n";
        }
        scoreGui.setText(text);
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
    
    private void addBall(Geometry cBall, int ID, int pIndex)
    {
        cBall.setUserData("ID", ID);
        cBall.setUserData("player", pIndex);
        cannonballNode.attachChild(cBall);
        playerBallList.get(pIndex).add(cBall);
    }
    private void removeBall(int pIndex, int ballID)
    {
        List<Spatial> playerBalls = playerBallList.get(pIndex);
        for (int i = 0; i < playerBalls.size(); i++)
        {
            if((Integer) playerBalls.get(i).getUserData("ID") == ballID)
            {
                playerBalls.get(i).removeFromParent();
                playerBalls.remove(i);
            }
        }
    }
}