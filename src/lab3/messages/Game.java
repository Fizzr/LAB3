/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lab3.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Spatial;
import java.util.List;

/**
 *
 * @author 4shitnigglesxxdeathx
 */
public class Game
{
    @Serializable
    public static class CansMessage extends AbstractMessage
    {
        List<Spatial> cans;
        public CansMessage()
        {
        }
        public CansMessage(List<Spatial> cans)
        {
            this.cans = cans;
        }
        public List<Spatial> getCans()
        {
            return this.cans;
        }
    }
    
    @Serializable
    public static class HitMessage extends AbstractMessage
    {
        private int playerID;
        private Spatial hitCan;
        private Spatial newCan;
        private Spatial cannonball;
        public HitMessage()
        {
        }
        public HitMessage(int playerID, Spatial hitCan, Spatial newCan, Spatial cannonball)
        {
            this.playerID = playerID;
            this.hitCan = hitCan;
            this.newCan = newCan;
            this.cannonball = cannonball;
        }
        public int getPlayer()
        {
            return this.playerID;
        }
        public Spatial getHitCan()
        {
            return this.hitCan;
        }
        public Spatial getNewCan()
        {
            return this.newCan;
        }
        public Spatial getCannonball()
        {
            return this.cannonball;
        }
    }
    @Serializable
    public static class ShootMessage extends AbstractMessage
    {
        private Spatial cannonball;
        public ShootMessage ()
        {
        }
        public ShootMessage (Spatial cannonball)
        {
            this.cannonball = cannonball;
        }
        public Spatial getCannonball()
        {
            return this.cannonball;
        }
    }
}
