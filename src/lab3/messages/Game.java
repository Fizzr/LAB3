/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lab3.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 *
 * @author 4shitnigglesxxdeathx
 */
public class Game
{

    @Serializable
    public static class CansMessage extends AbstractMessage
    {

        Node cans;

        public CansMessage()
        {
        }

        public CansMessage(Node cans)
        {
            this.cans = cans;
        }

        public Node getCans()
        {
            return this.cans;
        }
    }

    @Serializable
    public static class HitMessage extends AbstractMessage
    {

        private int playerID;
        private int hitCan;
        private Vector3f newTranslation;
        private int ballID;

        public HitMessage()
        {
        }

        public HitMessage(int playerID, int hitCan, Vector3f newTranslation, int ballID)
        {
            this.playerID = playerID;
            this.hitCan = hitCan;
            this.newTranslation = newTranslation;
            this.ballID = ballID;
        }

        public int getPlayer()
        {
            return this.playerID;
        }

        public int getHitCan()
        {
            return this.hitCan;
        }

        public Vector3f getNewTranslation()
        {
            return this.newTranslation;
        }

        public int getBallID()
        {
            return this.ballID;
        }
    }

    @Serializable
    public static class ShootMessage extends AbstractMessage
    {

        private Spatial cannonball;
        private int ballID;
        private int playerID;

        public ShootMessage()
        {
        }

        public ShootMessage(Spatial cannonball, int ballID, int playerID)
        {
            this.cannonball = cannonball;
            this.ballID = ballID;
            this.playerID = playerID;
        }

        public Spatial getCannonball()
        {
            return this.cannonball;
        }

        public int getPlayer()
        {
            return this.playerID;
        }
        public int getBallID()
        {
            return this.ballID;
        }
    }
}
