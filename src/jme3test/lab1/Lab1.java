package jme3test.lab1;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.Geometry;
import com.jme3.material.Material;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.audio.AudioNode;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;



/** Sample 7 - how to load an OgreXML model and play an animation,
 * using channels, a controller, and an AnimEventListener. */
public class Lab1 extends SimpleApplication
{
    private int cuboidRotation = 1;
    private int orbitRotation = 1;
    
    private boolean cylinderVisable = true;
    
    private Node orbitNode = new Node();
    private Node centerNode = new Node();
    private Node cubeoidNode;
    private Node sphereNode;
    private Node cylinderNode;
    private Node leanNode;
    
    private AudioNode audio;
    
      
    
    public static void main(String[] args) 
    {
        Lab1 app = new Lab1();
        app.start();
    }
 
    @Override
    public void simpleInitApp() 
    {
      
        Box b = new Box (0.5f,1.5f,1f);
        Geometry cuboid = new Geometry ("cuboid", b);
        Material matB = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");      
        matB.setColor("Color", ColorRGBA.Blue);
        cuboid.setMaterial(matB);
        cubeoidNode = new Node();
        cubeoidNode.attachChild(cuboid);
      
        Sphere s = new Sphere (32,32,0.8f);
        Geometry sphere = new Geometry ("sphere", s);
        Material matS = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");      
        matS.setColor("Color", ColorRGBA.Yellow);
        sphere.setMaterial(matS);
        sphereNode = new Node();
        sphereNode.attachChild(sphere);
      
        Cylinder c = new Cylinder (32,32,0.8f,0.7f,true);
        Geometry cylinder = new Geometry("cylinder", c);
        Material matC = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");      
        matC.setColor("Color", ColorRGBA.Red);
        cylinder.setMaterial(matC);
        cylinderNode = new Node();
        cylinderNode.attachChild(cylinder);
      
        orbitNode.attachChild(cylinderNode);
        orbitNode.attachChild(sphereNode);
        sphereNode.setLocalTranslation(0.8f, 0, 0);
        cylinderNode.setLocalTranslation(-0.8f, 0, 0);
      
        centerNode.attachChild(orbitNode);
        centerNode.attachChild(cubeoidNode);
        orbitNode.setLocalTranslation(3,0,0);
      
        rootNode.attachChild(centerNode);  
        leanNode = cubeoidNode;
        initKeys();
        initCrossHairs();
        
        audio = new AudioNode(assetManager, "Sound/Effects/Gun.wav", false);
        audio.setPositional(true);
        audio.setLooping(false);
        audio.setVolume(1);
        cylinderNode.attachChild(audio);
    }
  
    @Override
    public void simpleUpdate(float tpf) 
    {
        // make the player rotate:
        orbitNode.rotate(0, 0, 2*tpf*orbitRotation);
        centerNode.rotate(0, tpf*cuboidRotation ,0);
    }
    
    private Geometry createSnowball()
    {
        Sphere s = new Sphere (32,32,0.1f);
        Geometry snow = new Geometry ("sphere", s);
        Material matS = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");      
        matS.setColor("Color", ColorRGBA.White);
        snow.setMaterial(matS);
        return snow;
    }
     
       /** Custom Keybinding: Map named actions to inputs. */
    private void initKeys() 
    {
        // You can map one or several inputs to one named action
        inputManager.addMapping("cuboidRotationToggle",  new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("cuboidLeanBack",   new KeyTrigger(KeyInput.KEY_B));
        inputManager.addMapping("cuboidLeanForward",  new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("cuboidRotateNodeToggle", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("orbitNodeRotationToggle",  new KeyTrigger(KeyInput.KEY_O));
        inputManager.addMapping("cylinderViewToggle",   new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("sphereShrinkRay",  new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("sphereEnlargmentRay", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("Shoot", new KeyTrigger(KeyInput.KEY_SPACE));  
        // Add the names to the action listener.
        inputManager.addListener(actionListener,"cuboidRotationToggle", "cuboidRotateNodeToggle", "orbitNodeRotationToggle", "cylinderViewToggle", "Shoot");
        inputManager.addListener(analogListener,"cuboidLeanBack", "cuboidLeanForward", "sphereShrinkRay", "sphereEnlargmentRay");
 
    }
 
    private ActionListener actionListener = new ActionListener() 
    {
        public void onAction(String name, boolean keyPressed, float tpf) 
        {
            if (keyPressed)
            {
                if(name.equals("cuboidRotationToggle"))
                {
                    cuboidRotation = cuboidRotation*-1;
                }
                else if (name.equals("cuboidRotateNodeToggle"))
                {
                    leanNode = (leanNode == centerNode)? cubeoidNode: centerNode;
                }
                else if (name.equals("orbitNodeRotationToggle"))
                {
                    orbitRotation = orbitRotation*-1;
                }
                else if(name.equals("Shoot")&&keyPressed)
                {
                     CollisionResults results = new CollisionResults();
                     Ray ray = new Ray(cam.getLocation(), cam.getDirection());
                     centerNode.collideWith(ray, results);
                     if (results.size() > 0) 
                     {
                        CollisionResult closest = results.getCollision(0);
                        Geometry snow = createSnowball();
                        System.out.println(closest.getGeometry().getName());
                        System.out.println(closest.getContactPoint());
                        Vector3f coord = null;
                        System.out.println("1");
                        coord = closest.getGeometry().getParent().worldToLocal(closest.getContactPoint(), coord);
                        System.out.println("2");
                        System.out.println(coord);
                        snow.setLocalTranslation(coord);
                        System.out.println("3");
                        closest.getGeometry().getParent().attachChild(snow);
                        
                     }
                }
            }
            if (!keyPressed)
            {
                if (name.equals("cylinderViewToggle"))
                {
                    //Set cull to Inhit if current cullHint is always, if not set it to Always.
                   cylinderNode.setCullHint((cylinderNode.getCullHint()== CullHint.Always)? CullHint.Inherit: CullHint.Always);
                   if (cylinderVisable)
                   {
                       audio.playInstance();
                   }
                   cylinderVisable = !cylinderVisable;
                }
            }
        }
        
    };
 
  private AnalogListener analogListener = new AnalogListener() 
  {
      //"cuboidLeanBack", "cuboidLeanForward", "sphereShrinkRay", "sphereEnlargmentRay")
      public void onAnalog(String name, float value, float tpf) 
      {
          if (name.equals("cuboidLeanBack"))
          {
              leanNode.rotate(0, 0, tpf);
          }
          else if (name.equals("cuboidLeanForward"))
          {
              leanNode.rotate(0, 0, -tpf);              
          }
          else if (name.equals("sphereShrinkRay"))
          {
              sphereNode.scale(0.999f);
          }
          else if (name.equals("sphereEnlargmentRay"))
          {
              sphereNode.scale(1.001f);
          }
      }
  };
  
    protected void initCrossHairs() 
    {
    setDisplayStatView(false);
    guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
    BitmapText ch = new BitmapText(guiFont, false);
    ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
    ch.setText("+"); // crosshairs
    ch.setLocalTranslation( // center
      settings.getWidth() / 2 - ch.getLineWidth()/2, settings.getHeight() / 2 + ch.getLineHeight()/2, 0);
    guiNode.attachChild(ch);
  }
} 
            