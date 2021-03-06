/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lab3.messages;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
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

        private List<Integer> values;
        private List<Vector3f> translations;
        public CansMessage()
        {
        }

        public CansMessage(List<Vector3f> translations, List<Integer> values)
        {
            this.translations = translations;
            this.values = values;
        }

        public List<Vector3f> getTranslations()
        {
            return this.translations;
        }
        public Vector3f getTranslation(int i)
        {
            return this.translations.get(i);
        }
        public List<Integer> getValues()
        {
            return this.values;
        }
        public int getValue(int i)
        {
            return this.values.get(i);
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

        Quaternion rotation;
        Vector3f translation;
        private int ballID;
        private int playerID;

        public ShootMessage()
        {
        }

        public ShootMessage(Quaternion rotation, Vector3f translation, int ballID, int playerID)
        {
            this.rotation = rotation;
            this.translation = translation;
            this.ballID = ballID;
            this.playerID = playerID;
        }

        public Quaternion getRotation()
        {
            return this.rotation;
        }
        public Vector3f getTranslation()
        {
            return this.translation;
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
    @Serializable
    public static class CollisionMessage extends AbstractMessage
    {
        private int player1;
        private int player2;
        private int ball1;
        private int ball2;
        private Quaternion dir1;
        private Quaternion dir2;
        public CollisionMessage(){}
        public CollisionMessage(int player1, int ball1, int player2, int ball2, Quaternion dir1, Quaternion dir2)
        {
            this.player1 = player1;
            this.player2 = player2;
            this.ball1 = ball1;
            this.ball2 = ball2;
            this.dir1 = dir1;
            this.dir2 = dir2;
        }
        public int[] getPlayers()
        {
            return new int[]{player1, player2};
        }
        public int[] getBallIDs()
        {
            return new int[]{ball1, ball2};
        }
        public Quaternion[] getDirections()
        {
            return new Quaternion[]{dir1, dir2};
        }
    }
    @Serializable
    public static class WinnerMessage extends AbstractMessage
    {
        private int winner;
        public WinnerMessage (){}
        public WinnerMessage (int winnerPlayer)
        {
            this.winner = winnerPlayer;
        }
        public int getWinner()
        {
            return this.winner;
        }
    }
    @Serializable
    public static class RotateMessage extends AbstractMessage
    {
        private int dir;
        private Quaternion startRot;
        private int player;
        public RotateMessage(){}
        public RotateMessage(int direction, Quaternion startRotate, int player)
        {
            dir = direction;
            startRot = startRotate;
            this.player = player;
        }
        public int getDirection()
        {
            return dir;
        }
        public Quaternion getStartRotation()
        {
            return startRot;
        }
        public int getPlayer()
        {
            return player;
        }
    }
}
