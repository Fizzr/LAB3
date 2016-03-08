/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lab3;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.network.ConnectionListener;
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
import java.io.IOException;
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

    private int readyPlayers = 0;
    private Server server;
    private JTextArea textArea;
    private Node playingfieldNode;
    private Node playersNode;
    private Node cannonballNode;
    private Node canNode;
    private int STATE = Util.SERVER_IDLE;
    private Random sRand = new Random();
    private long seed = sRand.nextLong();
    private Random gRand = new Random(seed);

    public static void main(String[] args)
    {
        ServerMain app = new ServerMain();
        app.start(JmeContext.Type.Headless);
        /*
         * Below is a small text-based user interface.
         */
        System.out.println("Server console - echos text until 'q' is entered\n");
        // read lines and print them until "q" is encountered
        Scanner s = new Scanner(System.in);
        s.useDelimiter("\\n");
        skip:
        while (s.hasNext())
        {
            String input = s.next();
            if (input.equals("q"))
            {
                break skip; // jump out of the loop marked "skip"
            } else if (input.equals("s"))
            {
                app.prepare();
            };
            System.out.println(input);
        }
        s.close();
        System.out.println("Bye\n");
        System.exit(0); // end all server threads by brute force
    }

    ServerMain()
    {
    }

    public class connLis implements ConnectionListener
    {

        public void connectionAdded(Server server, HostedConnection conn)
        {
            if (STATE == Util.SERVER_IDLE)
            {
                conn.setAttribute("aliveMessages", 0);
                conn.setAttribute("ready", false);
                conn.send(new InitializationMessage(seed));
            } else
            {
                conn.close("Already playing");
            }
        }

        public void connectionRemoved(Server server, HostedConnection conn)
        {
            if ((Boolean) conn.getAttribute("ready") == true)
            {
                readyPlayers--;
                server.broadcast(new ReadyMessage(readyPlayers, server.getConnections().size()));
            } else if (server.getConnections().size() == readyPlayers)
            {
                startGame();
            }
        }
    }

    @Override
    public void simpleInitApp()
    {
        try
        {
            /*
             * Create the actual server object.
             */
            server = Network.createServer(Util.portNumber);
            server.start();
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
        server.addMessageListener(new ServerListener());
        prepareMatch();
    }

    @Override
    public void simpleUpdate(float tpf)
    {
        if (STATE == Util.SERVER_PLAYING)
        {
            for (Spatial ball : cannonballNode.getChildren())
            {
                if (ball.getWorldTranslation().distance(playingfieldNode.getWorldTranslation()) > Util.PLAYINGFIELD_RADIUS + Util.DEAD_MARGIN)
                {
                    ball.removeFromParent();
                    continue;
                }
                CollisionResults results = new CollisionResults();
                canNode.collideWith(ball.getWorldBound(), results);

                if (results.size() > 0)
                {
                    Geometry hit = results.getClosestCollision().getGeometry();
                    hit.removeFromParent();
                    ball.removeFromParent();
                    int value = hit.getUserData("value");
                    //SCORE!!!!
                    if (value == Util.LARGECAN_VALUE)
                    {
                        //activeLargeCan--;
                    } else if (value == Util.MEDIUMCAN_VALUE)
                    {
                        //activeMediumCan--;
                    } else if (value == Util.SMALLCAN_VALUE)
                    {
                        //activeSmallCan--;
                    }
                }

                float x = ball.getUserData("x");
                float z = ball.getUserData("z");
                ball.move(-20 * tpf * x, 0, -20 * tpf * z);

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
    /*
     * Receives messages from clients. 
     */

    private class ServerListener implements MessageListener<HostedConnection>
    {
        /*
         * This method is automatically called whenever a message arrives 
         * from a client.
         */

        public void messageReceived(HostedConnection source, Message m)
        {
            if (m instanceof AliveMessage)
            {
                source.setAttribute("aliveMessages", 0);
            }
            if (m instanceof ReadyMessage)
            {
                if ((Boolean) source.getAttribute("ready") == false)
                {
                    readyPlayers++;
                    source.setAttribute("ready", true);

                    if (readyPlayers == server.getConnections().size())
                    {
                        startGame();
                    } else
                    {
                        server.broadcast(new ReadyMessage(readyPlayers, server.getConnections().size()));
                    }
                }
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
            /*
             * NB! Must be "final"!
             */
            final String m = "Manual (sent from jME thread) "
                    + (manualCounter++);
            /*
             * Print the string m.
             */
            print(m);
            /*
             * Here several things happen. We see a call to the method enqueue 
             * on the object ServerMain.this (that is, a SimpleApplication, 
             * our jME thread). The argument is a new object of type Callable. 
             * What you see is the creation of an anonymous class, or actually
             * instance, that just contains a method call() with two Java 
             * statements. It is this method that will be called by the 
             * jME thread, the receiving thread. What is in this thread will be 
             * carried out by the jME thread when the next call to simpleUpdate
             * is made and before the method simpleUpdate is executed. 
             */
            Future result = ServerMain.this.enqueue(new Callable()
            {
                /*
                 * Define what should be done by the receiving thread.
                 */
                public Object call() throws Exception
                {
                    /*
                     * Send a message to all clients (do a "broadcast"). 
                     */
                    server.broadcast(new NetworkMessage(m));
                    /*
                     * A Callable have to return something so I choose to 
                     * return a boolean, but anything works. Here the result 
                     * is not used. 
                     */
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
                autoCounter = 1, MAX_ALIVE_FAILURES = 10;
        /*
         * Creates a window with a TextArea separate from the jME thread. 
         * 
         * Adds a KeyListerer to the window. 
         */

        AutomaticServerNetWrite(final String name)
        {
            /*
             * Create a window and put "name" in the top bar.
             */
            JFrame frame = new JFrame(name);
            /*
             * The thread with the JFrame should terminate if we click
             * to kill the window.
             */
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            /*
             * The area in which messages will be printed. 
             */
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
            /*
             * Eternal loop...
             */
            while (true)
            {
                String m = "Automatic (sent from ServerNetWrite) "
                        + (autoCounter++);
                print(m);
                /*
                 * Send to all clients. 
                 */
                //synchronized (PlayerNames)
                {
                    for (HostedConnection client : server.getConnections())
                    {
                        client.send(new AliveMessage());
                        client.setAttribute("aliveMessages", ((Integer) client.getAttribute("aliveMessages")) + 1);
                        if ((Integer) client.getAttribute("aliveMessages") > MAX_ALIVE_FAILURES)
                        {
                            print("Kicking unresponsive user.");
                            client.close("Timeout");
                        }
                    }
                }
                //  server.broadcast(new NetworkMessage(m));
                try
                {
                    /*
                     * Take a nap between SLEEP_MIN and SLEEP_MIN+SLEEP_EXTRA
                     * seconds.
                     */
                    Thread.sleep(SLEEP_MIN + sRand.nextInt(SLEEP_EXTRA));
                } catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void prepareMatch()
    {
    }

    public void startGame()
    {
    }
}
