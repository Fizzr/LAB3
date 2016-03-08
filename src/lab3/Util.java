/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lab3;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializer;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Cylinder;
import java.util.Random;
import lab3.messages.Network.*;
import lab3.messages.Game.*;
import lab3.messages.Setup.*;

/**
 *
 * @author 4shitnigglesxxdeathx
 */
public class Util
{
    //MISC
    public static final float ZERO = 0;
    public static final float ONE = 1;
    public static final float TWO = 2;
    public static final float THREE = 3;
    public static final int vertexes = 32;
    public static final Vector3f ROTATE_X = new Vector3f(ONE, ZERO, ZERO);
    public static final Vector3f ROTATE_Y = new Vector3f(ZERO, ONE, ZERO);
    public static final Vector3f ROTATE_Z = new Vector3f(ZERO, ZERO, ONE);
    public static final float START_TIME = 30f;
    //PLAYINGFIELD
    public static final int PLAYINGFIELD_RESOLUTION = 100;
    public static final float PLAYINGFIELD_RADIUS = 200f;
    public static final float PLAYINGFIELD_HEIGHT = 0.01f;
    public static final Vector3f PLAYINGFIELD_TRANSLATION = new Vector3f (ZERO, ZERO, -PLAYINGFIELD_RADIUS);
    //CANS
    public static final int CAN_RESOLUTION = 100;
    public static final int LARGECAN_NUM = 10;
    public static final int LARGECAN_VALUE = 10;
    public static final int MEDIUMCAN_NUM = 6;
    public static final int MEDIUMCAN_VALUE = 20;
    public static final int SMALLCAN_NUM = 3;
    public static final int SMALLCAN_VALUE = 40;
    public static final int CANS_NUM = LARGECAN_NUM + MEDIUMCAN_NUM + SMALLCAN_NUM;
    public static final float SMALLCAN_RADIUS = 3f;
    public static final float SMALLCAN_HEIGHT = 10f;
    public static final float MEDIUMCAN_RADIUS = 4f;
    public static final float MEDIUMCAN_HEIGHT = 15f;
    public static final float LARGECAN_RADIUS = 5f;
    public static final float LARGECAN_HEIGHT = 20f;
    public static final float MAXIMAL_CAN_RADIUS = LARGECAN_RADIUS;
    public static final int CYLINDER_RESOLUTION = 100;
    //CANNONBAL
    public static final int CANNONBALL_NUM = 5;
    public static final int CANNONBALL_RESOLUTION = 100;
    public static final float CANNONBALL_RADIUS = 1.1f* MAXIMAL_CAN_RADIUS;
    //CANNON
    public static final float CANNON_BARREL_RADIUS = CANNONBALL_RADIUS;
    public static final float CANNON_BARREL_LENGTH = 30f;
    public static final Vector3f CANNON_BARREL_TRANSLATION = new Vector3f(ZERO, ZERO, -CANNON_BARREL_LENGTH / TWO);
    public static final float CANNON_BASE_RADIUS = CANNON_BARREL_RADIUS * THREE;
    public static final float CANNON_BASE_HEIGHT = CANNON_BARREL_RADIUS * THREE;
    public static final Vector3f CANNON_BASE_TRANSLATION = new Vector3f(ZERO, -CANNON_BASE_RADIUS / TWO, ZERO);
    public static final float CANNON_SUPPORT_RADIUS = CANNON_BASE_RADIUS * 1.3f;
    public static final float CANNON_SUPPORT_HEIGHT = CANNON_BASE_RADIUS * 1.3f;
    public static final Vector3f CANNON_SUPPORT_TRANSLATION = new Vector3f(ZERO, -CANNON_SUPPORT_HEIGHT/TWO + PLAYINGFIELD_HEIGHT - CANNON_BARREL_RADIUS, ZERO);
    public static final Vector3f CANNON_NODE_TRANSLATION = new Vector3f (ZERO, CANNON_BARREL_RADIUS, ZERO);
    public static final float CANNON_ROTATION_SPEED = 20f;
    //MARGINS
    public static final float DEAD_MARGIN = 1f;
    public static final float CANNON_SAFETYDISTANCE = 20f;
    public static final float SAFETY_MARGIN = 2f* MAXIMAL_CAN_RADIUS + CANNON_SAFETYDISTANCE;
    //LASER
    public static final float LASER_LENGTH = PLAYINGFIELD_RADIUS/TWO;
    public static final float LASER_WIDTH = 0.1f;
    public static final Vector3f LASER_TRANSLATION = new Vector3f(ZERO, ZERO, -LASER_LENGTH);
    //SERVER
    public String hostname = "127.0.0.1";
    public static int portNumber = 7000;
    public static int SERVER_IDLE = 0;
    public static int SERVER_PLAYING = 1;
    //CLIENT
    public static int CLIENT_IDLE = 0;
    public static int CLIENT_LOADING = 1;
    public static int CLIENT_WAITING = 2;
    public static int CLIENT_PLAYIING = 3;
    
    public void initMessages()
    {
        Serializer.registerClass(AliveMessage.class);
        Serializer.registerClass(ReadyMessage.class);
        //Serializer.registerClass(AliveMessage.class);

    }
}
