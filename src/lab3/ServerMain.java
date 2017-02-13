/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lab3;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.ConnectionListener;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultCaret;
import lab3.messages.Game.*;
import lab3.messages.Network.*;
import lab3.messages.Setup.*;

/**
 * Client for NVE project as part of course D7038E at LTU.
 *
 * @author carf
 */
public class ServerMain extends SimpleApplication
{

    private CreateGeos geos;
    private int readyPlayers = 0;
    private Server server;
    private JTextArea textArea;
    private Node playingfieldNode = new Node("playingfieldNode");
    private Node playersNode = new Node("players");
    private Node cannonballNode = new Node("cannonballs");
    private Node canNode = new Node("canNode");
    private int STATE = Util.SERVER_IDLE;
    private Random rand = new Random();
    private boolean set = false;
    
    private int temp = 0;

    public static void main(String[] args)
    {
        Util.initMessages();
        ServerMain app = new ServerMain();
        app.start(JmeContext.Type.Headless);
    }

    ServerMain()
    {
    }

    public class connectionListener implements ConnectionListener
    {

        public void connectionAdded(Server server, HostedConnection conn)
        {
            if (STATE == Util.SERVER_IDLE)
            {
                print("Connection " + conn.getId() + " added");
                conn.setAttribute("aliveMessages", 0);
                conn.setAttribute("ready", false);
                conn.send(new ConnectionMessage(true));
                List<Integer> canValues = new ArrayList<Integer>();
                List<Vector3f> translations = new ArrayList<Vector3f>();
                for (Spatial can : canNode.getChildren())
                {
                    canValues.add((Integer) can.getUserData("value"));
                    translations.add(can.getLocalTranslation());
                }
                conn.send(new CansMessage(translations, canValues));
                set = true;
            } else
            {
                print("Connection refused");
                conn.send(new ConnectionMessage(false));
                conn.close("Already playing");
            }
        }

        public void connectionRemoved(Server server, HostedConnection conn)
        {
            print("Connection " + conn.getId() + " removed");
            if (STATE == Util.SERVER_IDLE)
            {
                server.broadcast(new DisconnectMessagae(conn.getId()));
                if ((Boolean) conn.getAttribute("ready") == true)
                {
                    readyPlayers--;
                    server.broadcast(new ReadyMessage(readyPlayers, server.getConnections().size()));
                } else if (server.getConnections().size() == readyPlayers && server.hasConnections())
                {
                    startGame();
                }
            } else
            {
                if(server.getConnections().isEmpty()) //reset for a new match (need fix)
                {
                    System.exit(0);
                    /*
                    STATE = Util.SERVER_IDLE;
                    readyPlayers = 0;
                    prepareMatch();
                    */
                }
                else
                {
                    //Remove player and such
                }
            }
        }
    }

