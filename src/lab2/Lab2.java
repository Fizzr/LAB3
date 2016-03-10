package lab2;

import java.util.List;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.Geometry;
import com.jme3.material.Material;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.Spatial;
import com.jme3.audio.AudioNode;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import com.jme3.math.FastMath;
import com.jme3.texture.Texture;
import java.util.Random;


public class Lab2 extends SimpleApplication
{
    
    public static final int PLAYINGFIELD_RESOLUTION = 100;
    public static final int CAN_RESOLUTION = 100;
    public static final int cannonball_NUM = 5;
    public static final int cannonball_RESOLUTION = 100;
    public static final int LARGECAN_NUM = 10;
    public static final int LARGECAN_VALUE = 10;
    public static final int MEDIUMCAN_NUM = 6;
    public static final int MEDIUMCAN_VALUE = 20;
    public static final int SMALLCAN_NUM = 3;
    public static final int SMALLCAN_VALUE = 40;
    public static final int CANS_NUM = LARGECAN_NUM + MEDIUMCAN_NUM + SMALLCAN_NUM;
    public static final int CYLINDER_RESOLUTION = 100;
    
    public static final float DEAD_MARGIN = 1f;
    public static final float START_TIME = 30f;
    public static final float SMALLCAN_RADIUS = 3f;
    public static final float SMALLCAN_HEIGHT = 10f;
    public static final float MEDIUMCAN_RADIUS = 4f;
    public static final float MEDIUMCAN_HEIGHT = 15f;
    public static final float LARGECAN_RADIUS = 5f;
    public static final float LARGECAN_HEIGHT = 20f;
    public static final float MAXIMAL_CAN_RADIUS = LARGECAN_RADIUS;
    public static final float CANNON_SAFETYDISTANCE = 20f;
    public static final float SAFETY_MARGIN = 2f* MAXIMAL_CAN_RADIUS + CANNON_SAFETYDISTANCE;
    public static final float cannonball_RADIUS = 1.1f* MAXIMAL_CAN_RADIUS;
    
    
    public static final float ZERO = 0;
    public static final float ONE = 1;
    public static final float TWO = 2;
    public static final float THREE = 3;
    public static final int vertexes = 32;
    public static final float PLAYINGFIELD_RADIUS = 200f;
    public static final float PLAYINGFIELD_HEIGHT = 0.01f;
    public static final Vector3f PLAYINGFIELD_TRANSLATION = new Vector3f (ZERO, ZERO, -PLAYINGFIELD_RADIUS);
    public static final float CANNON_BARREL_RADIUS = cannonball_RADIUS;
    public static final float CANNON_BARREL_LENGTH = 30f;
    public static final Vector3f CANNON_BARREL_TRANSLATION = new Vector3f(ZERO, ZERO, -CANNON_BARREL_LENGTH / TWO);
    public static final float CANNON_BASE_RADIUS = CANNON_BARREL_RADIUS * THREE;
    public static final float CANNON_BASE_HEIGHT = CANNON_BARREL_RADIUS * THREE;
    public static final Vector3f CANNON_BASE_TRANSLATION = new Vector3f(ZERO, -CANNON_BASE_RADIUS / TWO, ZERO);
    public static final float CANNON_SUPPORT_RADIUS = CANNON_BASE_RADIUS * 1.3f;
    public static final float CANNON_SUPPORT_HEIGHT = CANNON_BASE_RADIUS * 1.3f;
    public static final Vector3f CANNON_SUPPORT_TRANSLATION = new Vector3f(ZERO, -CANNON_SUPPORT_HEIGHT/TWO + PLAYINGFIELD_HEIGHT - CANNON_BARREL_RADIUS, ZERO);
    public static final Vector3f CANNON_NODE_TRANSLATION = new Vector3f (ZERO, CANNON_BARREL_RADIUS, ZERO);
    public static final Vector3f ROTATE_X = new Vector3f(ONE, ZERO, ZERO);
    public static final Vector3f ROTATE_Y = new Vector3f(ZERO, ONE, ZERO);
    public static final Vector3f ROTATE_Z = new Vector3f(ZERO, ZERO, ONE);
    //public static final float HALF_PI = FastMath.PI/TWO;
    
    
    public static final float CANNON_ROTATION_SPEED = 20f;
    public static final float LASER_LENGTH = PLAYINGFIELD_RADIUS/TWO;
    public static final float LASER_WIDTH = 0.1f;
    public static final Vector3f LASER_TRANSLATION = new Vector3f(ZERO, ZERO, -LASER_LENGTH);

    private Node cannonNode = new Node();
    private Node laserNode = new Node();
    private Node cannonballNode = new Node();
    private Node cannonballStartNode = new Node();
    private Node playingfieldNode = new Node();
    private Node canNode = new Node();
    private int activeSmallCan = 0;
    private int activeMediumCan= 0;
    private int activeLargeCan = 0;
    Random rand = new Random();
    BitmapText scoreText;
    BitmapText timeText;
    int score = 0;
    float time = 30.0f;
    
