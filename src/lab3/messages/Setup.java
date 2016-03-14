/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lab3.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import java.util.List;

/**
 *
 * @author 4shitnigglesxxdeathx
 */
public class Setup
{

    @Serializable
    public static class ReadyMessage extends AbstractMessage
    {

        private int ready;
        private int total;

        public ReadyMessage()
        {
        }

        public ReadyMessage(int ready, int total)
        {
            this.ready = ready;
            this.total = total;
        }

        public int getReadyPlayers()
        {
            return this.ready;
        }

        public int getTotalPlayers()
        {
            return this.total;
        }
    }

    @Serializable
    public static class StartMessage extends AbstractMessage
    {
        List<String> playerNames;
        public StartMessage()
        {
        }
        public StartMessage(List<String> playerNames)
        {
            this.playerNames = playerNames;
        }
        public List<String> getPlayerNames()
        {
            return this.playerNames;
        }
    }

    @Serializable
    public static class ConnectionMessage extends AbstractMessage
    {

        private boolean connect;

        public ConnectionMessage()
        {
        }

        public ConnectionMessage(boolean connect)
        {
            this.connect = connect;
        }

        public boolean getConnect()
        {
            return this.connect;
        }
    }

    @Serializable
    public static class PlayerMessage extends AbstractMessage
    {
        private Vector3f translation;
        public PlayerMessage()
        {
        }
        public PlayerMessage(Vector3f translation)
        {
            this.translation = translation;
        }
        public Vector3f getTranslation()
        {
            return this.translation;
        }
        
    }
    @Serializable
    public static class DisconnectMessagae extends AbstractMessage
    {
        private int playerID;
        public DisconnectMessagae()
        {
        }
        public DisconnectMessagae(int playerID)
        {
            this.playerID = playerID;
        }
        public int getPlayerID()
        {
            return this.playerID;
        }
    }
}