    @Override
    public void simpleInitApp()
    {
        geos = new CreateGeos(assetManager);
        flyCam.setMoveSpeed(100);
        try
        {
            /*
             * Create the actual server object.
             */
            server = Network.createServer(Util.portNumber);
            server.start();
            server.addConnectionListener(new connectionListener());
            server.addMessageListener(new ServerListener());
            prepareMatch();

        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
        /*
         * Start a separat thread that will send messages to all connected 
         * clients every now and then. This thread will then create a 
         * KeyListener that will send messages whenever a key is typed.
         */
        new Thread(new AutomaticServerNetWrite("ServerNetWrite")).start();
        /*
         * Here we add, to the server, a listener that automatically handles 
         * all incoming messages. 
         */
    }

    @Override
    public void simpleUpdate(float tpf)
    {
        if (STATE == Util.SERVER_PLAYING)
        {
            List<List<Integer>> collisionControl = new ArrayList<List<Integer>>(readyPlayers);
            for(int i = 0; i < collisionControl.size(); i++)
                collisionControl.add(new ArrayList<Integer>(Util.MAX_CANNONBALL));
            
            for (Spatial ball : cannonballNode.getChildren())
            {
                ball.move(ball.getLocalRotation().getRotationColumn(2).mult(tpf * Util.CANNONBALL_SPEED));
                if (ball.getWorldTranslation().distance(playingfieldNode.getWorldTranslation()) > Util.PLAYINGFIELD_RADIUS + Util.DEAD_MARGIN)
                {
                    ball.removeFromParent();
                    continue;
                }
                
                /* Check whether the given ball has already collided with something this update */
                boolean run = true;
                List<Integer> IDList = collisionControl.get((Integer)ball.getUserData("player"));
                if(IDList.size() > 0)
                {
                    for(int ID : IDList)
                    {
                        if(ID == (Integer) ball.getUserData("ID"))
                            run = false;
                    }
                }
                CollisionResults results;
                if(run)
                {
                    results = new CollisionResults();
                    int ballIndex = cannonballNode.getChildIndex(ball);
                    cannonballNode.detachChild(ball);
                    cannonballNode.collideWith(ball.getWorldBound(), results);
                    cannonballNode.attachChildAt(ball, ballIndex);
                    if(results.size() > 0)
                    {
                        Geometry hit = results.getClosestCollision().getGeometry();
                        Vector3f contact = hit.getWorldTranslation();
                        Vector3f normal = ball.getWorldTranslation().subtract(contact).normalize();
                        Quaternion dir = new Quaternion();
                        dir.lookAt(normal, Vector3f.UNIT_Y);
                        ball.rotate(dir);
                        hit.rotate(dir.inverse());
                        server.broadcast(new CollisionMessage((Integer)ball.getUserData("player"), (Integer)ball.getUserData("ID"), (Integer)hit.getUserData("player"), (Integer)hit.getUserData("ID"), dir, dir.inverse()));
                        print("ball collide!");
                        print("Normal: " + normal.toString());
                        collisionControl.get((Integer)hit.getUserData("player")).add((Integer)hit.getUserData("ID"));
                    }
                }
                results = new CollisionResults();
                canNode.collideWith(ball.getWorldBound(), results);

                if (results.size() > 0)
                {
                    Geometry hit = results.getClosestCollision().getGeometry();
                    int value = hit.getUserData("value");
                    hit.setLocalTranslation(0, (Float) hit.getUserData("height") / 2, 0);
                    hit.rotate(0, 0, rand.nextFloat() * FastMath.TWO_PI);
                    hit.move(hit.getLocalRotation().getRotationColumn(0).mult(rand.nextFloat() * (Util.PLAYINGFIELD_RADIUS - Util.SAFETY_MARGIN)));
                    server.broadcast(new HitMessage((Integer) ball.getUserData("player"), canNode.getChildIndex(hit), hit.getLocalTranslation(), (Integer) ball.getUserData("ID")));
                    ball.removeFromParent();
                }
            }
        }
    }

    @Override
    public void simpleRender(RenderManager rm)
    {
    }

    @Override
    public void destroy()
    {
        System.out.println("Shutting down");
        for (HostedConnection client : server.getConnections())
        {
            client.close("Server shutdown");
        }
        server.close();
        super.destroy();
    }
    /*
     * Prints msg in the TextArea in the window that is not 
     * the jME window. 
     * 
     * @param msg 
     */

    private void print(String msg)
    {
        textArea.append(msg + "\n");


    }

    private class ServerListener implements MessageListener<HostedConnection>
    {

        public void messageReceived(HostedConnection source, Message m)
        {
            if (m instanceof AliveMessage)
            {
                source.setAttribute("aliveMessages", 0);
                //print("Alive ACK from " + source.getId());
            }
            if (m instanceof ReadyMessage)
            {
                if ((Boolean) source.getAttribute("ready") == false)
                {
                    print(source.getId() + " is ready!");
                    readyPlayers++;
                    source.setAttribute("ready", true);

                    if (readyPlayers == server.getConnections().size())
                    {
                        startGame();
                    } else
                    {
                        print("Not all ready");
                        server.broadcast(new ReadyMessage(readyPlayers, server.getConnections().size()));
                    }
                }
            }
            if (m instanceof ShootMessage)
            {
                print(source.getId() + " Shooting");
                final ShootMessage message = (ShootMessage) m;
                server.broadcast(Filters.notEqualTo(source), message);
                Future result = ServerMain.this.enqueue(new Callable()
                {
                    public Object call() throws Exception
                    {
                        Spatial ball = geos.createcannonball(message.getRotation(), message.getTranslation());
                        ball.setUserData("player", message.getPlayer());
                        ball.setUserData("ID", message.getBallID());
                        cannonballNode.attachChild(ball);
                        return true;
                    }
                });
            }
        }
    }
    /*
     * Sends a message to all clients when a(ny) key is pressed. 
     */

    private class ManualServerNetWrite implements KeyListener
    {
        /*
         * A counter to number the messages sent. This number is included 
         * in the text message. 
         */

        private int manualCounter = 1;

        ManualServerNetWrite()
        {
        }

        public void keyPressed(KeyEvent arg0)
        {
            /* do nothing */
        }

        public void keyReleased(KeyEvent arg0)
        {
            /* do nothing */
        }
        /*
         * Sends a message to all clients whenever a key is typed on the keybord
         * by sending over an order to the jME thread (the SimpleApplication,
         * know as "this").
         */

        public void keyTyped(KeyEvent e)
        {
            /*
             * "Eat" the key pressed (otherwise it will end up in the printout).
             */
            e.consume();
            final String m = "Manual (sent from jME thread) "
                    + (manualCounter++);
            print(m);
            Future result = ServerMain.this.enqueue(new Callable()
            {
                public Object call() throws Exception
                {

                    server.broadcast(new NetworkMessage(m));
                    return true;
                }
            });
        }
    }

    public void prepare()
    {

        Future result = ServerMain.this.enqueue(new Callable()
        {
            public Object call() throws Exception
            {

                return true;
            }
        });


    }
    /*
     * Sends to clients every now and then. 
     */

    private class AutomaticServerNetWrite implements Runnable
    {
        /*
         * Constants used to create a JFRame and determine how often to send. 
         */

        private int XPOS = 600, YPOS = 700, WINDOW_SIZE = 700,
                SLEEP_MIN = 1000, SLEEP_EXTRA = 2000,
                autoCounter = 1;
        /*
         * Creates a window with a TextArea separate from the jME thread. 
         * 
         * Adds a KeyListerer to the window. 
         */

        AutomaticServerNetWrite(final String name)
        {
            JFrame frame = new JFrame(name);
            frame.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e)
                {
                    destroy();
                }
            });
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            textArea = new JTextArea("");
            /*
             * Add a listener teh reacts to keys pressed and sends a messages 
             * to all clients whenever a(ny) key is typed. 
             */
            textArea.addKeyListener(new ManualServerNetWrite());
            /*
             * Tedious GUI details (google for info...)
             */
            DefaultCaret caret = (DefaultCaret) textArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            JScrollPane scroll = new JScrollPane(textArea,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            frame.setSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE));
            frame.setLocation(XPOS, YPOS);
            frame.add(scroll);
            frame.setVisible(true);
        }
        /*
         * This method is called when start() is called on a thread object. 
         */

        public void run()
        {

            while (true)
            {
                String m = "Automatic (sent from ServerNetWrite) "
                        + (autoCounter++);
                //print(m);
                //synchronized (PlayerNames)
                {
                    for (HostedConnection client : server.getConnections())
                    {

                        client.send(new AliveMessage());
                        //print("AliveMessage to Client " + client.getId() + " with " + (Integer) client.getAttribute("aliveMessages") + "failiours");
                        client.setAttribute("aliveMessages", ((Integer) client.getAttribute("aliveMessages")) + 1);
                        if ((Integer) client.getAttribute("aliveMessages") > Util.MAX_ALIVE_FAILURES)
                        {
                            print("Kicking unresponsive user.");
                            client.close("Timeout");
                        }

                    }
                }
                try
                {
                    Thread.sleep(SLEEP_MIN + rand.nextInt(SLEEP_EXTRA));
                } catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void prepareMatch()
    {
        rootNode.detachAllChildren();
        playingfieldNode.detachAllChildren();
        playersNode.detachAllChildren();
        canNode.detachAllChildren();
        cannonballNode.detachAllChildren();
        playingfieldNode.attachChild(geos.createPlayingfield());
        for (int i = 0; i < Util.LARGECAN_NUM; i++)
        {
            Spatial can = geos.createCan(Util.LARGECAN_VALUE);
            can.rotate(0, 0, rand.nextFloat() * FastMath.TWO_PI);
            can.move(can.getLocalRotation().getRotationColumn(0).mult(rand.nextFloat() * (Util.PLAYINGFIELD_RADIUS - Util.SAFETY_MARGIN)));
            canNode.attachChild(can);
        }
        for (int i = 0; i < Util.MEDIUMCAN_NUM; i++)
        {
            Spatial can = geos.createCan(Util.MEDIUMCAN_VALUE);
            can.rotate(0, 0, rand.nextFloat() * FastMath.TWO_PI);
            can.move(can.getLocalRotation().getRotationColumn(0).mult(rand.nextFloat() * (Util.PLAYINGFIELD_RADIUS - Util.SAFETY_MARGIN)));
            canNode.attachChild(can);
        }
        for (int i = 0; i < Util.SMALLCAN_NUM; i++)
        {
            Spatial can = geos.createCan(Util.SMALLCAN_VALUE);
            can.rotate(0, 0, rand.nextFloat() * FastMath.TWO_PI);
            can.move(can.getLocalRotation().getRotationColumn(0).mult(rand.nextFloat() * (Util.PLAYINGFIELD_RADIUS - Util.SAFETY_MARGIN)));
            canNode.attachChild(can);
        }
        rootNode.attachChild(playingfieldNode);
        rootNode.attachChild(canNode);
        rootNode.attachChild(cannonballNode);
        rootNode.attachChild(playersNode);
    }

    public void startGame()
    {
        STATE = Util.SERVER_PLAYING;
        
        List<String> playerNames = new ArrayList<String>();
        
        float fractal = FastMath.TWO_PI / server.getConnections().size();
        for (int i = 0; i < server.getConnections().size(); i++)
        {
            playerNames.add(String.valueOf(server.getConnection(i).getId()));
            Node cannon = geos.createCannon();
            cannon.rotate(0, fractal * i, 0);
            cannon.move(cannon.getLocalRotation().getRotationColumn(2).mult(Util.PLAYINGFIELD_RADIUS));
            cannon.rotate(0, FastMath.PI, 0);
            playersNode.attachChild(cannon);
        }
        
        int i = 0;
        for (HostedConnection client : server.getConnections())
        {
            client.send(new StartMessage(playerNames, i));
            i++;
        }
    }
}
