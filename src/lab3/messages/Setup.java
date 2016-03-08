/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lab3.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

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
    public static class InitializationMessage extends AbstractMessage
    {
        private long seed;
        public InitializationMessage()
        {
        }
        public InitializationMessage(long seed)
        {
            this.seed = seed;
        }
        public long getSeed()
        {
            return this.seed;
        }
    }
}
