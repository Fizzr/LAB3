/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lab3.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author fredrik
 */
public class Network
{

    @Serializable
    public static class NetworkMessage extends AbstractMessage
    {
        private String message = "";

        public NetworkMessage()
        {
        }
        public NetworkMessage(String message)
        {
            this.message = message;
        }
        public String getMessage()
        {
            return message;
        }
    }
    
    @Serializable
    public static class AliveMessage extends AbstractMessage
    {
        public AliveMessage()
        {
        }
    }
}