        public static void main(String[] args) 
    {
        Lab2 app = new Lab2();
        app.start();
    }
    
    public void simpleInitApp()
    {
        Cylinder c = new Cylinder(vertexes, vertexes, CANNON_BARREL_RADIUS, CANNON_BARREL_LENGTH, true);
        Geometry cannon = new Geometry ("canon", c);
        Material matA = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");      
        matA.setColor("Color", ColorRGBA.DarkGray);
        cannon.setMaterial(matA);
        
        cannon.setLocalTranslation(CANNON_BARREL_TRANSLATION);
        
        Cylinder b = new Cylinder (vertexes, vertexes, CANNON_BASE_RADIUS, CANNON_BASE_HEIGHT, true);
        Geometry base = new Geometry ("base", b);
        Material matB = new Material (assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matB.setColor("Color", ColorRGBA.Brown);
        base.setMaterial(matB);
        
        Quaternion spin90 = new Quaternion();
        spin90.fromAngleAxis( FastMath.HALF_PI, ROTATE_Y);
        base.setLocalRotation(spin90);
        base.setLocalTranslation(CANNON_BASE_TRANSLATION);
        
        Cylinder p = new Cylinder (vertexes, CYLINDER_RESOLUTION, CANNON_SUPPORT_RADIUS, CANNON_SUPPORT_HEIGHT, true);
        Geometry plate = new Geometry ("plate", p);
        Material matC = new Material (assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matC.setColor("Color", ColorRGBA.Black);
        plate.setMaterial(matC);
        
        Quaternion pitch90 = new Quaternion();
        pitch90.fromAngleAxis(FastMath.HALF_PI, ROTATE_X);
        plate.setLocalRotation(pitch90);
        plate.setLocalTranslation(CANNON_SUPPORT_TRANSLATION);
        
        Cylinder t = new Cylinder (2, PLAYINGFIELD_RESOLUTION, PLAYINGFIELD_RADIUS, PLAYINGFIELD_HEIGHT, true);
        Geometry terrain = new Geometry("terrain", t);
        Material matD = new Material (assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture terrainTex = assetManager.loadTexture("Textures/grass.jpg");
        matD.setTexture("ColorMap", terrainTex);
        terrain.setMaterial(matD);
        
        playingfieldNode.attachChild(terrain);
        playingfieldNode.setLocalTranslation(PLAYINGFIELD_TRANSLATION);
        playingfieldNode.setLocalRotation(pitch90);
        
        Box l = new Box(LASER_WIDTH, LASER_WIDTH, LASER_LENGTH);
        Geometry laser = new Geometry("laser", l);
        Material matL = new Material (assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matL.setColor("Color", ColorRGBA.Red);
        laser.setMaterial(matL);
        
        laserNode.attachChild(laser);
        laserNode.setLocalTranslation(LASER_TRANSLATION);
        laserNode.setCullHint(CullHint.Always);
        
        cannonballStartNode.setLocalTranslation(ZERO, ZERO, -CANNON_BARREL_LENGTH);
        
        cannonNode.attachChild(cannonballStartNode);
        cannonNode.attachChild(cannon);
        cannonNode.attachChild(base);
        cannonNode.attachChild(plate);
        cannonNode.attachChild(laserNode);

        cannonNode.setLocalTranslation(CANNON_NODE_TRANSLATION);
       
        rootNode.attachChild(cannonNode);
        rootNode.attachChild(cannonballNode);
        rootNode.attachChild(playingfieldNode);
        rootNode.attachChild(canNode);
        
        
        cam.setLocation(new Vector3f(0f, 150f, 60f));
        
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        scoreText = new BitmapText(guiFont, false);
        scoreText.setSize(guiFont.getCharSet().getRenderedSize());
        scoreText.setText("Score: "+score);
        scoreText.setLocalTranslation(scoreText.getLineHeight(),  this.settings.getHeight() - scoreText.getLineHeight(), 0);
        
        timeText = new BitmapText(guiFont, false);
        timeText.setSize(guiFont.getCharSet().getRenderedSize());
        timeText.setText("Time: "+time);
        timeText.setLocalTranslation(timeText.getLineHeight(),  this.settings.getHeight() - timeText.getLineHeight()*2, 0);

        guiNode.attachChild(scoreText);
        guiNode.attachChild(timeText);

        
        inputInit();
    }
    
    private void inputInit()
    {
        inputManager.addMapping("turnLeft",  new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("turnRight",  new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("toggleLaser",  new KeyTrigger(KeyInput.KEY_L));
        inputManager.addMapping("fire",  new KeyTrigger(KeyInput.KEY_SPACE));
        
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
                    laserNode.setCullHint((laserNode.getCullHint()== CullHint.Dynamic)? CullHint.Always : CullHint.Dynamic);
                    
                }
                else if (name == "fire")
                {
                    if (cannonballNode.getChildren().size() < cannonball_NUM)
                    {
                        cannonballNode.attachChild(createcannonball(cannonNode.getLocalRotation()));
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
          if(time > 0)
          {
            if (name == "turnLeft")
            {
                cannonNode.rotate(ZERO, tpf, ZERO);

            }
            else if (name == "turnRight")
            {
                cannonNode.rotate(ZERO, -tpf, ZERO);

            }
          }
      }
  };
    
    public void simpleUpdate(float tpf)
    {
        if (time > 0)
        {
            time -= tpf;
        }
        else 
        {
            time = 0;
        }
        timeText.setText("Time: "+time);
        //Handle cans
        if (canNode.getChildren().size() < CANS_NUM)
        {
            Geometry can;
            if (activeSmallCan < SMALLCAN_NUM)
            {
                can = createCan(SMALLCAN_RADIUS, SMALLCAN_HEIGHT, SMALLCAN_VALUE);
                activeSmallCan++;
            }
            else if (activeMediumCan < MEDIUMCAN_NUM)
            {
                can = createCan(MEDIUMCAN_RADIUS, MEDIUMCAN_HEIGHT, MEDIUMCAN_VALUE);  
                activeMediumCan++;
            }
            else if (activeLargeCan < LARGECAN_NUM)
            {
                can = createCan(LARGECAN_RADIUS, LARGECAN_HEIGHT, LARGECAN_VALUE); 
                activeLargeCan++;
            }
            else
            {
                throw new RuntimeException("Can deployment error!");
            }
            canNode.attachChild(can);
        }
        
        //Handle cannonballs
        List<Spatial> balls = cannonballNode.getChildren();
        Spatial ball;
        int i=0;
        while(i < balls.size())
        {
            ball = balls.get(i);
            if (ball.getWorldTranslation().distance(playingfieldNode.getWorldTranslation()) > PLAYINGFIELD_RADIUS+DEAD_MARGIN)
            {
                ball.removeFromParent();
                i++;
                continue;
            }
            CollisionResults results = new CollisionResults();
            canNode.collideWith(ball.getWorldBound(), results);
            
            if (results.size() > 0)
            {
                
                Geometry hit = results.getClosestCollision().getGeometry();
                int value = hit.getUserData("value");
                score += value;
                if (value== LARGECAN_VALUE)
                {
                    activeLargeCan--;
                }
                else if(value == MEDIUMCAN_VALUE)
                {
                    activeMediumCan--;
                }
                else if (value == SMALLCAN_VALUE)
                {
                    activeSmallCan--;
                }
                hit.removeFromParent();
                ball.removeFromParent();
            }
            
            float x = ball.getUserData("x");
            float z = ball.getUserData("z");
            ball.move(-20*tpf*x, ZERO, -20*tpf*z);
            i++;
        }
        scoreText.setText("Score: "+score);
        

    }
    
    public Geometry createcannonball(Quaternion rotation)
    {
        Sphere c = new Sphere(cannonball_RESOLUTION,cannonball_RESOLUTION,cannonball_RADIUS);
        Geometry cBall = new Geometry("cannonball", c);
        Material matC = new Material (assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matC.setColor("Color", ColorRGBA.Gray);
        cBall.setMaterial(matC);
        cBall.setUserData("x", FastMath.sin(rotation.toAngles(new float[3])[1]));
        cBall.setUserData("z", FastMath.cos(rotation.toAngles(new float[3])[1]));
        cBall.setLocalTranslation(cannonballStartNode.getWorldTranslation());
        return cBall;
        
    }
    
    public Geometry createCan (float radius, float height, int value)
    {
        Cylinder c = new Cylinder (vertexes, CYLINDER_RESOLUTION, radius, height, true);
        Geometry can = new Geometry ("Can", c);
        Material matC = new Material (assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        switch(value)
        {
            case LARGECAN_VALUE:
                matC.setColor("Color", ColorRGBA.Yellow);
                break; 
            case MEDIUMCAN_VALUE:
                matC.setColor("Color", ColorRGBA.Orange);
                break;
            case SMALLCAN_VALUE:
                matC.setColor("Color", ColorRGBA.Red);
                break;
        }
        can.setMaterial(matC);
        can.setUserData("value", value);
        can.rotate(FastMath.HALF_PI, 0, 0);
                
        while(can.getWorldTranslation().distance(playingfieldNode.getLocalTranslation()) > PLAYINGFIELD_RADIUS-SAFETY_MARGIN)
        {
            float x= (rand.nextFloat()*(PLAYINGFIELD_RADIUS-SAFETY_MARGIN)*2)-(PLAYINGFIELD_RADIUS-SAFETY_MARGIN);
            float z= rand.nextFloat()*(PLAYINGFIELD_RADIUS-SAFETY_MARGIN)*2-(PLAYINGFIELD_RADIUS-SAFETY_MARGIN)-PLAYINGFIELD_RADIUS;
            can.setLocalTranslation(x, height/2, z);  //CAN CAN STILL BE LOCATED WHERE IT SHOULDN'T BE (x,z is square, playingfield is circle)
        }
        return can;
    }
    
}
