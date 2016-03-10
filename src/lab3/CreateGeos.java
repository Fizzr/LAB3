/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lab3;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import java.util.Random;

/**
 *
 * @author Claes
 */
public class CreateGeos
{
    private AssetManager assetManager;
    private Random rand = new Random();
    public CreateGeos(AssetManager assetManager)
    {
        this.assetManager = assetManager;
    }
    
    public Spatial createPlayingfield()
    {
        Cylinder t = new Cylinder (2, Util.PLAYINGFIELD_RESOLUTION, Util.PLAYINGFIELD_RADIUS, Util.PLAYINGFIELD_HEIGHT, true);
        Geometry playingfield = new Geometry("playingfield", t);
        Material matD = new Material (assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture terrainTex = assetManager.loadTexture("Textures/grass.jpg");
        matD.setTexture("ColorMap", terrainTex);
        playingfield.setMaterial(matD);
        playingfield.rotate(FastMath.HALF_PI, 0, 0);
        return playingfield;
    }
    
    public Node createCannon()
    {
        Cylinder c = new Cylinder(Util.vertexes, Util.vertexes, Util.CANNON_BARREL_RADIUS, Util.CANNON_BARREL_LENGTH, true);
        Geometry cannon = new Geometry ("canon", c);
        Material matA = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");      
        matA.setColor("Color", ColorRGBA.DarkGray);
        cannon.setMaterial(matA);
        
        cannon.setLocalTranslation(Util.CANNON_BARREL_TRANSLATION);
        
        Cylinder b = new Cylinder (Util.vertexes, Util.vertexes, Util.CANNON_BASE_RADIUS, Util.CANNON_BASE_HEIGHT, true);
        Geometry base = new Geometry ("base", b);
        Material matB = new Material (assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matB.setColor("Color", ColorRGBA.Brown);
        base.setMaterial(matB);
        
        Quaternion spin90 = new Quaternion();
        spin90.fromAngleAxis( FastMath.HALF_PI, Util.ROTATE_Y);
        base.setLocalRotation(spin90);
        base.setLocalTranslation(Util.CANNON_BASE_TRANSLATION);
        
        Cylinder p = new Cylinder (Util.vertexes, Util.CYLINDER_RESOLUTION, Util.CANNON_SUPPORT_RADIUS, Util.CANNON_SUPPORT_HEIGHT, true);
        Geometry plate = new Geometry ("plate", p);
        Material matC = new Material (assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matC.setColor("Color", ColorRGBA.Black);
        plate.setMaterial(matC);
        
        Quaternion pitch90 = new Quaternion();
        pitch90.fromAngleAxis(FastMath.HALF_PI, Util.ROTATE_X);
        plate.setLocalRotation(pitch90);
        plate.setLocalTranslation(Util.CANNON_SUPPORT_TRANSLATION);
        
        Box l = new Box(Util.LASER_WIDTH, Util.LASER_WIDTH, Util.LASER_LENGTH);
        Geometry laser = new Geometry("laser", l);
        Material matL = new Material (assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matL.setColor("Color", ColorRGBA.Red);
        laser.setMaterial(matL);
        
        laser.setLocalTranslation(Util.LASER_TRANSLATION);
        laser.setCullHint(Spatial.CullHint.Always);
        
        Node cannonNode = new Node("Cannon");
        cannonNode.attachChild(cannon);
        cannonNode.attachChild(base);
        cannonNode.attachChild(plate);
        cannonNode.attachChild(laser);
        
        return cannonNode;
    }
    
    public Spatial createCan(int value)
    {
        ColorRGBA colour = null;
        float radius = 0;
        float height = 0;
        switch(value)
        {
            case Util.LARGECAN_VALUE:
                colour = ColorRGBA.Yellow;
                radius = Util.LARGECAN_RADIUS;
                height = Util.LARGECAN_HEIGHT;
                break; 
            case Util.MEDIUMCAN_VALUE:
                colour = ColorRGBA.Orange;
                radius = Util.MEDIUMCAN_RADIUS;
                height = Util.MEDIUMCAN_HEIGHT;
                break;
            case Util.SMALLCAN_VALUE:
                colour = ColorRGBA.Red;
                radius = Util.SMALLCAN_RADIUS;
                height = Util.SMALLCAN_HEIGHT;
                break;
        }
        
        Cylinder c = new Cylinder (Util.vertexes, Util.CYLINDER_RESOLUTION, radius, height, true);
        Geometry can = new Geometry ("Can", c);
        Material matC = new Material (assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matC.setColor("Color", colour);
        can.setMaterial(matC);
        can.setUserData("value", value);
        can.rotate(FastMath.HALF_PI, 0, 0);
        can.rotate(0, 0, rand.nextFloat()*FastMath.TWO_PI);
        can.move(can.getLocalRotation().getRotationColumn(2).mult(Util.PLAYINGFIELD_RADIUS-Util.SAFETY_MARGIN));
        return can;
    }
    
    public Geometry createcannonball(Quaternion rotation)
    {
        Sphere c = new Sphere(Util.CANNONBALL_RESOLUTION,Util.CANNONBALL_RESOLUTION,Util.CANNONBALL_RADIUS);
        Geometry cBall = new Geometry("cannonball", c);
        Material matC = new Material (assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matC.setColor("Color", ColorRGBA.Gray);
        cBall.setMaterial(matC);
        cBall.setLocalRotation(rotation);
        return cBall;
        
    }
}
